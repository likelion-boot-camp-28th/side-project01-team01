package com.textrpg.game;

import com.textrpg.combat.AttackResult;
import com.textrpg.combat.CombatSystem;
import com.textrpg.combat.MonsterAI;
import com.textrpg.context.GameContext;
import com.textrpg.core.Direction;
import com.textrpg.core.MessageLog;
import com.textrpg.core.Position;
import com.textrpg.entity.Player;
import com.textrpg.item.FloorItem;
import com.textrpg.item.Inventory;
import com.textrpg.item.Item;
import com.textrpg.item.ItemType;
import com.textrpg.map.FieldOfView;
import com.textrpg.map.Floor;
import com.textrpg.monster.Boss;
import com.textrpg.monster.Monster;
import com.textrpg.monster.MonsterType;

/**
 * 탐험 중 한 턴을 처리한다: 플레이어 행동 → (턴을 썼으면) 몬스터 행동 → 턴 마무리.
 * 턴 순서를 이 클래스 한곳에서 정하므로 ExplorationScreen 은 playTurn() 결과만 보고 화면을 바꾼다.
 *
 * <p>가방 보기(INVENTORY), 도움말(HELP), 로그 보기(LOG), 상인과 대화(TALK), 저장 후 종료(SAVE_QUIT)는 턴을 쓰지 않는 화면 명령이라
 * ExplorationScreen 이 직접 처리하고 이 클래스로 넘기지 않는다.
 *
 * <p>턴을 쓰지 못한 행동(NO_TURN)은 이유를 MessageLog 에 남긴다. 아무 반응이 없으면 입력이 무시된 것처럼 보이기 때문이다.
 */
public class TurnSystem {
    private static final int POISON_DURATION = 2;
    private static final int POISON_DAMAGE = 2;

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
     *   <li>턴 마무리: ctx.nextTurn(), 자연 회복. 계단을 내려간 턴(DESCENDED)도 턴 수에 넣는다</li>
     * </ol>
     *
     * @param cmd 실행할 명령. INVENTORY, HELP, LOG, TALK, SAVE_QUIT 는 받지 않는다
     * @param ctx 이번 탐험의 컨텍스트
     * @return 턴 결과. NO_TURN 이면 몬스터는 움직이지 않고 턴 수도 그대로다
     * @throws IllegalArgumentException 화면 명령(INVENTORY, HELP, LOG, TALK, SAVE_QUIT)을 넘겼을 때
     */
    public TurnOutcome playTurn(Command cmd, GameContext ctx) {
        deathCause = "";
        TurnOutcome playerOutcome = executePlayer(cmd, ctx);
        return switch (playerOutcome) {
            case TURN_USED -> {
                if (runMonsters(ctx) == TurnOutcome.DIED) {
                    yield TurnOutcome.DIED;
                }
                yield endOfTurn(ctx);
            }
            case DESCENDED -> {
                if (endOfTurn(ctx) == TurnOutcome.DIED) {
                    yield TurnOutcome.DIED;
                }
                yield TurnOutcome.DESCENDED;
            }
            case NO_TURN, VICTORY, DIED -> playerOutcome;
        };
    }

    /** 플레이어가 쓰러졌을 때 원인 (몬스터 이름). DIED 가 아니면 빈 문자열. RunResult.finish() 에 넘긴다. */
    public String getDeathCause() {
        return deathCause;
    }

    /** 명령 종류에 따라 플레이어 행동을 실행한다. */
    private TurnOutcome executePlayer(Command cmd, GameContext ctx) {
        return switch (cmd.type()) {
            case MOVE -> tryMove(cmd.direction(), ctx);
            case PICK_UP -> pickUp(ctx);
            case USE, EQUIP -> useOrEquip(cmd, ctx);
            case DROP -> drop(cmd.itemIndex(), ctx);
            case DESCEND -> descend(ctx);
            case WAIT -> TurnOutcome.TURN_USED;
            case INVENTORY, HELP, LOG, TALK, SAVE_QUIT -> throw new IllegalArgumentException(
                    "턴을 쓰지 않는 화면 명령은 ExplorationScreen 이 처리합니다: " + cmd.type());
        };
    }

