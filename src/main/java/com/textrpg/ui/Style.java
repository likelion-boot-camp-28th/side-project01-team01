package com.textrpg.ui;

/**
 * 화면 요소별 글자 색. 색을 바꿀 때는 이 enum 만 고친다.
 * 값은 ANSI SGR 코드다 (예: "31" 빨강, "1;35" 굵은 자홍, "90" 회색).
 *
 * <p>흰색·검은색 글자는 쓰지 않는다. 터미널 배경이 밝든 어둡든 보이도록, 플레이어는 기본 글자색에 굵게만 준다.
 */
enum Style {
    /** 플레이어 {@code @}. */
    PLAYER("1"),
    /** 일반 몬스터 (영문자). */
    MONSTER("31"),
    /** 보스 {@code &}. */
    BOSS("1;35"),
    GOLD("33"),
    POTION("32"),
    /** 무기·방어구. */
    EQUIPMENT("36"),
    /** 두루마리·음식 등 그 밖의 아이템 (P1). */
    OTHER_ITEM("34"),
    /** 아래층 계단. 본 적만 있어도 같은 색으로 보여 찾기 쉽게 한다. */
    STAIRS("1;32"),
    DOOR("33"),
    /** 본 적은 있지만 지금은 안 보이는 칸. */
    REMEMBERED("90"),
    /** HP 막대: 절반 이상 / 4분의 1 이상 / 그 아래. */
    HP_HIGH("32"),
    HP_MID("33"),
    HP_LOW("31"),
    MP("94"),
    /** 화면 제목. */
    TITLE("1"),
    /** 클리어 결과 제목. */
    VICTORY("1;32"),
    /** 사망 결과 제목. */
    DEFEAT("1;31");

    private final String code;

    Style(String code) {
        this.code = code;
    }

    String code() {
        return code;
    }
}
