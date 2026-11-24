package com.textrpg;

import java.nio.file.Path;

import com.textrpg.combat.CombatSystem;
import com.textrpg.combat.MonsterAI;
import com.textrpg.game.GameManager;
import com.textrpg.game.TurnSystem;
import com.textrpg.save.SaveManager;
import com.textrpg.ui.ConsoleView;
import com.textrpg.ui.InputHandler;

/**
 * 프로그램 진입점. 객체를 조립해 GameManager 에 넘기고, 실제 게임 흐름은 GameManager(A 파트)가 맡는다.
 *
 * <p>{@code --no-clear}: ANSI 화면 지우기 대신 빈 줄로 밀어낸다 (IntelliJ 실행 콘솔용).
 */
public final class Main {

    private static final Path SAVE_DIR = Path.of("saves");

    private Main() {
    }

    public static void main(String[] args) {
        ConsoleView view = new ConsoleView(!hasArg(args, "--no-clear"));
        TurnSystem turnSystem = new TurnSystem(new CombatSystem(), new MonsterAI());
        new GameManager(view, new InputHandler(), new SaveManager(SAVE_DIR), turnSystem).start();
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
