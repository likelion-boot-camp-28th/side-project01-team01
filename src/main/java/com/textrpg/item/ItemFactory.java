package com.textrpg.item;

/**
 * 아이템은 모두 여기서 만든다. 세이브 파일의 id 로 다시 만들 때도 같은 메서드를 쓴다.
 * 아이템 수치(보너스, 회복량)는 이 클래스 한곳에서 관리한다.
 */
public final class ItemFactory {

    private ItemFactory() {
    }

    /**
     * id 로 아이템을 만든다. 모르는 id 면 IllegalArgumentException.
     * 지금은 테스트용 3종만 있다. 나머지 아이템은 TODO(B).
     */
    public static Item createById(String id) {
        return switch (id) {
            case "IRON_SWORD" -> new Weapon("IRON_SWORD", "철 검", 3);
            case "LEATHER_ARMOR" -> new Armor("LEATHER_ARMOR", "가죽 갑옷", 2);
            case "HP_POTION_S" -> new Potion("HP_POTION_S", "작은 치유 물약", 10, 0);
            default -> throw new IllegalArgumentException("모르는 아이템 id: " + id);
        };
    }

    /**
     * 층에 맞는 아이템을 무작위로 만든다. D 의 DungeonGenerator.placeItems() 가 쓴다.
     */
    public static Item createRandom(int floorLevel) {
        // TODO(B): RandomUtil 로 층이 깊을수록 좋은 아이템이 나오게
        throw new UnsupportedOperationException("TODO(B): ItemFactory.createRandom");
    }
}
