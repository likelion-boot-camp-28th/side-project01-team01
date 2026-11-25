package com.textrpg;

import java.util.Objects;
import java.util.Optional;

/**
 * 확인용 클래스(…Check)가 같이 쓰는 집계. 통과·실패를 세고, 끝날 때 실패가 있으면 종료 코드 1 로 빌드를 실패시킨다.
 * JUnit 을 쓰지 않으므로(Team_Rules 2.11) 이 클래스가 assert 역할을 한다.
 *
 * <p>사용법: {@code import static com.textrpg.CheckRunner.expect;} 처럼 가져와 부르고, main 의 마지막에서
 * {@code CheckRunner.finish()} 를 부른다.
 *
 * <p>개수를 static 으로 센다. build.gradle 이 확인용 클래스마다 JVM 을 따로 띄우므로 클래스끼리 숫자가 섞이지 않는다.
 */
public final class CheckRunner {

    private static int passed;
    private static int failed;

    private CheckRunner() {
    }

    /** actual 과 expected 가 같으면(Objects.equals) 통과. */
    public static void expect(String name, Object actual, Object expected) {
        if (Objects.equals(actual, expected)) {
            pass();
        } else {
            fail(name + ": 기대 " + expected + " / 실제 " + actual);
        }
    }

    /** condition 이 참이면 통과. */
    public static void check(String name, boolean condition) {
        if (condition) {
            pass();
        } else {
            fail(name);
        }
    }

    /**
     * action 이 expected 종류의 예외를 던지면 통과. 다른 예외가 나거나 예외가 없으면 실패.
     *
     * @return 잡은 예외 (원인·메시지를 이어서 확인할 때 쓴다). 실패했으면 빈 값
     */
    public static <T extends RuntimeException> Optional<T> expectThrows(String name, Class<T> expected,
                                                                        Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            if (expected.isInstance(e)) {
                pass();
                return Optional.of(expected.cast(e));
            }
            fail(name + ": 기대 " + expected.getSimpleName() + " / 실제 " + e.getClass().getSimpleName());
            return Optional.empty();
        }
        fail(name + ": " + expected.getSimpleName() + " 이 나지 않음");
        return Optional.empty();
    }

    /** 직접 판정한 통과 하나. 반복문 안에서 여러 조건을 본 뒤 한 번만 셀 때 쓴다. */
    public static void pass() {
        passed++;
    }

    /** 직접 판정한 실패 하나. message 를 "[실패] " 뒤에 출력한다. */
    public static void fail(String message) {
        failed++;
        System.out.println("[실패] " + message);
    }

    /** 집계를 출력하고, 실패가 하나라도 있으면 종료 코드 1 로 끝낸다. main 의 마지막에 부른다. */
    public static void finish() {
        System.out.println("통과 " + passed + " / 실패 " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }
}
