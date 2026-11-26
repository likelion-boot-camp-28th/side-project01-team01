package com.textrpg.combat;

import com.textrpg.entity.Entity;
import com.textrpg.util.RandomUtil;

/**
 * 명중, 치명타, 데미지를 확률로 판정하는 계산 전용 클래스.
 * 데미지는 공격력 × K / (K + 방어력) × 0.9~1.1이며 최소 1이고, 치명타는 1.5배다 (K = DEFENSE_CONSTANT).
 * 방어력은 비율로 피해를 줄인다. 방어력 K 이면 절반을 막고, 방어력이 높을수록 1 늘릴 때 줄어드는 비율이 작아진다.
 * 대신 방어력 1 마다 버틸 수 있는 피해량(유효 HP)은 1/K 씩 일정하게 늘어난다 (리그 오브 레전드 방어력 방식).
 * 난수는 RandomUtil을 쓰므로 setSeed()로 결과를 재현할 수 있다.
 */
public final class DamageCalculator {

    private static final int MIN_DAMAGE = 1;
    private static final double DAMAGE_VARIANCE_MIN = 0.9;
    private static final double DAMAGE_VARIANCE_MAX = 1.1;
    private static final double CRITICAL_MULTIPLIER = 1.5;
    /** 방어력 공식의 기준값. 방어력이 이 값이면 피해의 절반을 막는다. */
    private static final double DEFENSE_CONSTANT = 6.0;

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
     * 공격력에 방어 배율 K / (K + 방어력)과 0.9~1.1 편차를 곱하고 최소 1을 보장한다.
     * 예: 공격 12, 방어 0 → 12 / 방어 3 → 8 / 방어 6 → 6
     *
     * @param attacker 공격하는 쪽
     * @param defender 공격받는 쪽
     * @return 기본 데미지 (1 이상)
     */
    public static int calculate(Entity attacker, Entity defender) {
        double baseDamage = attacker.getAttack() * reductionMultiplier(defender.getDefense());
        double variance = RandomUtil.nextDouble(DAMAGE_VARIANCE_MIN, DAMAGE_VARIANCE_MAX);
        return Math.toIntExact(Math.max(MIN_DAMAGE, Math.round(baseDamage * variance)));
    }

    /**
     * 방어력으로 남는 피해 비율. 방어 0 이면 1.0, 방어 DEFENSE_CONSTANT(6) 이면 0.5.
     * 음수 방어력은 0 으로 본다 (피해가 늘어나지 않음).
     *
     * @param defense 방어력
     * @return 0 초과 1 이하의 배율
     */
    static double reductionMultiplier(int defense) {
        return DEFENSE_CONSTANT / (DEFENSE_CONSTANT + Math.max(0, defense));
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
