package com.textrpg.game;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.textrpg.CheckRunner;
import com.textrpg.core.Direction;

import static com.textrpg.CheckRunner.check;
import static com.textrpg.CheckRunner.expectThrows;

/**
 * CommandParser / Command 확인용. JUnit 대신 main 으로 실행한다 (Team_Rules 2.11).
 * 실패가 하나라도 있으면 종료 코드 1.
 */
public final class CommandParserCheck {

    private CommandParserCheck() {
    }

    public static void main(String[] args) {
        checkEmptyInput();
        checkMoves();
        checkSingleKeys();
        checkItemCommands();
        checkUnknownInput();
        checkCommandValidation();
        checkCommandKinds();

        CheckRunner.finish();
    }

    private static void checkEmptyInput() {
        expect(null, List.of());
        expect("", List.of());
        expect("   ", List.of());
        // 전각 공백(U+3000)도 공백으로 본다
        expect("　", List.of());
    }

    private static void checkMoves() {
        expect("w", List.of(Command.move(Direction.UP)));
        expect("a", List.of(Command.move(Direction.LEFT)));
        expect("s", List.of(Command.move(Direction.DOWN)));
        expect("d", List.of(Command.move(Direction.RIGHT)));
        expect("W", List.of(Command.move(Direction.UP)));
        expect("  d  ", List.of(Command.move(Direction.RIGHT)));
        expect("d　", List.of(Command.move(Direction.RIGHT)));
        expect("ddd", List.of(Command.move(Direction.RIGHT), Command.move(Direction.RIGHT),
                Command.move(Direction.RIGHT)));
        expect("wasd", List.of(Command.move(Direction.UP), Command.move(Direction.LEFT),
                Command.move(Direction.DOWN), Command.move(Direction.RIGHT)));
        // 대문자 S 도 저장이 아니라 아래 이동
        expect("S", List.of(Command.move(Direction.DOWN)));
        expect("SS", List.of(Command.move(Direction.DOWN), Command.move(Direction.DOWN)));
    }

    private static void checkSingleKeys() {
        expect("g", List.of(Command.of(CommandType.PICK_UP)));
        expect("G", List.of(Command.of(CommandType.PICK_UP)));
        expect("i", List.of(Command.of(CommandType.INVENTORY)));
        expect(">", List.of(Command.of(CommandType.DESCEND)));
        expect("save", List.of(Command.of(CommandType.SAVE_QUIT)));
        expect("SAVE", List.of(Command.of(CommandType.SAVE_QUIT)));
        expect("  Save  ", List.of(Command.of(CommandType.SAVE_QUIT)));
        expect("?", List.of(Command.of(CommandType.HELP)));
        expect("l", List.of(Command.of(CommandType.LOG)));
        expect("L", List.of(Command.of(CommandType.LOG)));
        expect(".", List.of(Command.of(CommandType.WAIT)));
        expect("t", List.of(Command.of(CommandType.TALK)));
        expect("T", List.of(Command.of(CommandType.TALK)));
    }

    private static void checkItemCommands() {
        expect("u 3", List.of(Command.item(CommandType.USE, 2)));
        expect("u3", List.of(Command.item(CommandType.USE, 2)));
        expect("E 1", List.of(Command.item(CommandType.EQUIP, 0)));
        expect("x 12", List.of(Command.item(CommandType.DROP, 11)));
        expect("u   5", List.of(Command.item(CommandType.USE, 4)));
        expect("u　3", List.of(Command.item(CommandType.USE, 2)));
    }

    private static void checkUnknownInput() {
        expect("u", List.of());
        expect("e", List.of());
        expect("u 0", List.of());
        expect("u -1", List.of());
        expect("u abc", List.of());
        expect("u 99999999999", List.of());
        expect("q", List.of());
        expect("ddq", List.of());
        expect("w w", List.of());
        expect("gg", List.of());
        expect("saves", List.of());
        expect("sav", List.of());
        expect("save 1", List.of());
    }

    private static void checkCommandValidation() {
        expectThrows("방향 없는 MOVE", IllegalArgumentException.class, () -> Command.of(CommandType.MOVE));
        expectThrows("번호 없는 USE", IllegalArgumentException.class, () -> Command.of(CommandType.USE));
        expectThrows("MOVE 에 번호", IllegalArgumentException.class, () -> Command.item(CommandType.MOVE, 1));
        expectThrows("음수 번호", IllegalArgumentException.class, () -> Command.item(CommandType.USE, -1));
        expectThrows("PICK_UP 에 방향", IllegalArgumentException.class,
                () -> new Command(CommandType.PICK_UP, Direction.UP, Command.NO_ITEM));
        expectThrows("type 이 null", NullPointerException.class, () -> new Command(null, null, Command.NO_ITEM));
    }

    /** 명령 분류. ExplorationScreen·InventoryScreen 이 이 분류로 화면 명령과 아이템 명령을 가른다. */
    private static void checkCommandKinds() {
        Set<CommandType> items = EnumSet.of(CommandType.USE, CommandType.EQUIP, CommandType.DROP);
        Set<CommandType> screens = EnumSet.of(CommandType.INVENTORY, CommandType.HELP, CommandType.LOG,
                CommandType.TALK, CommandType.SAVE_QUIT);
        for (CommandType type : CommandType.values()) {
            check(type + " 아이템 명령 여부", type.isItemCommand() == items.contains(type));
            check(type + " 화면 명령 여부", type.isScreenCommand() == screens.contains(type));
        }
    }

    /** parse(input) 결과가 expected 인지. */
    private static void expect(String input, List<Command> expected) {
        CheckRunner.expect("parse(\"" + input + "\")", CommandParser.parse(input), expected);
    }
}
