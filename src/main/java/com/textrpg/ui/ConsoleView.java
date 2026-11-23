package com.textrpg.ui;

import java.util.List;

import com.textrpg.context.GameContext;
import com.textrpg.entity.Player;
import com.textrpg.item.Inventory;
import com.textrpg.meta.Profile;
import com.textrpg.meta.RunResult;

/**
 * 모든 콘솔 출력의 단일 창구. 다른 클래스는 System.out 을 직접 쓰지 않는다.
 * 맵·상태줄 렌더링(render, printStatusBar 등)은 A 파트가 이 클래스에 추가한다.
 */
public class ConsoleView {

    private static final String DIVIDER = "-".repeat(60);

    private final boolean ansiClear;
    private final MapRenderer mapRenderer = new MapRenderer();

    /** @param ansiClear true 면 ANSI 코드로 화면을 지우고, false 면 빈 줄로 밀어낸다(IntelliJ 콘솔용). */
    public ConsoleView(boolean ansiClear) {
        this.ansiClear = ansiClear;
    }

    public void clearScreen() {
        if (ansiClear) {
            System.out.print("\033[H\033[2J");
            System.out.flush();
        } else {
            System.out.print("\n".repeat(30));
        }
    }

    public void printLine(String message) {
        System.out.println(message);
    }

    public void printDivider() {
        System.out.println(DIVIDER);
    }

    public void printTitle(String title) {
        printDivider();
        System.out.println("  " + title);
        printDivider();
    }

    /** 번호 메뉴 출력. 예: "1) 새 탐험" */
    public void printMenu(String title, List<String> options) {
        printTitle(title);
        for (int i = 0; i < options.size(); i++) {
            System.out.println((i + 1) + ") " + options.get(i));
        }
    }

    /** "HP 32/40 [########--]" 형태의 막대. */
    /**
     * 탐험 화면 한 장을 그린다: 화면 지우기 → 맵 → 상태 바 → 최근 메시지.
     *
     * @param ctx 이번 탐험의 컨텍스트 (층, 플레이어, 턴, 메시지 로그)
     */
    public void render(GameContext ctx) {
        // TODO(A): clearScreen → mapRenderer.render(floor, player) 출력 → printStatusBar → log.recent(n) 출력
        throw new UnsupportedOperationException("ConsoleView.render 는 아직 구현되지 않았습니다.");
    }

    /** 이름·직업·레벨, HP/MP 바, 공격·방어, 골드, 층, 턴을 한두 줄로 보여 준다. */
    public void printStatusBar(Player player, int floorLevel, int turn) {
        // TODO(A): bar("HP", hp, maxHp, 10) 등을 한 줄로
        throw new UnsupportedOperationException("ConsoleView.printStatusBar 는 아직 구현되지 않았습니다.");
    }

    /** 가방 목록. 번호는 1부터 보여 준다 (입력 u 1 → index 0). 빈 가방이면 안내 문장. */
    public void printInventory(Inventory inventory) {
        // TODO(A)
        throw new UnsupportedOperationException("ConsoleView.printInventory 는 아직 구현되지 않았습니다.");
    }

    /** 명령어 도움말. CommandParser 의 입력 규칙과 같은 내용. */
    public void printHelp() {
        // TODO(A)
        throw new UnsupportedOperationException("ConsoleView.printHelp 는 아직 구현되지 않았습니다.");
    }

    /** 어둠의 제단: 보유 파편과 강화 목록(현재 단계 / 최대 단계 / 다음 비용). 번호는 1부터. */
    public void printAltar(Profile profile) {
        // TODO(A): UpgradeType.values() 순서대로, 최대 단계면 "완료"
        throw new UnsupportedOperationException("ConsoleView.printAltar 는 아직 구현되지 않았습니다.");
    }

    /** 프로필 누적 기록: 총 탐험 수, 최고 층, 클리어 수, 누적 처치, 보유 파편. */
    public void printRecords(Profile profile) {
        // TODO(A)
        throw new UnsupportedOperationException("ConsoleView.printRecords 는 아직 구현되지 않았습니다.");
    }

    /**
     * 사망·클리어 결과 화면.
     *
     * @param result         이번 탐험 결과
     * @param shardBreakdown ShardCalculator.breakdown() 결과 (항목별 파편 내역)
     */
    public void printRunResult(RunResult result, List<String> shardBreakdown) {
        // TODO(A): 사망/클리어 제목, 원인, 도달 층, 처치, 레벨, 골드, 파편 내역
        throw new UnsupportedOperationException("ConsoleView.printRunResult 는 아직 구현되지 않았습니다.");
    }

    public static String bar(String label, int current, int max, int width) {
        int filled = max <= 0 ? 0 : (int) Math.round((double) current / max * width);
        filled = Math.max(0, Math.min(width, filled));
        return label + " " + current + "/" + max + " [" + "#".repeat(filled) + "-".repeat(width - filled) + "]";
    }
}
