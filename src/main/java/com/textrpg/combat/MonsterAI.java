package com.textrpg.combat;

import java.util.List;
import com.textrpg.context.GameContext;
import com.textrpg.core.Direction;
import com.textrpg.core.MessageLog;
import com.textrpg.core.Position;
import com.textrpg.entity.Player;
import com.textrpg.map.Floor;
import com.textrpg.monster.Boss;
import com.textrpg.monster.Monster;
import com.textrpg.monster.MonsterType;
import com.textrpg.util.RandomUtil;


/**
 * 몬스터 한 마리의 턴 행동(대기, 추적, 공격)을 결정한다.
 * 플레이어와 맞닿으면 CombatSystem으로 공격을 넘긴다.
 */
public class MonsterAI {

    private static final int SIGHT_RANGE = 6;
    private static final int ZERO_SPEED_ACTION_INTERVAL = 2;
    private final CombatSystem combat = new CombatSystem();

    /**
     * monster의 이번 턴 행동을 한 번 수행한다.
     *
     * @param monster 행동할 몬스터
     * @param ctx     플레이어와 현재 층을 담은 게임 상태
     */
    public void act(Monster monster, GameContext ctx) {
        Player player = ctx.getPlayer();
        Floor floor = ctx.getFloor();
        MessageLog log = ctx.getLog();
        if (!monster.isAwake() && canSee(monster, player, floor)) {
            monster.wakeUp();
        }

        if (player.isAlive() && monster.isAwake()) {
            int actionCount = monster.getType().getSpeed();

            if (monster instanceof Boss) {
                actionCount = ((Boss) monster).attacksPerTurn();
            }

            for (int i = 0; i < actionCount; i++) {
                moveOrAttack(monster, player, floor, log);
            }

            if (ctx.getTurn() % ZERO_SPEED_ACTION_INTERVAL == 0 && monster.getType().getSpeed() == 0) {
                moveOrAttack(monster, player, floor, log);
            }
        }
    }

    private void moveOrAttack(Monster monster, Player player, Floor floor, MessageLog log) {
        if (monster.getPos().isAdjacent(player.getPos())) {
            combat.attack(monster, player, log);
            return;
        }
        if (monster.getType() == MonsterType.BAT) {
            wander(monster, floor);
        } else {
            Direction direction = stepToward(monster, player.getPos(), floor);
            if (direction == null) {
                return;
            }
            Position newPos = monster.getPos().move(direction);
            if (floor.isWalkable(newPos) && floor.getMonsterAt(newPos) == null) {
                monster.setPos(newPos);
            }
        }
    }

    private boolean canSee(Monster monster, Player player, Floor floor) {
        boolean isSame = false;
        if (floor.roomAt(monster.getPos()) != null && floor.roomAt(player.getPos()) != null) {
            isSame = floor.roomAt(monster.getPos()) == floor.roomAt(player.getPos());
        }
        boolean isInSight = monster.getPos().distanceTo(player.getPos()) <= SIGHT_RANGE;
        return isSame || isInSight;
    }

    private Direction stepToward(Monster monster, Position target, Floor floor) {
        int dx = target.x() - monster.getPos().x();
        int dy = target.y() - monster.getPos().y();
        Direction direction;

        if (Math.abs(dx) >= Math.abs(dy)) {
            direction = directionOnX(dx);
        } else {
            direction = directionOnY(dy);
        }

        Position newPos = monster.getPos().move(direction);
        if (floor.isWalkable(newPos) && floor.getMonsterAt(newPos) == null) {
            return direction;
        }

        if (Math.abs(dx) < Math.abs(dy)) {
            if (dx == 0) {
                return null;
            }
            direction = directionOnX(dx);
        } else {
            if (dy == 0) {
                return null;
            }
            direction = directionOnY(dy);
        }
        return direction;
    }

    private Direction directionOnX(int dx) {
        if (dx > 0) {
            return Direction.RIGHT;
        } else {
            return Direction.LEFT;
        }
    }

    private Direction directionOnY(int dy) {
        if (dy > 0) {
            return Direction.DOWN;
        } else {
            return Direction.UP;
        }
    }

    private void wander(Monster monster, Floor floor) {
        Direction direction = RandomUtil.pick(List.of(Direction.values()));
        Position newPos = monster.getPos().move(direction);
        if (floor.isWalkable(newPos) && floor.getMonsterAt(newPos) == null) {
            monster.setPos(newPos);
        }
    }
}
