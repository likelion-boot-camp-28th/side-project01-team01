package com.textrpg.item;

import com.textrpg.entity.Player;
import com.textrpg.map.Floor;

/**
 * 모든 아이템의 부모 (추상 클래스). 직접 new 할 수 없고, Weapon·Armor·Potion 같은 자식만 만든다.
 * 아이템은 ItemFactory.createById() 로만 만든다 (세이브 복원과 같은 경로).
 */
public abstract class Item {

    /** 세이브 파일에 쓰는 고유 id. 예: "IRON_SWORD" */
    protected final String id;
    protected final String name;
    protected final char symbol;
    protected final ItemType type;

    protected Item(String id, String name, ItemType type) {
        this.id = id;
        this.name = name;
        this.symbol = type.getSymbol();
        this.type = type;
    }

    public String getId() {
        return id;
    }

    /** 화면에 보일 이름. 미확인 아이템(P1)이 생기면 ItemIdentifier 를 거친다. */
    public String getDisplayName() {
        return name;
    }

    public char getSymbol() {
        return symbol;
    }

    public ItemType getType() {
        return type;
    }

    /**
     * 아이템을 사용한다. 결과 문장을 돌려주고, TurnSystem 이 MessageLog 에 넣는다.
     * 무기·방어구는 장착, 물약은 효과 적용 후 가방에서 제거.
     */
    public abstract String use(Player p, Floor f);
}
