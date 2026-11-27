package com.textrpg.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 게임 중 발생한 문장(전투, 획득, 이벤트)을 모아 두는 곳.
 * 모든 파트는 System.out 대신 log.add() 로 메시지를 남기고, 화면 출력은 ConsoleView 가 맡는다.
 *
 * <p>메시지마다 남긴 시점의 턴 수를 함께 기록한다. 턴 수는 GameContext 가 setTurn() 으로 맞춰 준다.
 * 탐험 화면은 아직 보여 주지 않은 메시지(unread)만 그리고, 지난 메시지는 로그 화면에서 본다.
 */
public class MessageLog {

    private static final int MAX_SIZE = 100;

    private final List<Entry> entries = new ArrayList<>();
    /** 지금까지 지난 턴 수 (GameContext.getTurn() 과 같은 값). */
    private int turn;
    /** 마지막 markRead() 이후에 들어온 메시지 수. MAX_SIZE 를 넘지 않는다. */
    private int unreadCount;

    /**
     * 메시지 한 줄.
     *
     * @param turn 메시지를 남길 때까지 지난 턴 수. 이 턴 다음에 벌어진 일이다
     * @param text 메시지 내용
     */
    public record Entry(int turn, String text) {
    }

    public void add(String message) {
        entries.add(new Entry(turn, message));
        if (entries.size() > MAX_SIZE) {
            entries.remove(0);
        }
        unreadCount = Math.min(unreadCount + 1, entries.size());
    }

    /** 앞으로 남길 메시지에 붙일 턴 수. GameContext 가 턴이 바뀔 때 호출한다. */
    public void setTurn(int turn) {
        this.turn = turn;
    }

    /** 가장 최근 n개의 문장을 오래된 순서로 돌려준다. */
    public List<String> recent(int n) {
        return recentEntries(n).stream().map(Entry::text).toList();
    }

    /** 가장 최근 n개를 오래된 순서로 돌려준다. 로그 화면에서 쓴다. */
    public List<Entry> recentEntries(int n) {
        int from = Math.max(0, entries.size() - n);
        return List.copyOf(entries.subList(from, entries.size()));
    }

    /** 마지막 markRead() 이후에 들어온 메시지를 오래된 순서로 돌려준다. 탐험 화면에서 쓴다. */
    public List<Entry> unread() {
        return recentEntries(unreadCount);
    }

    /** 지금까지의 메시지를 읽은 것으로 표시한다. 플레이어가 새 행동을 시작할 때 ExplorationScreen 이 호출한다. */
    public void markRead() {
        unreadCount = 0;
    }

    public void clear() {
        entries.clear();
        unreadCount = 0;
    }
}
