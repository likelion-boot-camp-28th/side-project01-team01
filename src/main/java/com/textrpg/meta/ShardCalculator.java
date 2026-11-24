package com.textrpg.meta;

import java.util.List;
import java.util.ArrayList;

/**
 * 탐험이 끝났을 때 받을 파편을 계산한다 (기획서 2.6 파편 획득 공식).
 * 예: 3층 사망, 처치 14, 레벨 4, 골드 57 → 30 + 14 + 12 + 5 = 61
 */
public final class ShardCalculator {

    public static final int SHARDS_PER_FLOOR = 10;
    public static final int SHARDS_PER_KILL = 1;
    public static final int SHARDS_PER_LEVEL = 3;
    public static final int GOLD_PER_SHARD = 10;
    public static final int CLEAR_BONUS = 100;

    private ShardCalculator() {
    }

    /**
     * 받을 파편 합계. 파편 수집가(SHARD_BOOST, P1) 단계만큼 % 를 더한다.
     */
    public static int calculate(RunResult r, Profile p) {
        int total = (r.getDeepestFloor() * SHARDS_PER_FLOOR);
        total += (r.getKills() * SHARDS_PER_KILL);
        total += (r.getFinalLevel() * SHARDS_PER_LEVEL);
        total += (r.getGold() / GOLD_PER_SHARD);
        if (r.isVictory() == true) {
            total += CLEAR_BONUS;
        }
        int boostPercent = p.getLevel(UpgradeType.SHARD_BOOST) * UpgradeType.SHARD_BOOST.getValuePerLevel();
        return total + total * boostPercent / 100;
    }

    /**
     * 결과 화면에 보여 줄 항목별 내역. 예: "도달 층 3 × 10 = 30"
     */
    public static List<String> breakdown(RunResult r, Profile p) {

        List<String> lines = new ArrayList<>();

        int floor = r.getDeepestFloor();
        lines.add("도달 층 " + floor + " × " + SHARDS_PER_FLOOR + " = " + (floor * SHARDS_PER_FLOOR));
        int kills = r.getKills();
        lines.add("처치 " + kills + " × " + SHARDS_PER_KILL + " = " + (kills * SHARDS_PER_KILL));
        int level = r.getFinalLevel();
        lines.add("레벨 " + level + " × " + SHARDS_PER_LEVEL + " = " + (level * SHARDS_PER_LEVEL));
        int gold = r.getGold();
        lines.add("골드 " + gold + " ÷ " + GOLD_PER_SHARD + " = " + (gold / GOLD_PER_SHARD));

        if (r.isVictory() == true) {
            lines.add("클리어포인트 = " + CLEAR_BONUS);
        }
        lines.add("합계 = " + calculate(r, p));
        return lines;
    }
}

