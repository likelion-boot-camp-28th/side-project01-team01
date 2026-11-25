package com.textrpg.game;

import com.textrpg.CheckRunner;

import static com.textrpg.CheckRunner.expect;
import static com.textrpg.CheckRunner.expectThrows;

/**
 * DebugOptions 확인용: 명령줄 인자 읽기와 잘못된 값 거절.
 * JUnit 대신 main 으로 실행한다 (Team_Rules 2.11). 실패가 하나라도 있으면 종료 코드 1.
 */
public final class DebugOptionsCheck {

    private DebugOptionsCheck() {
    }

    public static void main(String[] args) {
        checkParse();
        checkInvalid();

        CheckRunner.finish();
    }

    private static void checkParse() {
        DebugOptions none = DebugOptions.parse(new String[0]);
        expect("인자 없음은 NONE", none, DebugOptions.NONE);
        expect("인자 없음은 디버그 아님", none.isEnabled(), false);
        expect("--no-clear 만 있으면 디버그 아님", DebugOptions.parse(new String[] {"--no-clear"}).isEnabled(), false);

        DebugOptions shards = DebugOptions.parse(new String[] {"--debug-shards"});
        expect("--debug-shards 파편", shards.minShards(), DebugOptions.DEBUG_SHARDS);
        expect("--debug-shards 는 1층 시작", shards.startFloor(), 1);
        expect("--debug-shards 는 디버그", shards.isEnabled(), true);

        DebugOptions both = DebugOptions.parse(new String[] {"--no-clear", "--start-floor=5", "--debug-shards"});
        expect("두 옵션 함께", both, new DebugOptions(DebugOptions.DEBUG_SHARDS, 5));
        expect("--start-floor 만", DebugOptions.parse(new String[] {"--start-floor=3"}), new DebugOptions(0, 3));
    }

    private static void checkInvalid() {
        expectRejected("숫자가 아닌 층", new String[] {"--start-floor=abc"});
        expectRejected("빈 층", new String[] {"--start-floor="});
        expectRejected("0층", new String[] {"--start-floor=0"});
        expectRejected("최하층보다 깊음", new String[] {"--start-floor=6"});
    }

    /** 잘못된 인자면 parse 가 IllegalArgumentException 을 던져야 한다. */
    private static void expectRejected(String name, String[] args) {
        expectThrows(name, IllegalArgumentException.class, () -> DebugOptions.parse(args));
    }
}