    /**
     * 한 칸 이동한다. 그 칸에 몬스터가 있으면 이동 대신 공격한다.
     * 골드·아이템이 있는 칸에 들어가면 턴을 더 쓰지 않고 자동으로 줍는다 (가방이 가득 차면 안내만 하고 바닥에 남김).
     *
     * 상인이 있는 칸으로는 들어갈 수 없고, 대화하는 방법만 알려 준다.
     *
     * @return 벽·상인이면 NO_TURN, 이동·공격이면 TURN_USED, 공격으로 보스를 쓰러뜨리면 VICTORY
     */
    private TurnOutcome tryMove(Direction d, GameContext ctx) {
        Player player = ctx.getPlayer();
        Floor floor = ctx.getFloor();
        Position next = player.getPos().move(d);

        Monster target = floor.getMonsterAt(next);
        if (target != null) {
            return attack(target, ctx);
        }
        if (floor.isMerchantAt(next)) {
            ctx.getLog().add("상인이 있습니다. t 를 누르면 상점을 이용할 수 있습니다.");
            return TurnOutcome.NO_TURN;
        }
        if (!floor.isWalkable(next)) {
            ctx.getLog().add("벽에 막혀 움직일 수 없습니다.");
            return TurnOutcome.NO_TURN;
        }
        player.setPos(next);
        // 연속 이동(dddd) 중에 지나친 칸도 지도에 남도록 한 칸 움직일 때마다 시야를 갱신한다.
        FieldOfView.update(floor, next);
        FloorItem floorItem = floor.getItemAt(next);
        if (floorItem != null) {
            take(floorItem, ctx);
        }
        return TurnOutcome.TURN_USED;
    }

    /**
     * 몬스터를 공격하고, 쓰러뜨렸으면 C 파트의 rewardKill 로 처치를 마무리한다.
     * rewardKill 이 처치 수·경험치·골드 보상과 층에서 몬스터 제거(floor.removeMonster)까지 하므로 여기서는 부르지 않는다.
     * 보스 처치로 게임이 끝나는지는 이 클래스가 정한다.
     *
     * @return 보스를 쓰러뜨렸으면 VICTORY, 아니면 TURN_USED
     */
    private TurnOutcome attack(Monster target, GameContext ctx) {
        AttackResult result = combat.attack(ctx.getPlayer(), target, ctx.getLog());
        if (!result.killed()) {
            return TurnOutcome.TURN_USED;
        }
        combat.rewardKill(target, ctx);
        return target instanceof Boss ? TurnOutcome.VICTORY : TurnOutcome.TURN_USED;
    }

    /**
     * 발밑의 골드나 아이템을 줍는다 (g). 밟으면 자동으로 줍기 때문에, 가방이 가득 차서 남겨 둔 아이템을 나중에 주울 때 쓴다.
     *
     * @return 주웠으면 TURN_USED, 발밑이 비었거나 가방이 가득 차면 NO_TURN
     */
    private TurnOutcome pickUp(GameContext ctx) {
        FloorItem floorItem = ctx.getFloor().getItemAt(ctx.getPlayer().getPos());
        if (floorItem == null) {
            ctx.getLog().add("주울 것이 없습니다.");
            return TurnOutcome.NO_TURN;
        }
        return take(floorItem, ctx) ? TurnOutcome.TURN_USED : TurnOutcome.NO_TURN;
    }

    /**
     * 바닥의 골드나 아이템을 플레이어에게 옮긴다. 아이템은 가방에 넣은 뒤에 바닥에서 지운다 (가방이 가득 차면 바닥에 그대로 남음).
     *
     * @return 주웠으면 true, 가방이 가득 차서 못 주웠으면 false
     */
    private boolean take(FloorItem floorItem, GameContext ctx) {
        Player player = ctx.getPlayer();
        Floor floor = ctx.getFloor();
        MessageLog log = ctx.getLog();

        if (floorItem.isGold()) {
            player.addGold(floorItem.goldAmount());
            floor.removeItem(floorItem);
            log.add(floorItem.goldAmount() + " 골드를 주웠습니다.");
            return true;
        }
        if (!player.getInventory().add(floorItem.item())) {
            log.add("가방이 가득 차서 " + floorItem.item().getDisplayName() + "을(를) 줍지 못했습니다.");
            return false;
        }
        floor.removeItem(floorItem);
        log.add(floorItem.item().getDisplayName() + "을(를) 주웠습니다.");
        return true;
    }

    /**
     * 가방의 아이템을 사용하거나(USE) 장착한다(EQUIP). 결과 문장은 MessageLog 에 넣는다.
     * 장착은 Player.equip 이 가방에서 새 장비를 빼고 원래 장비를 가방으로 돌려보낸다.
     * 무기·방어구가 아닌 아이템은 상태가 바뀌지 않으므로 턴을 쓰지 않는다.
     *
     * @return 번호가 가방 범위를 벗어나거나 장착할 수 없는 아이템이면 NO_TURN, 아니면 TURN_USED
     */
    private TurnOutcome useOrEquip(Command cmd, GameContext ctx) {
        Player player = ctx.getPlayer();
        int index = cmd.itemIndex();
        if (!hasItemAt(player.getInventory(), index, ctx.getLog())) {
            return TurnOutcome.NO_TURN;
        }
        if (cmd.type() == CommandType.USE) {
            ctx.getLog().add(player.useItem(index));
            return TurnOutcome.TURN_USED;
        }
        Item item = player.getInventory().get(index);
        if (item.getType() != ItemType.WEAPON && item.getType() != ItemType.ARMOR) {
            ctx.getLog().add(item.getDisplayName() + "은(는) 장착할 수 없습니다.");
            return TurnOutcome.NO_TURN;
        }
        ctx.getLog().add(player.equip(item));
        return TurnOutcome.TURN_USED;
    }

