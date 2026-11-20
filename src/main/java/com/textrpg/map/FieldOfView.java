package com.textrpg.map;

import com.textrpg.entity.Position;

/**
 * (P1) 시야 계산. 방 안에서는 방 전체(벽 포함), 복도·문에서는 주변 CORRIDOR_RADIUS 칸이 보인다.
 */
public final class FieldOfView {

    /** 복도·문 위에서 보이는 거리. */
    public static final int CORRIDOR_RADIUS = 1;

    private FieldOfView() {
    }

    /**
     * 플레이어 위치 기준으로 층의 visible/seen 을 갱신한다. 플레이어가 이동할 때마다 호출한다.
     *
     * @param f         층
     * @param playerPos 플레이어 위치
     */
    public static void update(Floor f, Position playerPos) {
        // TODO(D): clearVisible → roomAt 이면 방 전체(벽 포함) setVisible, 아니면 주변 CORRIDOR_RADIUS 칸
        throw new UnsupportedOperationException("FieldOfView.update 는 아직 구현되지 않았습니다.");
    }
}
