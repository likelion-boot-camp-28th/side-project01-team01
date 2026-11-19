package com.textrpg.map;

/**
 * 이동 방향. 좌표계는 x 가 오른쪽(열), y 가 아래쪽(행)으로 증가한다.
 */
public enum Direction {
    UP('w', 0, -1),
    DOWN('s', 0, 1),
    LEFT('a', -1, 0),
    RIGHT('d', 1, 0);

    private final char key;
    private final int dx;
    private final int dy;

    Direction(char key, int dx, int dy) {
        this.key = key;
        this.dx = dx;
        this.dy = dy;
    }

    public char getKey() {
        return key;
    }

    public int getDx() {
        return dx;
    }

    public int getDy() {
        return dy;
    }

    /** w/a/s/d (대소문자 무시) 를 방향으로 바꾼다. 해당 없으면 null. */
    public static Direction fromKey(char c) {
        char lower = Character.toLowerCase(c);
        for (Direction d : values()) {
            if (d.key == lower) {
                return d;
            }
        }
        return null;
    }
}
