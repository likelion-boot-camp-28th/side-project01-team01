package com.textrpg.ui;

/**
 * 입력 스트림이 끝났을 때(Ctrl+D, 파이프 입력 끝) InputHandler 가 던진다.
 * 빈 문자열로 돌려주면 다시 묻는 루프(readInt, 탐험 턴 루프)가 끝나지 않으므로 예외로 알린다.
 * GameManager.start() 가 잡아서 게임을 끝낸다.
 */
public class InputClosedException extends RuntimeException {

    public InputClosedException() {
        super("입력이 끝났습니다.");
    }
}
