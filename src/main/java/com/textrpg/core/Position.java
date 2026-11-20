package com.textrpg.core;

/**
 * 맵 위의 좌표. x 는 열(0~59), y 는 행(0~19). 불변 객체라서 이동하면 새 Position 을 만든다.
 */
public record Position(int x, int y) {

    public Position move(Direction d) {
        return new Position(x + d.getDx(), y + d.getDy());
    }

    /** 체비쇼프 거리(대각선도 1칸). 시야·추적 판정용. */
    public int distanceTo(Position o) {
        return Math.max(Math.abs(x - o.x), Math.abs(y - o.y));
    }

    /** 상하좌우로 바로 붙어 있는지. 공격 가능 판정용 (대각선 공격은 하지 않음). */
    public boolean isAdjacent(Position o) {
        return Math.abs(x - o.x) + Math.abs(y - o.y) == 1;
    }
}
