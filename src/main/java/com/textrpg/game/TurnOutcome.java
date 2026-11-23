package com.textrpg.game;

/**
 * TurnSystem 이 플레이어 명령을 실행한 결과. GameManager 는 이 값을 보고 다음 단계를 정한다.
 * 가방 보기, 도움말, 저장 후 종료처럼 화면이나 게임 상태를 바꾸는 명령은 TurnSystem 이 아니라 GameManager 가 처리한다.
 */
public enum TurnOutcome {
    /** 턴을 쓰지 않음 (벽에 막힘, 빈 칸에서 줍기 등). 몬스터가 행동하지 않는다. */
    NO_TURN,
    /** 턴을 씀. 이어서 모든 몬스터가 행동한다. */
    TURN_USED,
    /** 계단을 내려감. GameManager 가 다음 층을 만든다. */
    DESCENDED,
    /** 보스를 처치함. GameManager 가 클리어 처리를 한다. */
    VICTORY,
    /** 몬스터 행동 중 플레이어가 쓰러짐. 원인은 TurnSystem.getDeathCause(). GameManager 가 사망 처리를 한다. */
    DIED
}
