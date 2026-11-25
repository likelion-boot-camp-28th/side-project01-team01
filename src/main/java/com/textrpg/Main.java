package com.textrpg;

import java.nio.file.Path;

import com.textrpg.combat.CombatSystem;
import com.textrpg.combat.MonsterAI;
import com.textrpg.game.DebugOptions;
import com.textrpg.game.GameManager;
import com.textrpg.game.TurnSystem;
import com.textrpg.save.SaveManager;
import com.textrpg.ui.ConsoleView;
import com.textrpg.ui.InputHandler;

/**
 * 프로그램 진입점. 객체를 조립해 GameManager 에 넘기고, 실제 게임 흐름은 GameManager(A 파트)가 맡는다.
 *
 * <p>실행 옵션
 * <ul>
 *   <li>{@code --no-clear}: ANSI 화면 지우기 대신 빈 줄로 밀어낸다 (IntelliJ 실행 콘솔용)</li>
 *   <li>{@code --no-color}: 글자 색(ANSI)을 쓰지 않는다. 환경 변수 NO_COLOR 가 있어도 같다 (no-color.org 관례)</li>
 *   <li>{@code --debug-shards}: 슬롯을 고를 때 파편을 1000 까지 채운다 (시연·테스트용)</li>
 *   <li>{@code --start-floor=N}: 새 탐험을 지하 N층(1~5)에서 시작한다 (시연·테스트용)</li>
 * </ul>
 */
public final class Main {

    private static final Path SAVE_DIR = Path.of("saves");

    private Main() {
    }

    public static void main(String[] args) {
        // 옵션 오류도 ConsoleView 로 출력하도록 화면을 먼저 만든다 (Team_Rules 1.2)
        boolean color = !hasArg(args, "--no-color") && System.getenv("NO_COLOR") == null;
        ConsoleView view = new ConsoleView(!hasArg(args, "--no-clear"), color);
        DebugOptions debug;
        try {
            debug = DebugOptions.parse(args);
        } catch (IllegalArgumentException e) {
            view.printLine("실행 옵션 오류: " + e.getMessage());
            return;
        }
        TurnSystem turnSystem = new TurnSystem(new CombatSystem(), new MonsterAI());
        new GameManager(view, new InputHandler(), new SaveManager(SAVE_DIR), turnSystem, debug).start();
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
