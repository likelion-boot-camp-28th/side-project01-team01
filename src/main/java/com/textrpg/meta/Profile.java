package com.textrpg.meta;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.textrpg.entity.Job;
import com.textrpg.entity.Player;

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
        // TODO(B): MAX_HP·ATTACK·DEFENSE → 스탯 증가, START_POTION → 치유 물약 지급,
        //  BAG_SIZE → 가방 칸 증가(P1), EXP_BOOST → p.setExpMultiplier(P1)
        throw new UnsupportedOperationException("TODO(B): Profile.applyTo");
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

    // TODO(B, A와 합의): 세이브 파일([PROFILE], [UPGRADES])에서 값을 채우는 방법
}
