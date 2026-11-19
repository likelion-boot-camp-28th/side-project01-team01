package com.textrpg.map;

/**
 * 맵의 한 칸. displayChar 는 화면 출력용, saveChar 는 세이브 파일 기록용 문자다.
 * (벽과 복도가 화면에서는 둘 다 '#' 이라서 저장용 문자를 따로 둔다.)
 */
public enum Tile {
    WALL('#', 'W', false),
    FLOOR('.', '.', true),
    CORRIDOR('#', '#', true),
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
