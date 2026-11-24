package com.textrpg.combat;

import com.textrpg.context.GameContext;
import com.textrpg.core.MessageLog;
import com.textrpg.entity.Entity;
import com.textrpg.monster.Monster;
import com.textrpg.monster.MonsterFactory;
import com.textrpg.monster.MonsterType;

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
     * 처치한 몬스터의 경험치·골드를 플레이어에게 주고, 몬스터를 맵에서 제거한다.
     * 단, 처치한 몬스터가 해골 병사(SKELETON)인 경우 해골 병사가 제거된 자리에 뼈 무더기(PILE_OF_BONES)가 생성된다.
     * 추가적으로 처치한 몬스터가 해골 병사 혹은 보스일 경우 처치 보상이 없어 보상 로그를 남기지 않는다.
     * ctx.getRunResult().addKill()로 처치 수를 기록한다. (해골 병사는 제외)
     *
     * @param monster 처치한 몬스터
     * @param ctx     플레이어와 런 기록을 담은 게임 상태
     */
    public void rewardKill(Monster monster, GameContext ctx) {
        if (monster.getType() != MonsterType.SKELETON) {
            ctx.getRunResult().addKill();
        }
        int exp = monster.getExpReward();
        int gold = monster.getGoldReward();
        boolean isLevelUp = ctx.getPlayer().gainExp(exp);
        ctx.getPlayer().addGold(gold);
        int level = ctx.getPlayer().getLevel();

        ctx.getFloor().removeMonster(monster);
        if (monster.getType() == MonsterType.SKELETON) {
            ctx.getFloor().addMonster(MonsterFactory.createBones(monster.getPos(), ctx.getTurn()));
            ctx.getLog().add("해골 병사가 사망하여 뼈 무더기가 되었습니다. 얼마 후에 해골 병사로 부활합니다. 그전에 처치하세요.");
        } else if (monster.getType() == MonsterType.ABYSS_WARDEN) {
            return;
        } else {
            ctx.getLog().add("처치 보상이 정산됩니다. +" + exp + "exp +" + gold + "gold");
        }
        if (isLevelUp) {
            ctx.getLog().add("레벨이 올랐습니다. (Lv." + level + ")");
        }
    }
}
