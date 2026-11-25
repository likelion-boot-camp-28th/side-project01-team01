package com.textrpg.item;

/**
 * 모든 아이템의 부모 (추상 클래스). 직접 new 할 수 없고, Weapon·Armor·Potion 같은 자식만 만든다.
 * 아이템은 ItemFactory.createById() 로만 만든다 (세이브 복원과 같은 경로).
 *
 * <p>필드는 모두 private 이므로 자식 클래스도 getter 로 읽는다.
 * 생성자만 protected 로 둔다 (private 이면 Weapon·Armor·Potion 이 super(...) 를 부를 수 없다).
 *
 * <p>이름·설명·드랍 층 같은 종류별 데이터는 ItemKind 에 있고, 여기서는 종류를 가리키기만 한다.
 */
public abstract class Item {

    /** 아이템 종류. id·이름·설명·드랍 층은 여기서 읽는다. */
    private final ItemKind kind;
    private final ItemType type;

    protected Item(ItemKind kind, ItemType type) {
        this.kind = kind;
        this.type = type;
    }

    /** 세이브 파일에 쓰는 고유 id. 예: "IRON_SWORD" */
    public String getId() {
        return kind.name();
    }

    public ItemKind getKind() {
        return kind;
    }

    /** 화면에 보일 이름. 미확인 아이템(P1)이 생기면 ItemIdentifier 를 거친다. */
    public String getDisplayName() {
        return kind.getDisplayName();
    }

    public char getSymbol() {
        return type.getSymbol();
    }

    public ItemType getType() {
        return type;
    }

    /** 가방 화면 등에서 보여 줄 설명. 종류의 설명 뒤에 실제 수치를 붙인다. 예: "평범한 철로 만든 검. 공격력 +3" */
    public String getDescription() {
        return kind.getDescription() + " " + effectText();
    }

    public int getMinFloor() {
        return kind.getMinFloor();
    }

    public int getMaxFloor() {
        return kind.getMaxFloor();
    }

    /** floorLevel 층에서 나올 수 있는 아이템이면 true. */
    public boolean canDropOn(int floorLevel) {
        return kind.canDropOn(floorLevel);
    }

    /** 효과 수치만. 상점 목록처럼 설명을 다 쓰기엔 좁은 곳에 쓴다. 예: "공격력 +3" */
    public String getEffectText() {
        return effectText();
    }

    /** 상점에서 살 때의 고정 가격(골드). */
    public int getPrice() {
        return kind.getPrice();
    }

    /** 설명 뒤에 붙일 효과 수치. 예: "공격력 +3", "HP 10 회복" */
    protected abstract String effectText();

    /**
     * 아이템을 사용한다. 결과 문장을 돌려주고, TurnSystem 이 MessageLog 에 넣는다.
     * 무기·방어구는 장착, 물약은 효과 적용 후 가방에서 제거.
     *
     * @param user 아이템을 쓰는 쪽 (Player)
     */
    public abstract String use(ItemUser user);
}
