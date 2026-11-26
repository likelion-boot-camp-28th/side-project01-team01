package com.textrpg.game;

import com.textrpg.map.DungeonGenerator;

/**
 * 개발·시연용 옵션. Main 이 명령줄 인자로 만들어 GameManager 에 넘긴다.
 *
 * @param minShards  슬롯을 고를 때 파편이 이보다 적으면 이 값으로 맞춘다. 0 이면 끔 ({@code --debug-shards})
 * @param startFloor 새 탐험을 시작할 층. 1 이면 보통 게임과 같음 ({@code --start-floor=N})
 */
public record DebugOptions(int minShards, int startFloor) {

    /** 디버그 옵션 없음. 보통 실행. */
    public static final DebugOptions NONE = new DebugOptions(0, 1);
    /** {@code --debug-shards} 로 맞추는 파편 수. 영구 강화를 모두 사 볼 수 있는 정도. */
    public static final int DEBUG_SHARDS = 1000;

    /**
     * @throws IllegalArgumentException 파편이 음수이거나 시작 층이 1 ~ 최하층을 벗어날 때
     */
    public DebugOptions {
        if (minShards < 0) {
            throw new IllegalArgumentException("파편은 0 이상입니다: " + minShards);
        }
        if (startFloor < 1 || startFloor > DungeonGenerator.MAX_FLOOR) {
            throw new IllegalArgumentException("시작 층은 1 ~ " + DungeonGenerator.MAX_FLOOR + " 입니다: " + startFloor);
        }
    }

    /**
     * 명령줄 인자에서 디버그 옵션을 읽는다. 모르는 인자는 무시한다 (--no-clear 같은 다른 옵션).
     *
     * @throws IllegalArgumentException --start-floor 값이 숫자가 아니거나 범위를 벗어날 때
     */
    public static DebugOptions parse(String[] args) {
        int shards = 0;
        int floor = 1;
        for (String arg : args) {
            if (arg.equals("--debug-shards")) {
                shards = DEBUG_SHARDS;
            } else if (arg.startsWith("--start-floor=")) {
                String value = arg.substring("--start-floor=".length());
                try {
                    floor = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("--start-floor 값이 숫자가 아닙니다: " + value, e);
                }
            }
        }
        return new DebugOptions(shards, floor);
    }

    /** 보통 실행과 다른 옵션이 하나라도 켜져 있으면 true. 메뉴에 [디버그] 표시를 붙인다. */
    public boolean isEnabled() {
        return !equals(NONE);
    }
}
