package com.textrpg.monster;

import com.textrpg.core.Position;
import com.textrpg.entity.Entity;
import com.textrpg.entity.Stats;

/**
 * 던전의 일반 몬스터. 기본 수치는 MonsterType에서 가져온다.
 * 명중·치명·회피율은 MonsterType의 speed로 계산한다.
 * 세이브 복원과 같은 경로를 쓰기 위해 반드시 MonsterFactory로 생성한다.
 */
public class Monster extends Entity {

    private final MonsterType type;
    private boolean awake;

    Monster(MonsterType type, Position pos) {
        super(type.getDisplayName(), type.getSymbol(), pos, createStats(type));
        this.type = type;
        this.awake = false;
    }

    private static Stats createStats(MonsterType type) {
        int maxHp = type.getHp();
        int maxMp = 0;
        int attack = type.getAttack();
        int defense = type.getDefense();
        double hitRate = type.getHitRate();
        double critRate = type.getCritRate();
        double evadeRate = type.getEvadeRate();
        return new Stats(maxHp, maxMp, attack, defense, hitRate, critRate, evadeRate);
    }

    public MonsterType getType() {
        return type;
    }

    @Override
    public int getAttack() {
        return type.getAttack();
    }

    @Override
    public int getDefense() {
        return type.getDefense();
    }

    /**
     * 이 몬스터를 처치했을 때 얻는 경험치.
     *
     * @return 경험치 보상
     */
    public int getExpReward() {
        return type.getExp();
    }

    /**
     * 이 몬스터를 처치했을 때 얻는 골드.
     *
     * @return 골드 보상
     */
    public int getGoldReward() {
        return type.getGold();
    }

    /**
     * 몬스터가 깨어 있는지 확인한다. 생성 직후에는 잠든 상태다.
     *
     * @return 깨어 있으면 true
     */
    public boolean isAwake() {
        return awake;
    }

    /** 몬스터를 깨운다. 이미 깨어 있으면 아무 변화가 없다. */
    public void wakeUp() {
        awake = true;
    }
}
