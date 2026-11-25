package com.textrpg.game;

import java.util.List;
import java.util.Optional;

import com.textrpg.item.Inventory;
import com.textrpg.ui.ConsoleView;
import com.textrpg.ui.InputHandler;

/**
 * 가방 화면 (i). 가방 목록을 보여 주고 u/e/x 번호를 입력받는다.
 * 명령을 실행하지는 않는다. 고른 명령은 한 턴으로 실행되므로 ExplorationScreen 이 TurnSystem 에 넘긴다.
 */
final class InventoryScreen {

    private static final String PROMPT = "명령 (u/e/x 번호, Enter: 돌아가기): ";

    private final ConsoleView view;
    private final InputHandler input;

    InventoryScreen(ConsoleView view, InputHandler input) {
        this.view = view;
        this.input = input;
    }

    /**
     * 아이템 명령(u/e/x 번호)이 들어올 때까지 묻는다. 다른 입력이면 안내를 보여 주고 다시 묻는다.
     *
     * @return 입력한 아이템 명령. Enter 만 누르면 빈 값 (턴을 쓰지 않고 돌아간다)
     */
    Optional<Command> choose(Inventory inventory) {
        String error = "";
        while (true) {
            view.clearScreen();
            view.printInventory(inventory);
            if (!error.isEmpty()) {
                view.printLine(error);
            }
            String line = input.readLine(PROMPT);
            if (line.isEmpty()) {
                return Optional.empty();
            }
            List<Command> commands = CommandParser.parse(line);
            if (commands.size() == 1 && commands.get(0).type().isItemCommand()) {
                return Optional.of(commands.get(0));
            }
            error = "가방에서는 u 번호, e 번호, x 번호만 입력할 수 있습니다: " + line;
        }
    }
}
