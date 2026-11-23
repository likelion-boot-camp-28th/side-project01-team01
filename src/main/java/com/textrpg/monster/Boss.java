package com.textrpg.monster;

import com.textrpg.core.Position;

/**
 * 5층 보스 어둠의 파수꾼(ABYSS_WARDEN).
 * 기본적으로 2회 공격하지만 HP가 절반 이하로 떨어지면 분노 상태가 되어 체력을 재생하고 한 턴에 세 번 공격한다.
 * MonsterFactory.create(ABYSS_WARDEN, ...) 또는 createBoss()로 생성한다.
 */
public class Boss extends Monster {

    private static final int ENRAGE_HP_DIVISOR = 2;
    private static final int ENRAGED_ATTACK = 3;
    private static final int NORMAL_ATTACK = 2;
    private static final int ENRAGED_HEAL = 5;

    Boss(Position pos) {
        super(MonsterType.ABYSS_WARDEN, pos);
    }

    /**
     * 이번 턴에 공격할 횟수. HP가 최대 HP의 절반 이하면 3회, 아니면 2회다.
     *
     * @return 이번 턴 공격 횟수
     */
    public int attacksPerTurn() {
        if (isEnraged()) {
            return ENRAGED_ATTACK;
        } else {
            return NORMAL_ATTACK;
        }
    }

    /**
     * HP가 최대 HP의 절반 이하일 때 재생한다.
     */
    public void restore() {
        if (isEnraged()) {
            this.heal(ENRAGED_HEAL);
        }
    }

    private boolean isEnraged() {
        return this.getStats().getHp() <= this.getStats().getMaxHp() / ENRAGE_HP_DIVISOR;
    }
}
