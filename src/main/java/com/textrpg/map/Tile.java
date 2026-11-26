package com.textrpg.map;

/**
 * 맵의 한 칸. displayChar 는 화면 출력용, saveChar 는 세이브 파일 기록용 문자다.
 * 화면에서는 벽('#')과 복도(':')를 다른 문자로 그린다. 세이브 문자는 기존 형식(벽 'W', 복도 '#')을 유지한다.
 * ROCK 은 방 바깥의 빈 공간이다. 화면에서는 공백으로 보인다.
 */
public enum Tile {
    ROCK(' ', 'R', false),
    WALL('#', 'W', false),
    FLOOR('.', '.', true),
    CORRIDOR(':', '#', true),
    DOOR('+', '+', true),
    STAIRS_DOWN('>', '>', true);

    private final char displayChar;
    private final char saveChar;
    private final boolean walkable;

    Tile(char displayChar, char saveChar, boolean walkable) {
        this.displayChar = displayChar;
        this.saveChar = saveChar;
        this.walkable = walkable;
    }

    public char getDisplayChar() {
        return displayChar;
    }

    public char getSaveChar() {
        return saveChar;
    }

    public boolean isWalkable() {
        return walkable;
    }

    /** 세이브 파일의 문자를 타일로 복원한다. 모르는 문자면 예외. */
    public static Tile fromSaveChar(char c) {
        for (Tile t : values()) {
            if (t.saveChar == c) {
                return t;
            }
        }
        throw new IllegalArgumentException("알 수 없는 타일 문자: '" + c + "'");
    }
}
