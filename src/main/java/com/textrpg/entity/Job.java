package com.textrpg.entity;

/**
 * 직업. 직업마다 시작 능력치만 다르다 (P0).
 * 수치는 초안이며, 밸런스를 바꿀 때는 이 enum 만 고친다.
 */
public enum Job {
    //       이름      최대HP 최대MP 공격 방어 명중(%) 치명(%) 회피(%)
    WARRIOR("전사", 40, 5, 7, 4, 90, 5, 5),
    MAGE("마법사", 30, 15, 8, 2, 90, 10, 5),
    ROGUE("도적", 34, 8, 6, 3, 95, 20, 15);

    private final String displayName;
    private final int maxHp;
    private final int maxMp;
    private final int attack;
    private final int defense;
    private final double hitRate;
    private final double critRate;
    private final double evadeRate;

    Job(String displayName, int maxHp, int maxMp, int attack, int defense,
        double hitRate, double critRate, double evadeRate) {
        this.displayName = displayName;
        this.maxHp = maxHp;
        this.maxMp = maxMp;
        this.attack = attack;
        this.defense = defense;
        this.hitRate = hitRate;
        this.critRate = critRate;
        this.evadeRate = evadeRate;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** 이 직업의 시작 능력치를 새 Stats 로 만들어 준다. 캐릭터마다 따로 가지도록 매번 새로 만든다. */
    public Stats createBaseStats() {
        return new Stats(maxHp, maxMp, attack, defense, hitRate, critRate, evadeRate);
    }
}
