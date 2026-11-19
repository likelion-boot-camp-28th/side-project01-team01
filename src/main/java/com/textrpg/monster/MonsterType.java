package com.textrpg.monster;

public enum MonsterType {

    SLIME("슬라임", 'S', 1, 2, 8, 3, 0, 3, 3, 0),
    BAT("박쥐", 'B', 1, 2, 6, 3, 1, 3, 3, 1),
    GOBLIN("고블린", 'G', 1, 3, 12, 5, 1, 6, 6, 1),
    WOLF("늑대", 'W', 2, 3, 14, 6, 2, 8, 8, 2),
    SKELETON("해골 병사", 'K', 3, 4, 20, 8, 3, 14, 14, 1),
    ORC("오크", 'O', 3, 5, 26, 9, 4, 18, 18, 1),
    SPIDER("독거미", 'V', 4, 5, 18, 8, 2, 16, 16, 1),
    SHADOW_KNIGHT("그림자 기사", 'D', 4, 5, 32, 11, 6, 25, 25, 1),
    ABYSS_WARDEN("어둠의 파수꾼", '&', 5, 5, 120, 14, 7, 0, 0, 1);

    final String displayName;
    final char symbol;
    final int minFloor;
    final int maxFloor;
    final int hp;
    final int attack;
    final int defense;
    final int exp;
    final int gold;
    final int speed;

    MonsterType(String displayName, char symbol, int minFloor, int maxFloor, int hp, int attack, int defense, int exp, int gold, int speed) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
        this.hp = hp;
        this.attack = attack;
        this.defense = defense;
        this.exp = exp;
        this.gold = gold;
        this.speed = speed;
    }
}
