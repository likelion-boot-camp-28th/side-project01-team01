package com.textrpg.meta;

/**
 * 어둠의 제단에서 파는 영구 강화 목록 (기획서 2.6 영구 강화 표).
 * 비용 개수 = 최대 단계. 수치를 바꿀 때는 이 enum 만 고친다.
 */
public enum UpgradeType {
    //          이름                  설명                  단계당 효과  단계별 비용
    MAX_HP("강인한 육체", "최대 HP +5", 5, 20, 40, 60, 80, 100),
    ATTACK("날카로운 칼날", "공격 +1", 1, 30, 60, 90),
    DEFENSE("단단한 피부", "방어 +1", 1, 30, 60, 90),
    ACCURACY("매의 눈", "명중률 +3%", 3, 30, 60, 90),
    START_POTION("보급품", "시작 시 치유 물약 +1", 1, 25, 50),
    BAG_SIZE("큰 가방", "가방 칸 +2", 2, 40, 80),
    EXP_BOOST("배움의 축복", "경험치 획득 +10%", 10, 35, 70, 105),
    SHARD_BOOST("파편 수집가", "파편 획득 +10%", 10, 50, 100, 150),
    UNLOCK_MAGE("직업 해금: 마법사", "마법사 선택 가능", 1, 60),
    UNLOCK_ROGUE("직업 해금: 도적", "도적 선택 가능", 1, 60);

    private final String displayName;
    private final String description;
    private final int valuePerLevel;
    private final int[] costs;

    UpgradeType(String displayName, String description, int valuePerLevel, int... costs) {
        this.displayName = displayName;
        this.description = description;
        this.valuePerLevel = valuePerLevel;
        this.costs = costs;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getValuePerLevel() {
        return valuePerLevel;
    }

    public int getMaxLevel() {
        return costs.length;
    }

    /**
     * nextLevel 단계로 올리는 비용. 예: MAX_HP.costFor(1) == 20.
     * 1 ~ 최대 단계 밖이면 IllegalArgumentException.
     */
    public int costFor(int nextLevel) {
        if (nextLevel < 1 || nextLevel > getMaxLevel()) {
            throw new IllegalArgumentException(name() + " 에 " + nextLevel + " 단계는 없습니다.");
        }
        return costs[nextLevel - 1];
    }
}
