package com.textrpg.combat;

import com.textrpg.entity.Entity;
import com.textrpg.util.RandomUtil;

/**
 * 명중, 치명타, 데미지를 확률로 판정하는 계산 전용 클래스.
 * 데미지는 (공격력 - 방어력) × 0.9~1.1이며 최소 1이고, 치명타는 1.5배다.
 * 난수는 RandomUtil을 쓰므로 setSeed()로 결과를 재현할 수 있다.
 */
public final class DamageCalculator {

    private static final int MIN_DAMAGE = 1;
    private static final double DAMAGE_VARIANCE_MIN = 0.9;
    private static final double DAMAGE_VARIANCE_MAX = 1.1;
    private static final double CRITICAL_MULTIPLIER = 1.5;

    private DamageCalculator() {
    }

    /**
     * 공격이 명중했는지 판정한다. attacker의 명중률을 통과하고 defender의 회피율에 걸리지 않아야 명중이다.
     *
     * @param attacker 공격하는 쪽
     * @param defender 공격받는 쪽
     * @return 명중했으면 true
     */
    public static boolean rollHit(Entity attacker, Entity defender) {
        return RandomUtil.chance(attacker.getStats().getHitRate())
                && !RandomUtil.chance(defender.getStats().getEvadeRate());
    }

    /**
     * attacker의 치명타율로 치명타인지 판정한다.
     *
     * @param attacker 공격하는 쪽
     * @return 치명타면 true
     */
    public static boolean rollCritical(Entity attacker) {
        return RandomUtil.chance(attacker.getStats().getCritRate());
    }

    /**
     * 치명타를 반영하기 전의 데미지를 계산한다.
     * (공격력 - 방어력)에 0.9~1.1 편차를 곱하고 최소 1을 보장한다.
     *
     * @param attacker 공격하는 쪽
     * @param defender 공격받는 쪽
     * @return 기본 데미지 (1 이상)
     */
    public static int calculate(Entity attacker, Entity defender) {
        int baseDamage = attacker.getAttack() - defender.getDefense();
        double variance = RandomUtil.nextDouble(DAMAGE_VARIANCE_MIN, DAMAGE_VARIANCE_MAX);
        return Math.toIntExact(Math.max(MIN_DAMAGE, Math.round(baseDamage * variance)));
    }

    /**
     * 데미지에 치명타 배율(1.5배)을 적용한다. 소수점은 반올림한다.
     *
     * @param damage calculate()로 구한 기본 데미지
     * @return 치명타가 적용된 데미지
     */
    public static int applyCritical(int damage) {
        return Math.toIntExact(Math.round(damage * CRITICAL_MULTIPLIER));
    }
}
