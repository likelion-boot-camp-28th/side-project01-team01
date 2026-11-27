package com.textrpg.game;

import java.util.Objects;

import com.textrpg.core.Direction;

/**
 * 플레이어 명령 하나. CommandParser 가 만들고, 화면 명령은 ExplorationScreen 이, 나머지는 TurnSystem 이 실행한다.
 * 생성자 대신 정적 팩토리(move, of, item)로 만들고, type 과 맞지 않는 값 조합은 생성 시점에 막는다.
 *
 * @param type      명령 종류
 * @param direction MOVE 일 때만 값이 있고, 그 외에는 null
 * @param itemIndex 아이템 명령일 때 가방 번호(0부터), 그 외에는 {@link #NO_ITEM}
 */
public record Command(CommandType type, Direction direction, int itemIndex) {

    /** 가방 번호가 필요 없는 명령의 itemIndex 값. */
    public static final int NO_ITEM = -1;

    /**
     * @throws IllegalArgumentException type 과 direction / itemIndex 조합이 맞지 않을 때
     */
    public Command {
        Objects.requireNonNull(type, "type");
        if ((type == CommandType.MOVE) == (direction == null)) {
            throw new IllegalArgumentException("MOVE 에만 방향이 있어야 합니다: " + type + ", " + direction);
        }
        if (type.isItemCommand() && itemIndex < 0) {
            throw new IllegalArgumentException(type + " 에는 0 이상의 가방 번호가 필요합니다: " + itemIndex);
        }
        if (!type.isItemCommand() && itemIndex != NO_ITEM) {
            throw new IllegalArgumentException(type + " 에는 가방 번호를 넣을 수 없습니다: " + itemIndex);
        }
    }

    /** 이동 명령. 이동 방향에 몬스터가 있으면 TurnSystem 이 공격으로 처리한다. */
    public static Command move(Direction direction) {
        return new Command(CommandType.MOVE, direction, NO_ITEM);
    }

    /**
     * 방향도 가방 번호도 없는 명령 (PICK_UP, INVENTORY, DESCEND, SAVE_QUIT, HELP, LOG, TALK, WAIT).
     *
     * @throws IllegalArgumentException MOVE 나 아이템 명령을 넘겼을 때
     */
    public static Command of(CommandType type) {
        return new Command(type, null, NO_ITEM);
    }

    /**
     * 가방 번호가 필요한 명령 (USE, EQUIP, DROP).
     *
     * @param itemIndex 가방 번호(0부터). 화면에 보이는 번호(1부터)는 CommandParser 가 바꿔서 넘긴다.
     * @throws IllegalArgumentException 아이템 명령이 아니거나 번호가 음수일 때
     */
    public static Command item(CommandType type, int itemIndex) {
        return new Command(type, null, itemIndex);
    }
}
