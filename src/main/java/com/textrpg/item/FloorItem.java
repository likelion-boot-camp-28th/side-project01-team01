package com.textrpg.item;

import com.textrpg.core.Position;

/**
 * 바닥에 놓인 아이템 또는 골드 더미.
 * 아이템이면 item 이 있고 goldAmount 는 0, 골드면 item 이 null 이고 goldAmount 가 1 이상.
 * 직접 new 하지 말고 FloorItem.of(...) / FloorItem.gold(...) 로 만든다.
 */
public record FloorItem(Item item, Position pos, int goldAmount) {

    public FloorItem {
        if (pos == null) {
            throw new IllegalArgumentException("pos 는 null 일 수 없습니다.");
        }
        boolean isItem = item != null && goldAmount == 0;
        boolean isGoldPile = item == null && goldAmount > 0;
        if (!isItem && !isGoldPile) {
            throw new IllegalArgumentException("아이템이거나 골드(1 이상) 중 하나여야 합니다.");
        }
    }

    public static FloorItem of(Item item, Position pos) {
        return new FloorItem(item, pos, 0);
    }

    public static FloorItem gold(int amount, Position pos) {
        return new FloorItem(null, pos, amount);
    }

    public boolean isGold() {
        return item == null;
    }

    /** 맵에 그릴 기호. 골드면 '$'. */
    public char symbol() {
        return isGold() ? ItemType.GOLD.getSymbol() : item.getSymbol();
    }
}
