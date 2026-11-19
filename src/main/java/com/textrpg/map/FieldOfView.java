package com.textrpg.map;

import com.textrpg.entity.Position;

/**
 * 시야 계산 (P1). 방 안에서는 방 전체(벽 포함), 복도·문에서는 주변 CORRIDOR_RADIUS 칸이 보인다.
 */
public final class FieldOfView {

    public static final int CORRIDOR_RADIUS = 1;

    private FieldOfView() {
    }

    /**
     * 플레이어 위치 기준으로 floor 의 visible/seen 을 갱신한다. 플레이어가 이동할 때마다 호출한다.
     */
    public static void update(Floor f, Position playerPos) {
        // TODO(D): P1 시야
        throw new UnsupportedOperationException("TODO(D): FieldOfView.update");
    }
}
