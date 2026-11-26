package com.textrpg.item;

import java.util.function.Function;

/**
 * 아이템 종류별 이름·설명·드랍 층과 수치를 모아 둔 enum.
 * 아이템을 추가하거나 밸런스를 조정할 때는 이 enum 만 고친다 (MonsterType 과 같은 방식).
 *
 * <p>상수 이름이 세이브 파일에 쓰는 아이템 id 다. 이름을 바꾸면 예전 세이브를 읽지 못하므로 바꾸지 않는다.
 * 설명에는 수치를 쓰지 않는다. 수치는 Item.getDescription() 이 실제 값으로 붙인다.
 * 드랍 층수는 MonsterType 처럼 1~5층 기준 (5층은 보스층). 장비는 2층부터 나오고,
 * 맨몸으로 2층에 들어가지 않도록 가죽 갑옷만 1층부터 나온다.
 * 장비는 깊은 층일수록 수치가 높고, 물약 수치는 최대 HP·MP 의 회복 비율(%)이다.
 * 상점가는 상점에서 살 때의 고정 가격(골드)이다. 팔 때는 Shop 이 이 값의 일부만 준다.
 */
public enum ItemKind {
    //               이름              설명                                드랍 층  상점가  종류와 수치
    // 무기: 공격력 보너스
    IRON_SWORD("철 검", "평범한 철로 만든 검.", 2, 3, 50, kind -> new Weapon(kind, 3)),
    STEEL_SWORD("강철 검", "잘 벼린 강철 검.", 3, 4, 90, kind -> new Weapon(kind, 5)),
    RUNE_BLADE("룬 대검", "푸른 룬이 새겨진 묵직한 대검.", 4, 5, 160, kind -> new Weapon(kind, 7)),
    // 방어구: 방어력 보너스
    LEATHER_ARMOR("가죽 갑옷", "질긴 가죽으로 만든 갑옷.", 1, 3, 40, kind -> new Armor(kind, 2)),
    CHAIN_MAIL("사슬 갑옷", "쇠고리를 엮어 만든 갑옷.", 3, 4, 70, kind -> new Armor(kind, 3)),
    STEEL_PLATE("강철 판금", "온몸을 덮는 강철 판금 갑옷.", 4, 5, 110, kind -> new Armor(kind, 4)),
    GUARDIAN_PLATE("수호자의 판금", "어둠의 파수꾼에 맞선 기사들이 입던 갑옷.", 5, 5, 200, kind -> new Armor(kind, 5)),
    // 물약: 최대 HP·MP 의 회복 비율(%). 마나 물약은 같은 크기의 치유 물약보다 조금 더 회복한다
    HP_POTION_S("작은 치유 물약", "마시면 상처가 아문다.", 1, 5, 20, kind -> new Potion(kind, 25, 0)),
    HP_POTION_L("큰 치유 물약", "마시면 깊은 상처까지 아문다.", 3, 5, 45, kind -> new Potion(kind, 50, 0)),
    MP_POTION_S("작은 마나 물약", "마시면 정신이 맑아진다.", 1, 5, 20, kind -> new Potion(kind, 0, 30)),
    MP_POTION_L("큰 마나 물약", "마시면 정신이 또렷하게 깨어난다.", 3, 5, 45, kind -> new Potion(kind, 0, 60));

    private final String displayName;
    private final String description;
    private final int minFloor;
    private final int maxFloor;
    private final int price;
    /** 이 종류의 아이템을 새로 만드는 방법. 아이템마다 따로 만들어야 가방에서 서로 다른 물건으로 다룬다. */
    private final Function<ItemKind, Item> creator;

    ItemKind(String displayName, String description, int minFloor, int maxFloor, int price,
             Function<ItemKind, Item> creator) {
        if (minFloor < 1 || maxFloor < minFloor) {
            throw new IllegalArgumentException("드랍 층수가 잘못되었습니다: " + name() + " (" + minFloor + "~" + maxFloor + ")");
        }
        if (price < 1) {
            throw new IllegalArgumentException("상점가는 1 이상입니다: " + name() + " (" + price + ")");
        }
        this.displayName = displayName;
        this.description = description;
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
        this.price = price;
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

    /** 상점에서 살 때의 고정 가격(골드). */
    public int getPrice() {
        return price;
    }

    /** floorLevel 층에서 나올 수 있는 종류면 true. */
    public boolean canDropOn(int floorLevel) {
        return minFloor <= floorLevel && floorLevel <= maxFloor;
    }
}
