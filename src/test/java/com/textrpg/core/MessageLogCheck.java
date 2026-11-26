package com.textrpg.core;

import java.util.List;
import java.util.Objects;

/**
 * MessageLog 확인용: 턴 기록, 읽지 않은 메시지(unread), 최대 개수.
 * JUnit 대신 main 으로 실행한다 (Team_Rules 2.11). 실패가 하나라도 있으면 종료 코드 1.
 */
public final class MessageLogCheck {

    private static int passed;
    private static int failed;

    private MessageLogCheck() {
    }

    public static void main(String[] args) {
        checkTurn();
        checkUnread();
        checkMaxSize();

        System.out.println("통과 " + passed + " / 실패 " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void checkTurn() {
        MessageLog log = new MessageLog();
        log.add("시작");
        log.setTurn(3);
        log.add("공격");
        expect("턴 기록", log.recentEntries(2),
                List.of(new MessageLog.Entry(0, "시작"), new MessageLog.Entry(3, "공격")));
        expect("recent 는 문장만", log.recent(2), List.of("시작", "공격"));
    }

    private static void checkUnread() {
        MessageLog log = new MessageLog();
        log.add("a");
        log.add("b");
        expect("처음엔 모두 unread", log.unread().size(), 2);

        log.markRead();
        expect("markRead 후 비어 있음", log.unread(), List.of());

        log.add("c");
        expect("새 메시지만", log.unread(), List.of(new MessageLog.Entry(0, "c")));
        expect("지난 메시지는 recent 로", log.recent(3), List.of("a", "b", "c"));

        log.clear();
        expect("clear 후 unread 없음", log.unread(), List.of());
    }

    private static void checkMaxSize() {
        MessageLog log = new MessageLog();
        for (int i = 0; i < 150; i++) {
            log.add("m" + i);
        }
        expect("최대 100개 보관", log.recentEntries(1000).size(), 100);
        expect("unread 도 최대 100개", log.unread().size(), 100);
        expect("오래된 것부터 지움", log.recentEntries(1000).get(0).text(), "m50");
    }

    private static void expect(String name, Object actual, Object expected) {
        if (Objects.equals(actual, expected)) {
            passed++;
        } else {
            failed++;
            System.out.println("[실패] " + name + ": 기대 " + expected + " / 실제 " + actual);
        }
    }
}
