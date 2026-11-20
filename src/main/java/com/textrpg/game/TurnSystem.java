package com.textrpg.game;

import com.textrpg.combat.CombatSystem;
import com.textrpg.combat.MonsterAI;
import com.textrpg.context.GameContext;
import com.textrpg.core.Direction;
import com.textrpg.monster.Monster;

/**
 * 탐험 중 한 턴을 처리한다: 플레이어 행동 → (턴을 썼으면) 몬스터 행동 → 턴 마무리.
 * 턴 순서를 이 클래스 한곳에서 정하므로 GameManager 는 playTurn() 결과만 보고 화면을 바꾼다.
 *
 * <p>가방 보기(INVENTORY), 도움말(HELP), 저장 후 종료(SAVE_QUIT)는 턴을 쓰지 않는 화면 명령이라
 * GameManager 가 직접 처리하고 이 클래스로 넘기지 않는다.
 */
public class TurnSystem {

    private final CombatSystem combat;
    private final MonsterAI monsterAI;
    /** 마지막으로 플레이어를 쓰러뜨린 몬스터 이름. playTurn() 이 DIED 를 돌려준 직후에만 의미가 있다. */
    private String deathCause = "";

    public TurnSystem(CombatSystem combat, MonsterAI monsterAI) {
        this.combat = combat;
        this.monsterAI = monsterAI;
    }

    /**
     * 플레이어 명령 하나로 한 턴을 진행한다.
     * <ol>
     *   <li>플레이어 행동 (이동·공격, 줍기, 사용, 장착, 버리기, 내려가기, 대기)</li>
     *   <li>TURN_USED 이면 모든 몬스터 행동. 플레이어가 쓰러지면 DIED</li>
     *   <li>턴 마무리: ctx.nextTurn(), 자연 회복</li>
     * </ol>
     *
     * @param cmd 실행할 명령. INVENTORY, HELP, SAVE_QUIT 는 받지 않는다
     * @param ctx 이번 탐험의 컨텍스트
     * @return 턴 결과. NO_TURN 이면 몬스터는 움직이지 않고 턴 수도 그대로다
     * @throws IllegalArgumentException 화면 명령(INVENTORY, HELP, SAVE_QUIT)을 넘겼을 때
     */
    public TurnOutcome playTurn(Command cmd, GameContext ctx) {
        // TODO(A): executePlayer → TURN_USED 면 runMonsters → 살아 있으면 endOfTurn
        throw new UnsupportedOperationException("TurnSystem.playTurn 은 아직 구현되지 않았습니다.");
    }

    /** 플레이어가 쓰러졌을 때 원인 (몬스터 이름). DIED 가 아니면 빈 문자열. RunResult.finish() 에 넘긴다. */
    public String getDeathCause() {
        return deathCause;
    }

    /** 명령 종류에 따라 플레이어 행동을 실행한다. */
    private TurnOutcome executePlayer(Command cmd, GameContext ctx) {
        // TODO(A): switch (cmd.type()) → tryMove / pickUp / useItem / equip / drop / descend / WAIT 는 TURN_USED
        throw new UnsupportedOperationException("TurnSystem.executePlayer 는 아직 구현되지 않았습니다.");
    }

    /**
     * 한 칸 이동한다. 그 칸에 몬스터가 있으면 이동 대신 공격한다.
     *
     * @return 벽이면 NO_TURN, 이동·공격이면 TURN_USED, 공격으로 보스를 쓰러뜨리면 VICTORY
     */
    private TurnOutcome tryMove(Direction d, GameContext ctx) {
        // TODO(A): next = pos.move(d) → floor.getMonsterAt(next) 있으면 attack
        //  → 없으면 floor.isWalkable(next) 일 때 player.setPos(next)
        throw new UnsupportedOperationException("TurnSystem.tryMove 는 아직 구현되지 않았습니다.");
    }

    /**
     * 몬스터를 공격하고, 쓰러뜨렸으면 C 파트의 rewardKill 로 보상(경험치·골드·처치 수)을 준다.
     *
     * @return 보스를 쓰러뜨렸으면 VICTORY, 아니면 TURN_USED
     */
    private TurnOutcome attack(Monster target, GameContext ctx) {
        // TODO(A): combat.attack(player, target, log) → killed 면 floor.removeMonster + combat.rewardKill(target, ctx)
        //  → target 이 Boss 면 VICTORY
        throw new UnsupportedOperationException("TurnSystem.attack 은 아직 구현되지 않았습니다.");
    }

    /**
     * 발밑의 골드나 아이템을 줍는다.
     *
     * @return 주웠으면 TURN_USED, 발밑이 비었거나 가방이 가득 차면 NO_TURN
     */
    private TurnOutcome pickUp(GameContext ctx) {
        // TODO(A): floor.getItemAt(pos) → 골드면 player.addGold, 아이템이면 inventory.add → floor.removeItem
        throw new UnsupportedOperationException("TurnSystem.pickUp 은 아직 구현되지 않았습니다.");
    }

    /**
     * 가방의 아이템을 사용하거나(USE) 장착한다(EQUIP). 결과 문장은 MessageLog 에 넣는다.
     *
     * @return 번호가 가방 범위를 벗어나면 NO_TURN, 아니면 TURN_USED
     */
    private TurnOutcome useOrEquip(Command cmd, GameContext ctx) {
        // TODO(A): 번호 검사 → USE 면 player.useItem(i), EQUIP 이면 player.equip(inventory.get(i)) → log.add(결과)
        throw new UnsupportedOperationException("TurnSystem.useOrEquip 은 아직 구현되지 않았습니다.");
    }

    /**
     * 가방의 아이템을 발밑에 버린다. Player 는 Floor 를 모르므로(패키지 의존 방향) 바닥에 놓는 일은 여기서 한다.
     *
     * @return 번호가 가방 범위를 벗어나면 NO_TURN, 아니면 TURN_USED
     */
    private TurnOutcome drop(int index, GameContext ctx) {
        // TODO(A): item = player.dropItem(index) → floor.addItem(FloorItem.of(item, player.getPos()))
        throw new UnsupportedOperationException("TurnSystem.drop 은 아직 구현되지 않았습니다.");
    }

    /**
     * 계단 위에서 내려간다. 새 층을 만드는 일은 GameManager 가 한다.
     *
     * @return 계단 위면 DESCENDED, 아니면 NO_TURN
     */
    private TurnOutcome descend(GameContext ctx) {
        // TODO(A): floor.getStairs() 가 player.getPos() 와 같으면 DESCENDED
        throw new UnsupportedOperationException("TurnSystem.descend 는 아직 구현되지 않았습니다.");
    }

    /**
     * 층의 모든 몬스터가 한 번씩 행동한다. 도중에 플레이어가 쓰러지면 멈추고 deathCause 를 남긴다.
     *
     * @return 플레이어가 쓰러졌으면 DIED, 아니면 TURN_USED
     */
    private TurnOutcome runMonsters(GameContext ctx) {
        // TODO(A): floor.getMonsters() 순회 (죽은 몬스터 건너뜀) → monsterAI.act(m, ctx) → !player.isAlive() 면 DIED
        throw new UnsupportedOperationException("TurnSystem.runMonsters 는 아직 구현되지 않았습니다.");
    }

    /** 턴 수를 늘리고 자연 회복을 적용한다. */
    private void endOfTurn(GameContext ctx) {
        // TODO(A): player.regenerate(ctx.nextTurn())
        throw new UnsupportedOperationException("TurnSystem.endOfTurn 은 아직 구현되지 않았습니다.");
    }
}
