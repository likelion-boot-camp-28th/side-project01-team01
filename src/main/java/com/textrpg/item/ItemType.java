package com.textrpg.item;

/**
 * 아이템 분류와 맵 표시 기호.
 */
public enum ItemType {
    WEAPON(')'),
    ARMOR('['),
    POTION('!'),
    SCROLL('?'),
    FOOD('%'),
    GOLD('$');

    private final char symbol;

    ItemType(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }
}
