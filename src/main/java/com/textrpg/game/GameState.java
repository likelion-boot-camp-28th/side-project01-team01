package com.textrpg.game;

/**
 * 지금 어떤 화면에 있는지. GameManager 가 이 값에 따라 다음에 보여 줄 화면을 고른다.
 */
public enum GameState {
    /** 타이틀, 슬롯 선택. */
    MAIN_MENU,
    /** 슬롯 메뉴: 새 탐험 / 이어하기 / 어둠의 제단 / 기록 / 뒤로. */
    SLOT_MENU,
    /** 어둠의 제단: 파편으로 영구 강화 구매. */
    ALTAR,
    /** 프로필 기록 화면 (총 탐험 수, 최고 층, 클리어 수, 누적 처치). */
    RECORDS,
    /** 탐험 중 (턴 루프). */
    PLAYING,
    /** 사망 결과 화면. */
    DEAD,
    /** 클리어 결과 화면. */
    VICTORY,
    /** 프로그램 종료. */
    QUIT
}
