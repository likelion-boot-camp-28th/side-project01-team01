package com.textrpg.game;

/**
 * 플레이어 명령의 종류. 명령마다 분류(Kind)를 하나씩 가진다.
 * <ul>
 *   <li>TURN: 턴을 쓰는 행동. TurnSystem 이 처리한다.</li>
 *   <li>ITEM: 가방 번호(Command.itemIndex)가 필요한 행동. 턴을 쓰며 TurnSystem 이 처리한다.</li>
 *   <li>SCREEN: 턴을 쓰지 않고 다른 화면을 여는 명령. ExplorationScreen 이 처리한다.</li>
 * </ul>
 * 분류를 여기 한곳에 두어, 명령을 추가할 때 분류를 빠뜨리면 컴파일 오류가 나게 한다.
 */
public enum CommandType {
    MOVE(Kind.TURN),
    PICK_UP(Kind.TURN),
    INVENTORY(Kind.SCREEN),
    USE(Kind.ITEM),
    EQUIP(Kind.ITEM),
    DROP(Kind.ITEM),
    DESCEND(Kind.TURN),
    SAVE_QUIT(Kind.SCREEN),
    HELP(Kind.SCREEN),
    LOG(Kind.SCREEN),
    TALK(Kind.SCREEN),
    WAIT(Kind.TURN);

    /** 명령 분류. boolean 두 개 대신 enum 을 써서 "아이템 명령이면서 화면 명령" 같은 조합을 만들 수 없게 한다. */
    private enum Kind { TURN, ITEM, SCREEN }

    private final Kind kind;

    CommandType(Kind kind) {
        this.kind = kind;
    }

    /** 가방 번호가 필요한 명령(USE, EQUIP, DROP)인지. */
    public boolean isItemCommand() {
        return kind == Kind.ITEM;
    }

    /** 턴을 쓰지 않고 다른 화면을 여는 명령(INVENTORY, HELP, LOG, TALK, SAVE_QUIT)인지. CommandParser 가 한 줄에 하나만 만든다. */
    public boolean isScreenCommand() {
        return kind == Kind.SCREEN;
    }
}
