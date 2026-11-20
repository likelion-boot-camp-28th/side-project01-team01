package com.textrpg.item;

/**
 * 모든 아이템의 부모 (추상 클래스). 직접 new 할 수 없고, Weapon·Armor·Potion 같은 자식만 만든다.
 * 아이템은 ItemFactory.createById() 로만 만든다 (세이브 복원과 같은 경로).
 *
 * <p>필드는 모두 private 이므로 자식 클래스도 getter 로 읽는다.
 * 생성자만 protected 로 둔다 (private 이면 Weapon·Armor·Potion 이 super(...) 를 부를 수 없다).
 *
 * <p>드랍 층수는 MonsterType 의 minFloor/maxFloor 와 같은 방식이다.
 * minFloor 층부터 maxFloor 층까지 바닥에 놓일 수 있다.
 */
public abstract class Item {

    /** 세이브 파일에 쓰는 고유 id. 예: "IRON_SWORD" */
    private final String id;
    private final String name;
    private final char symbol;
    private final ItemType type;
    /** 아이템 설명. 가방 화면 등에서 보여 준다. 예: "평범한 철로 만든 검." */
    private final String description;
    /** 이 아이템이 나오기 시작하는 층 (1부터). */
    private final int minFloor;
    /** 이 아이템이 나오는 마지막 층. */
    private final int maxFloor;

    protected Item(String id, String name, ItemType type, String description, int minFloor, int maxFloor) {
        if (minFloor < 1 || maxFloor < minFloor) {
            throw new IllegalArgumentException(
                    "드랍 층수가 잘못되었습니다: " + id + " (" + minFloor + "~" + maxFloor + ")");
        }
        this.id = id;
        this.name = name;
        this.symbol = type.getSymbol();
        this.type = type;
        this.description = description;
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
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

    public String getDescription() {
        return description;
    }

    public int getMinFloor() {
        return minFloor;
    }

    public int getMaxFloor() {
        return maxFloor;
    }

    /** floorLevel 층에서 나올 수 있는 아이템이면 true. ItemFactory.createRandom() 에서 쓴다. */
    public boolean canDropOn(int floorLevel) {
        return minFloor <= floorLevel && floorLevel <= maxFloor;
    }

    /**
     * 아이템을 사용한다. 결과 문장을 돌려주고, TurnSystem 이 MessageLog 에 넣는다.
     * 무기·방어구는 장착, 물약은 효과 적용 후 가방에서 제거.
     *
     * @param user 아이템을 쓰는 쪽 (Player)
     */
    public abstract String use(ItemUser user);
}
