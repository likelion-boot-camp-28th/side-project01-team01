package com.textrpg;

import com.textrpg.ui.ConsoleView;

/**
 * 프로그램 진입점. 실제 게임 흐름은 GameManager(A 파트)가 맡는다.
 */
public final class Main {

    public static final String GAME_TITLE = "Into the Dark";

    private Main() {
    }

    public static void main(String[] args) {
        ConsoleView view = new ConsoleView(!hasArg(args, "--no-clear"));
        view.printTitle(GAME_TITLE);
        view.printLine("공통 뼈대가 준비되었습니다. GameManager 구현 후 게임이 시작됩니다.");
        // TODO(A): new GameManager(view, new InputHandler()).start();
    }

    private static boolean hasArg(String[] args, String flag) {
        for (String arg : args) {
            if (arg.equals(flag)) {
                return true;
            }
        }
        return false;
    }
}