    /**
     * 가방의 아이템을 발밑에 버린다. Player 는 Floor 를 모르므로(패키지 의존 방향) 바닥에 놓는 일은 여기서 한다.
     * 한 칸에는 바닥 아이템을 하나만 둔다 (Floor.getItemAt 이 하나만 돌려주므로 겹치면 아래 것을 줍지 못함).
     *
     * @return 번호가 가방 범위를 벗어나거나 발밑에 이미 물건이 있으면 NO_TURN, 아니면 TURN_USED
     */
    private TurnOutcome drop(int index, GameContext ctx) {
        Player player = ctx.getPlayer();
        Floor floor = ctx.getFloor();
        MessageLog log = ctx.getLog();

        if (!hasItemAt(player.getInventory(), index, log)) {
            return TurnOutcome.NO_TURN;
        }
        if (floor.getItemAt(player.getPos()) != null) {
            log.add("여기에는 이미 물건이 있습니다.");
            return TurnOutcome.NO_TURN;
        }
        Item item = player.dropItem(index);
        floor.addItem(FloorItem.of(item, player.getPos()));
        log.add(item.getDisplayName() + "을(를) 버렸습니다.");
        return TurnOutcome.TURN_USED;
    }

    /**
     * 계단 위에서 내려간다. 새 층을 만드는 일은 ExplorationScreen 이 한다.
     * 보스층은 계단이 없어(getStairs() 가 비어 있음) 항상 NO_TURN 이다.
     *
     * @return 계단 위면 DESCENDED, 아니면 NO_TURN
     */
    private TurnOutcome descend(GameContext ctx) {
        Position pos = ctx.getPlayer().getPos();
        boolean onStairs = ctx.getFloor().getStairs().filter(pos::equals).isPresent();
        if (!onStairs) {
            ctx.getLog().add("여기에는 내려갈 계단이 없습니다.");
            return TurnOutcome.NO_TURN;
        }
        return TurnOutcome.DESCENDED;
    }

    private TurnOutcome poisonActivate(GameContext ctx) {
        Player player = ctx.getPlayer();
        if (!player.detox(ctx.getTurn(), POISON_DURATION)) {
            int damage = player.takeDamage(POISON_DAMAGE);
            ctx.getLog().add(player.getName() + "의 체력이 독에 의해 " + damage + "만큼 감소했습니다.");
            if (!player.isAlive()) {
                deathCause = MonsterType.SPIDER.getDisplayName();
                return TurnOutcome.DIED;
            }
            return TurnOutcome.TURN_USED;
        }
        ctx.getLog().add(player.getName() + "의 중독 상태가 해제되었습니다.");
        return TurnOutcome.TURN_USED;
    }

    /**
     * 층의 모든 몬스터가 한 번씩 행동한다. 도중에 플레이어가 쓰러지면 멈추고 deathCause 를 남긴다.
     * getMonsters() 는 복사본이라 행동 도중 몬스터가 층에서 빠져도 순회가 깨지지 않는다.
     *
     * @return 플레이어가 쓰러졌으면 DIED, 아니면 TURN_USED
     */
    private TurnOutcome runMonsters(GameContext ctx) {
        Player player = ctx.getPlayer();
        for (Monster m : ctx.getFloor().getMonsters()) {
            if (!m.isAlive()) {
                continue;
            }
            monsterAI.act(m, ctx);
            if (!player.isAlive()) {
                deathCause = m.getName();
                return TurnOutcome.DIED;
            }
        }
        return TurnOutcome.TURN_USED;
    }

    /**
     * 턴 수를 늘리고 자연 회복을 적용한다.
     *
     * @return 플레이어가 쓰러졌으면 DIED, 아니면 TURN_USED
     */
    private TurnOutcome endOfTurn(GameContext ctx) {
        ctx.getPlayer().regenerate(ctx.nextTurn());
        if (ctx.getPlayer().isPoisoned() && poisonActivate(ctx) == TurnOutcome.DIED) {
            return TurnOutcome.DIED;
        }
        return TurnOutcome.TURN_USED;
    }

    /**
     * 가방에 index 번 아이템이 있는지. 없으면 안내 문장을 남긴다. 음수 번호는 Command 가 이미 막는다.
     *
     * @return index 가 가방 범위 안이면 true
     */
    private boolean hasItemAt(Inventory inventory, int index, MessageLog log) {
        if (index >= inventory.size()) {
            log.add((index + 1) + "번 칸에는 아이템이 없습니다.");
            return false;
        }
        return true;
    }
}
