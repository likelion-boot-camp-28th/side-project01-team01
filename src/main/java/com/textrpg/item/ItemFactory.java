package com.textrpg.item;

import java.util.Arrays;
import java.util.List;

import com.textrpg.util.RandomUtil;

/**
 * 아이템은 모두 여기서 만든다. 세이브 파일의 id 로 다시 만들 때도 같은 메서드를 쓴다.
 * 아이템 종류와 수치는 ItemKind 에서 관리한다. 새 아이템은 ItemKind 에 한 줄만 추가하면 된다.
 */
public final class ItemFactory {

    private ItemFactory() {
    }

    /**
     * id(ItemKind 이름)로 아이템을 만든다.
     *
     * @throws IllegalArgumentException 모르는 id 일 때
     */
    public static Item createById(String id) {
        if (id == null) {
            throw new IllegalArgumentException("아이템 id 가 null 입니다.");
        }
        try {
            return ItemKind.valueOf(id).create();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("모르는 아이템 id: " + id, e);
        }
    }

    /**
     * 층에 맞는 아이템을 무작위로 만든다. D 의 DungeonGenerator.placeItems() 와 C 의 몬스터 드롭이 쓴다.
     * 그 층에서 나올 수 있는 종류(canDropOn) 중 하나를 같은 확률로 고른다.
     *
     * @throws IllegalArgumentException 그 층에서 나올 수 있는 아이템이 없을 때
     */
    public static Item createRandom(int floorLevel) {
        List<ItemKind> candidates = Arrays.stream(ItemKind.values())
                .filter(kind -> kind.canDropOn(floorLevel))
                .toList();
        return RandomUtil.pick(candidates).create();
    }
}
