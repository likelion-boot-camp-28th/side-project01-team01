package com.textrpg.combat;

import java.util.List;
import java.util.Objects;

import com.textrpg.core.Position;
import com.textrpg.entity.Entity;
import com.textrpg.entity.Job;
import com.textrpg.entity.Player;
import com.textrpg.monster.Monster;
import com.textrpg.monster.MonsterFactory;
import com.textrpg.monster.MonsterType;

/**
 * DamageCalculator 확인용: 비율 방어 공식, 방어력이 높을수록 줄어드는 추가 효율, 최소 데미지.
 * JUnit 대신 main 으로 실행한다 (Team_Rules 2.11). 실패가 하나라도 있으면 종료 코드 1.
 */
public final class DamageCalculatorCheck {

    private static final int TRIES = 200;

    private static int passed;
    private static int failed;

    private DamageCalculatorCheck() {
    }

    public static void main(String[] args) {
        checkMultiplier();
        checkDiminishingReturns();
        checkCalculate();

        System.out.println("통과 " + passed + " / 실패 " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void checkMultiplier() {
        expect("방어 0 은 그대로", DamageCalculator.reductionMultiplier(0), 1.0);
        expect("방어 6 은 절반", DamageCalculator.reductionMultiplier(6), 0.5);
        expect("방어 18 은 1/4", DamageCalculator.reductionMultiplier(18), 0.25);
        expect("음수 방어는 0 으로", DamageCalculator.reductionMultiplier(-5), 1.0);
    }

    /** 방어력 1 을 더할 때 줄어드는 비율은 방어력이 낮을 때 더 크다. 유효 HP 는 1/K 씩 일정하게 는다. */
    private static void checkDiminishingReturns() {
        double low = DamageCalculator.reductionMultiplier(0) - DamageCalculator.reductionMultiplier(1);
        double mid = DamageCalculator.reductionMultiplier(5) - DamageCalculator.reductionMultiplier(6);
        double high = DamageCalculator.reductionMultiplier(10) - DamageCalculator.reductionMultiplier(11);
        expect("낮은 방어일수록 추가 효율이 큼", low > mid && mid > high, true);

        double ehp5 = 1 / DamageCalculator.reductionMultiplier(5);
        double ehp6 = 1 / DamageCalculator.reductionMultiplier(6);
        double ehp11 = 1 / DamageCalculator.reductionMultiplier(11);
        double ehp12 = 1 / DamageCalculator.reductionMultiplier(12);
        expect("유효 HP 증가량은 일정", Math.abs((ehp6 - ehp5) - (ehp12 - ehp11)) < 1e-9, true);
    }

    private static void checkCalculate() {
        Player warrior = new Player("전사", Job.WARRIOR);              // 공격 5, 방어 3
        Monster slime = monster(MonsterType.SLIME);                    // 공격 3, 방어 0
        Monster knight = monster(MonsterType.SHADOW_KNIGHT);           // 공격 11, 방어 6

        expectRange("방어 0 상대: 공격 5 × 0.9~1.1", warrior, slime, List.of(5, 6));
        // 5 × 6/12 = 2.5 → 2.25 ~ 2.75
        expectRange("방어 6 상대: 공격 5 × 6/12", warrior, knight, List.of(2, 3));
        // 11 × 6/9 = 7.33 → 6.6 ~ 8.07
        expectRange("방어 3 상대: 공격 11 × 6/9", knight, warrior, List.of(7, 8));

        Player wall = Player.restore("벽", Job.WARRIOR, 1, 0, 0, 30, 30, 5, 5, 5, 1000,
                new Position(0, 0), null, null, List.of());
        expectRange("아무리 방어가 높아도 최소 1", slime, wall, List.of(1));
    }

    private static Monster monster(MonsterType type) {
        return MonsterFactory.create(type, new Position(0, 0));
    }

    /** TRIES 번 계산해서 모든 결과가 allowed 안에 있어야 통과. */
    private static void expectRange(String name, Entity attacker,
                                    Entity defender, List<Integer> allowed) {
        for (int i = 0; i < TRIES; i++) {
            int damage = DamageCalculator.calculate(attacker, defender);
            if (!allowed.contains(damage)) {
                failed++;
                System.out.println("[실패] " + name + ": 허용 " + allowed + " / 실제 " + damage);
                return;
            }
        }
        passed++;
    }

    private static void expect(String name, Object actual, Object expected) {
        if (Objects.equals(actual, expected)) {
            passed++;
        } else {
            failed++;
            System.out.println("[실패] " + name + ": 기대 " + expected + " / 실제 " + actual);
        }
    }
}
