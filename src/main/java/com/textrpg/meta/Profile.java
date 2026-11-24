package com.textrpg.meta;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.textrpg.entity.Job;
import com.textrpg.entity.Player;
import com.textrpg.entity.Stats;

/**
 * 슬롯에 영구히 남는 기록: 파편, 강화 단계, 해금 직업, 누적 기록. 캐릭터가 죽어도 지워지지 않는다.
 */
public class Profile {

    private int shards;
    private int totalRuns;
    private int bestFloor;
    private int clears;
    private int totalKills;
    private final Map<UpgradeType, Integer> upgradeLevels = new EnumMap<>(UpgradeType.class);
    private final Set<Job> unlockedJobs = EnumSet.of(Job.WARRIOR);

    private Profile() {
    }

    /**
     * 처음 시작하는 슬롯의 빈 프로필. 전사만 해금, 파편 0, 강화 0단계.
     */
    public static Profile newProfile() {
        return new Profile();
    }

    /**
     * 세이브 파일([PROFILE], [UPGRADES])에서 읽은 값으로 프로필을 복원한다. SaveManager(SlotCodec) 가 호출한다.
     * 전사는 항상 해금 상태로 둔다. 강화 맵에 없는 강화는 0단계.
     *
     * @throws IllegalArgumentException 음수 값이나 최대 단계를 넘는 강화가 있을 때 (SlotCodec 이 SaveLoadException 으로 감싼다)
     */
    public static Profile restore(int shards, int totalRuns, int bestFloor, int clears, int totalKills,
                                  Set<Job> unlockedJobs, Map<UpgradeType, Integer> upgradeLevels) {
        Profile p = new Profile();
        p.shards = requireNotNegative("shards", shards);
        p.totalRuns = requireNotNegative("totalRuns", totalRuns);
        p.bestFloor = requireNotNegative("bestFloor", bestFloor);
        p.clears = requireNotNegative("clears", clears);
        p.totalKills = requireNotNegative("totalKills", totalKills);
        p.unlockedJobs.addAll(unlockedJobs);
        upgradeLevels.forEach((type, level) -> {
            if (level < 0 || level > type.getMaxLevel()) {
                throw new IllegalArgumentException(
                        type + " 단계는 0 ~ " + type.getMaxLevel() + " 입니다: " + level);
            }
            if (level > 0) {
                p.upgradeLevels.put(type, level);
            }
        });
        return p;
    }

    private static int requireNotNegative(String name, int value) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " 은(는) 0 이상입니다: " + value);
        }
        return value;
    }

    public int getShards() {
        return shards;
    }

    public int getTotalRuns() {
        return totalRuns;
    }

    public int getBestFloor() {
        return bestFloor;
    }

    public int getClears() {
        return clears;
    }

    public int getTotalKills() {
        return totalKills;
    }

    /**
     * 강화 단계. 산 적 없으면 0.
     */
    public int getLevel(UpgradeType t) {
        return upgradeLevels.getOrDefault(t, 0);
    }

    public boolean isUnlocked(Job job) {
        return unlockedJobs.contains(job);
    }

    /**
     * 다음 단계를 살 수 있는지 (최대 단계 전이고 파편이 충분한지).
     */
    public boolean canBuy(UpgradeType t) {
        return getLevel(t) < t.getMaxLevel() && shards >= t.costFor(getLevel(t) + 1);
    }

    /**
     * 다음 단계를 산다. 샀으면 true. 직업 해금 강화면 unlockedJobs 에도 넣는다.
     */
    public boolean buy(UpgradeType t) {
        if (!canBuy(t)) {
            return false;
        }
        int nextLevel = getLevel(t) + 1;
        shards -= t.costFor(nextLevel);
        upgradeLevels.put(t, nextLevel);
        if (t == UpgradeType.UNLOCK_MAGE) {
            unlockedJobs.add(Job.MAGE);
        }
        if (t == UpgradeType.UNLOCK_ROGUE) {
            unlockedJobs.add(Job.ROGUE);
        }
        return true;
    }

    /**
     * 새 탐험 시작 직후 호출. 강화 단계만큼 Player 를 강하게 만든다.
     */
    public void applyTo(Player p) {
        Stats stats = p.getStats();

        int hpBonus = getLevel(UpgradeType.MAX_HP) * UpgradeType.MAX_HP.getValuePerLevel();
        stats.addMaxHp(hpBonus);

        int attackBonus = getLevel(UpgradeType.ATTACK) * UpgradeType.ATTACK.getValuePerLevel();
        stats.setAttack(stats.getAttack() + attackBonus);

        int defenseBonus = getLevel(UpgradeType.DEFENSE) * UpgradeType.DEFENSE.getValuePerLevel();
        stats.setDefense(stats.getDefense() + defenseBonus);

        int potionCount = getLevel(UpgradeType.START_POTION) * UpgradeType.START_POTION.getValuePerLevel();
        p.giveStartPotions(potionCount);

        applyRunModifiers(p);
    }

    /**
     * 저장되지 않는 탐험용 강화 효과를 Player 에 맞춘다: 큰 가방(BAG_SIZE) 칸 수, 배움의 축복(EXP_BOOST) 경험치 배율.
     * applyTo() 가 새 탐험 때 부르고, 이어하기로 Player.restore() 한 뒤에도 불러야 한다.
     * 더하는 게 아니라 맞추는 것이라 여러 번 불러도 결과가 같다.
     */
    public void applyRunModifiers(Player p) {
        int extraSlots = getLevel(UpgradeType.BAG_SIZE) * UpgradeType.BAG_SIZE.getValuePerLevel();
        p.getInventory().setExtraCapacity(extraSlots);

        int expPercent = getLevel(UpgradeType.EXP_BOOST) * UpgradeType.EXP_BOOST.getValuePerLevel();
        p.setExpMultiplier(1.0 + expPercent / 100.0);
    }

    /**
     * 탐험이 끝났을 때(사망·클리어) 기록과 파편을 더한다.
     */
    public void recordRun(RunResult r, int shardsEarned) {
        shards += shardsEarned;
        totalRuns++;
        totalKills += r.getKills();
        if (r.isVictory()) {
            clears++;
        }
        bestFloor = Math.max(bestFloor, r.getDeepestFloor());
    }
}
