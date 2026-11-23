package com.textrpg.item;

import java.util.ArrayList;
import java.util.List;

import com.textrpg.util.RandomUtil;

/**
 * 아이템은 모두 여기서 만든다. 세이브 파일의 id 로 다시 만들 때도 같은 메서드를 쓴다.
 * 아이템 수치(보너스, 회복량)는 이 클래스 한곳에서 관리한다.
 */
public final class ItemFactory {

    /** 무작위로 고를 수 있는 모든 아이템 id. 새 아이템을 만들면 여기에도 추가한다. */
    private static final String[] ALL_IDS = {"IRON_SWORD", "LEATHER_ARMOR", "HP_POTION_S"};

    private ItemFactory() {
    }

    /**
     * id 로 아이템을 만든다. 모르는 id 면 IllegalArgumentException.
     * 지금은 테스트용 3종만 있다. 나머지 아이템은 TODO(B).
     */
    public static Item createById(String id) {
        // 인자 순서: id, 이름, 설명, 최소 드랍 층, 최대 드랍 층, 수치...
        // 드랍 층수는 MonsterType 처럼 1~5층 기준 (5층은 보스층).
        return switch (id) {
            case "IRON_SWORD" -> new Weapon("IRON_SWORD", "철 검",
                    "평범한 철로 만든 검. 공격력 +3", 1, 3, 3);
            case "LEATHER_ARMOR" -> new Armor("LEATHER_ARMOR", "가죽 갑옷",
                    "질긴 가죽으로 만든 갑옷. 방어력 +2", 1, 3, 2);
            case "HP_POTION_S" -> new Potion("HP_POTION_S", "작은 치유 물약",
                    "마시면 HP 를 10 회복한다.", 1, 5, 10, 0);
            default -> throw new IllegalArgumentException("모르는 아이템 id: " + id);
        };
    }

    /**
     * 층에 맞는 아이템을 무작위로 만든다. D 의 DungeonGenerator.placeItems() 가 쓴다.
     * 그 층에서 나올 수 있는 아이템(canDropOn)만 후보로 모아 그중 하나를 고른다.
     */
    public static Item createRandom(int floorLevel) {
        List<Item> candidates = new ArrayList<>();
        for (String id : ALL_IDS) {
            Item item = createById(id);
            if (item.canDropOn(floorLevel)) {
                candidates.add(item);
            }
        }

        return RandomUtil.pick(candidates);
    }
}
