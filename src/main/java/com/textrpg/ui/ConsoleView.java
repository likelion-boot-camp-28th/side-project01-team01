package com.textrpg.ui;

import java.util.List;

/**
 * 모든 콘솔 출력의 단일 창구. 다른 클래스는 System.out 을 직접 쓰지 않는다.
 * 맵·상태줄 렌더링(render, printStatusBar 등)은 A 파트가 이 클래스에 추가한다.
 */
public class ConsoleView {

    private static final String DIVIDER = "-".repeat(60);

    private final boolean ansiClear;

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
    public static String bar(String label, int current, int max, int width) {
        int filled = max <= 0 ? 0 : (int) Math.round((double) current / max * width);
        filled = Math.max(0, Math.min(width, filled));
        return label + " " + current + "/" + max + " [" + "#".repeat(filled) + "-".repeat(width - filled) + "]";
    }
}
