package com.textrpg.ui;

import java.util.regex.Pattern;

/**
 * 글자에 색(Style)을 입힌다. ANSI 는 색 코드로 감싸고, PLAIN 은 글자를 그대로 둔다 (--no-color, 색을 못 쓰는 콘솔).
 * 색 코드는 화면에서 칸을 차지하지 않으므로, 너비를 계산할 때는 strip() 으로 코드를 지운 길이를 쓴다.
 */
enum Painter {
    PLAIN,
    ANSI;

    private static final String ESC = "\033[";
    private static final String RESET = ESC + "0m";
    private static final Pattern CODE = Pattern.compile("\033\\[[0-9;]*m");

    /** text 에 style 색을 입힌다. PLAIN 이거나 빈 문자열이면 그대로 돌려준다. */
    String paint(Style style, String text) {
        if (this == PLAIN || text.isEmpty()) {
            return text;
        }
        return ESC + style.code() + "m" + text + RESET;
    }

    /** 색 코드를 모두 지운 글자. 너비 계산과 확인용. */
    static String strip(String text) {
        return CODE.matcher(text).replaceAll("");
    }
}
