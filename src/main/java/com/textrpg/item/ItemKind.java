package com.textrpg.item;

import java.util.function.Function;

/**
 * 아이템 종류별 이름·설명·드랍 층과 수치를 모아 둔 enum.
 * 아이템을 추가하거나 밸런스를 조정할 때는 이 enum 만 고친다 (MonsterType 과 같은 방식).
 *
 * <p>상수 이름이 세이브 파일에 쓰는 아이템 id 다. 이름을 바꾸면 예전 세이브를 읽지 못하므로 바꾸지 않는다.
 * 설명에는 수치를 쓰지 않는다. 수치는 Item.getDescription() 이 실제 값으로 붙인다.
 * 드랍 층수는 MonsterType 처럼 1~5층 기준 (5층은 보스층).
 */
public enum ItemKind {
    //            이름              설명                          드랍 층   종류와 수치
    IRON_SWORD("철 검", "평범한 철로 만든 검.", 1, 3, kind -> new Weapon(kind, 3)),
    LEATHER_ARMOR("가죽 갑옷", "질긴 가죽으로 만든 갑옷.", 1, 3, kind -> new Armor(kind, 2)),
    HP_POTION_S("작은 치유 물약", "마시면 상처가 아문다.", 1, 5, kind -> new Potion(kind, 10, 0)),
    MP_POTION_S("작은 마나 물약", "마시면 정신이 맑아진다.", 1, 5, kind -> new Potion(kind, 0, 5));

    private final String displayName;
    private final String description;
    private final int minFloor;
    private final int maxFloor;
    /** 이 종류의 아이템을 새로 만드는 방법. 아이템마다 따로 만들어야 가방에서 서로 다른 물건으로 다룬다. */
    private final Function<ItemKind, Item> creator;

    ItemKind(String displayName, String description, int minFloor, int maxFloor, Function<ItemKind, Item> creator) {
        if (minFloor < 1 || maxFloor < minFloor) {
            throw new IllegalArgumentException("드랍 층수가 잘못되었습니다: " + name() + " (" + minFloor + "~" + maxFloor + ")");
        }
        this.displayName = displayName;
        this.description = description;
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
        this.creator = creator;
    }

    /** 이 종류의 아이템을 하나 새로 만든다. ItemFactory 가 부른다. */
    Item create() {
        return creator.apply(this);
    }

    public String getDisplayName() {
        return displayName;
    }

    /** 수치를 뺀 설명. 화면에는 Item.getDescription() 을 쓴다. */
    public String getDescription() {
        return description;
    }

    public int getMinFloor() {
        return minFloor;
    }

    public int getMaxFloor() {
        return maxFloor;
    }

    /** floorLevel 층에서 나올 수 있는 종류면 true. */
    public boolean canDropOn(int floorLevel) {
        return minFloor <= floorLevel && floorLevel <= maxFloor;
    }
}
