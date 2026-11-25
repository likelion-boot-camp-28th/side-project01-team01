package com.textrpg.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.textrpg.core.Direction;

/**
 * 플레이어가 입력한 한 줄을 Command 목록으로 바꾼다. 입력을 읽거나 실행하지는 않는다.
 *
 * <p>입력 규칙 (11/20 회의 후 확정):
 * <ul>
 *   <li>{@code w/a/s/d}: 이동. {@code ddd} 처럼 이어 쓰면 순서대로 여러 번 (P1 연속 이동)</li>
 *   <li>{@code g} 줍기, {@code i} 가방, {@code l} 지난 로그, {@code >} 내려가기, {@code save} 저장 후 종료, {@code ?} 도움말</li>
 *   <li>{@code t} 옆에 있는 상인과 대화 (상점)</li>
 *   <li>(제안) {@code u 3} 사용, {@code e 2} 장착, {@code x 1} 버리기, {@code .} 대기. 번호는 화면 기준 1부터</li>
 * </ul>
 */
public final class CommandParser {

    /** 한 글자 키로 두면 실수로 누르기 쉬워서 단어로 입력받는다. */
    private static final String SAVE_QUIT_WORD = "save";

    /** 화면에 보이는 가방 번호는 1부터 시작한다. */
    private static final int FIRST_ITEM_NUMBER = 1;

    private CommandParser() {
    }

    /**
     * 입력 한 줄을 명령 목록으로 바꾼다.
     * 대소문자를 구분하지 않는다 (Caps Lock 이 켜져 있어도 이동 가능).
     * 한 글자라도 해석할 수 없으면 줄 전체를 버린다. 예: {@code ddq} 는 빈 리스트.
     *
     * @param line InputHandler.readLine() 이 돌려준 문자열 (앞뒤 공백 제거됨)
     * @return 해석한 명령들. 빈 입력이나 알 수 없는 입력이면 빈 리스트 (null 을 돌려주지 않는다)
     */
    public static List<Command> parse(String line) {
        if (line == null) {
            return List.of();
        }
        String input = line.trim();
        if (input.isEmpty()) {
            return List.of();
        }
        if (input.equalsIgnoreCase(SAVE_QUIT_WORD)) {
            return List.of(Command.of(CommandType.SAVE_QUIT));
        }
        Optional<Command> command = parseSingleKey(input).or(() -> parseItemCommand(input));
        return command.map(List::of).orElseGet(() -> parseMoves(input));
    }

    /** g, i, l, t, >, ?, . 처럼 한 글자로 끝나는 명령. */
    private static Optional<Command> parseSingleKey(String input) {
        if (input.length() != 1) {
            return Optional.empty();
        }
        CommandType type = switch (Character.toLowerCase(input.charAt(0))) {
            case 'g' -> CommandType.PICK_UP;
            case 'i' -> CommandType.INVENTORY;
            case 'l' -> CommandType.LOG;
            case 't' -> CommandType.TALK;
            case '>' -> CommandType.DESCEND;
            case '?' -> CommandType.HELP;
            case '.' -> CommandType.WAIT;
            default -> null;
        };
        return Optional.ofNullable(type).map(Command::of);
    }

    /** "u 3", "u3" 처럼 키 한 글자 + 가방 번호. 번호는 1부터 받아 0부터로 바꾼다. */
    private static Optional<Command> parseItemCommand(String input) {
        if (input.length() < 2) {
            return Optional.empty();
        }
        CommandType type = switch (Character.toLowerCase(input.charAt(0))) {
            case 'u' -> CommandType.USE;
            case 'e' -> CommandType.EQUIP;
            case 'x' -> CommandType.DROP;
            default -> null;
        };
        if (type == null) {
            return Optional.empty();
        }
        int number;
        try {
            number = Integer.parseInt(input.substring(1).trim());
        } catch (NumberFormatException e) {
            // 숫자가 아니거나 int 범위를 넘으면 알 수 없는 입력으로 본다.
            return Optional.empty();
        }
        if (number < FIRST_ITEM_NUMBER) {
            return Optional.empty();
        }
        return Optional.of(Command.item(type, number - FIRST_ITEM_NUMBER));
    }

    /** w/a/s/d 로만 이루어진 입력. 다른 글자가 하나라도 섞이면 빈 리스트. */
    private static List<Command> parseMoves(String input) {
        List<Command> moves = new ArrayList<>();
        for (char key : input.toCharArray()) {
            Direction direction = Direction.fromKey(key);
            if (direction == null) {
                return List.of();
            }
            moves.add(Command.move(direction));
        }
        return List.copyOf(moves);
    }
}
