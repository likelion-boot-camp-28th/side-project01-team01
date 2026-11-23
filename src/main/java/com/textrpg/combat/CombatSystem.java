package com.textrpg.combat;

import com.textrpg.context.GameContext;
import com.textrpg.core.MessageLog;
import com.textrpg.entity.Entity;
import com.textrpg.monster.Monster;

/**
 * 플레이어와 몬스터가 부딪혔을 때의 공격 한 번과 처치 보상을 처리한다.
 * 명중·치명타·데미지 판정은 DamageCalculator에 맡기고,
 * 전투 문장은 MessageLog에 남긴다.
 */
public class CombatSystem {

    /**
     * attacker가 defender를 한 번 공격한다. 명중·치명타·데미지를 판정해 defender의 HP를 깎고,
     * 결과 문장을 log에 남긴다.
     *
     * @param attacker 공격하는 쪽
     * @param defender 공격받는 쪽
     * @param log      전투 문장을 남길 메시지 로그
     * @return 이번 공격의 결과
     */
    public AttackResult attack(Entity attacker, Entity defender, MessageLog log) {
        boolean hit = DamageCalculator.rollHit(attacker, defender);
        boolean critical = false;
        int damage = 0;
        if (hit) {
            String resultText = "명중했습니다.";
            damage = DamageCalculator.calculate(attacker, defender);
            critical = DamageCalculator.rollCritical(attacker);
            if (critical) {
                resultText = "치명타로 " + resultText;
                damage = DamageCalculator.applyCritical(damage);
            }
            log.add(attacker.getName() + "의 공격이 " + defender.getName() + "에게 " + resultText);
            damage = defender.takeDamage(damage);
            log.add(defender.getName() + "의 체력이 " + damage + "만큼 감소했습니다.");
        } else {
            log.add(attacker.getName() + "의 공격이 " + defender.getName() + "에게 빗나갔습니다.");
        }
        boolean killed = hit && !defender.isAlive();
        if (killed) {
            log.add(attacker.getName() + "에 의해 " + defender.getName() + "(이)가 사망하였습니다.");
        }
        return new AttackResult(hit, critical, damage, killed);
    }

    /**
     * 처치한 몬스터의 경험치·골드를 플레이어에게 주고,
     * ctx.getRunResult().addKill()로 처치 수를 기록한다.
     *
     * @param monster 처치한 몬스터
     * @param ctx     플레이어와 런 기록을 담은 게임 상태
     */
    public void rewardKill(Monster monster, GameContext ctx) {
        ctx.getRunResult().addKill();

        int exp = monster.getExpReward();
        int gold = monster.getGoldReward();
        ctx.getPlayer().gainExp(exp);
        ctx.getPlayer().addGold(gold);
        ctx.getLog().add("처치 보상이 정산됩니다. +" + exp + "exp +" + gold + "gold");

        ctx.getFloor().removeMonster(monster);
        ctx.getLog().add("처치된 몬스터가 맵에서 제거됩니다.");
    }
}
