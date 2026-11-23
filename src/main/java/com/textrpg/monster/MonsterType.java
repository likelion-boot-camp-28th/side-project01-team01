package com.textrpg.monster;

/**
 * 몬스터 종류별 등장 층 범위와 기본 수치를 모아 둔 enum.
 * 몬스터 밸런스를 조정할 때는 이 enum만 수정한다.
 * ABYSS_WARDEN은 반드시 마지막에 둔다.
 * MonsterFactory.createRandom()이 ordinal로 보스를 제외하기 때문이다.
 */
public enum MonsterType {

    SLIME("슬라임", 'S', 1, 2, 8, 3, 0, 3, 3, 0),
    BAT("박쥐", 'B', 1, 2, 6, 3, 1, 3, 3, 1),
    GOBLIN("고블린", 'G', 1, 3, 12, 5, 1, 6, 25, 1),
    WOLF("늑대", 'W', 2, 3, 14, 6, 2, 8, 8, 2),
    SKELETON("해골 병사", 'K', 3, 4, 20, 8, 3, 0, 0, 1),
    // -1이라는 speed 값은 행동 안함을 의미
    PILE_OF_BONES("뼈 무더기", 'P', 0, 0, 10, 0, 3, 14, 14, -1),
    ORC("오크", 'O', 3, 5, 40, 9, 4, 18, 18, 1),
    SPIDER("독거미", 'V', 4, 5, 18, 8, 2, 16, 16, 1),
    SHADOW_KNIGHT("그림자 기사", 'D', 4, 5, 32, 11, 6, 25, 25, 1),
    ABYSS_WARDEN("어둠의 파수꾼", '&', 5, 5, 120, 14, 7, 0, 0, 2);

    private static final int BASE_HIT_RATE = 70;
    private static final int HIT_RATE_PER_SPEED = 15;
    private static final int CRIT_RATE_PER_SPEED = 5;
    private static final int EVADE_RATE_PER_SPEED = 5;

    private final String displayName;
    private final char symbol;
    private final int minFloor;
    private final int maxFloor;
    private final int hp;
    private final int attack;
    private final int defense;
    private final int exp;
    private final int gold;
    private final int speed;
    private final double hitRate;
    private final double critRate;
    private final double evadeRate;

    MonsterType(String displayName, char symbol, int minFloor, int maxFloor,
                int hp, int attack, int defense, int exp, int gold, int speed) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
        this.hp = hp;
        this.attack = attack;
        this.defense = defense;
        this.exp = exp;
        this.gold = gold;
        this.speed = speed;
        hitRate = BASE_HIT_RATE + HIT_RATE_PER_SPEED * speed;
        critRate = CRIT_RATE_PER_SPEED * speed;
        evadeRate = EVADE_RATE_PER_SPEED * speed;
    }

    public String getDisplayName() {
        return displayName;
    }

    public char getSymbol() {
        return symbol;
    }

    public int getMinFloor() {
        return minFloor;
    }

    public int getMaxFloor() {
        return maxFloor;
    }

    public int getHp() {
        return hp;
    }

    public int getAttack() {
        return attack;
    }

    public int getDefense() {
        return defense;
    }

    public int getExp() {
        return exp;
    }

    public int getGold() {
        return gold;
    }

    public int getSpeed() {
        return speed;
    }

    public double getHitRate() {
        return hitRate;
    }

    public double getCritRate() {
        return critRate;
    }

    public double getEvadeRate() {
        return evadeRate;
    }
}
