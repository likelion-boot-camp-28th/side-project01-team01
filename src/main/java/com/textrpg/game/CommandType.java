package com.textrpg.game;

/**
 * 플레이어 명령의 종류.
 * itemCommand 가 true 인 명령은 가방 번호(Command.itemIndex)가 함께 있어야 한다.
 */
public enum CommandType {
    MOVE(false),
    PICK_UP(false),
    INVENTORY(false),
    USE(true),
    EQUIP(true),
    DROP(true),
    DESCEND(false),
    SAVE_QUIT(false),
    HELP(false),
    LOG(false),
    WAIT(false);

    private final boolean itemCommand;

    CommandType(boolean itemCommand) {
        this.itemCommand = itemCommand;
    }

    /** 가방 번호가 필요한 명령(USE, EQUIP, DROP)인지. */
    public boolean isItemCommand() {
        return itemCommand;
    }
}
