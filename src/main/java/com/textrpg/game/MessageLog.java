package com.textrpg.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 게임 중 발생한 문장(전투, 획득, 이벤트)을 모아 두는 곳.
 * 모든 파트는 System.out 대신 log.add() 로 메시지를 남기고, 화면 출력은 ConsoleView 가 맡는다.
 */
public class MessageLog {

    private static final int MAX_SIZE = 100;

    private final List<String> messages = new ArrayList<>();

    public void add(String message) {
        messages.add(message);
        if (messages.size() > MAX_SIZE) {
            messages.remove(0);
        }
    }

    /** 가장 최근 n개를 오래된 순서로 돌려준다. */
    public List<String> recent(int n) {
        int from = Math.max(0, messages.size() - n);
        return List.copyOf(messages.subList(from, messages.size()));
    }

    public void clear() {
        messages.clear();
    }
}
