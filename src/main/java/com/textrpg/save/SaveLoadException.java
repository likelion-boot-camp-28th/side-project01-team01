package com.textrpg.save;

/**
 * 세이브 파일을 읽거나 쓰지 못했을 때 던진다 (파일 없음, 손상, 버전 불일치, 입출력 오류).
 * GameManager 는 이 예외를 잡아 안내 메시지를 보여 주고 메인 메뉴로 돌아간다. 게임을 종료하지 않는다.
 */
public class SaveLoadException extends RuntimeException {

    /** @param message 사용자에게 보여 줄 수 있는 설명. 섹션과 key 이름을 넣는다. 예: "[PLAYER] level 값이 숫자가 아닙니다: abc" */
    public SaveLoadException(String message) {
        super(message);
    }

    /**
     * @param message 사용자에게 보여 줄 수 있는 설명
     * @param cause   원인 예외 (IOException, NumberFormatException 등)
     */
    public SaveLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
