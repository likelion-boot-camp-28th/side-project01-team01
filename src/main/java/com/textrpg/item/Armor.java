package com.textrpg.item;

/**
 * 방어구. 장착하면 방어력에 defenseBonus 가 더해진다.
 */
public class Armor extends Item {

    private final int defenseBonus;

    public Armor(String id, String name, String description, int minFloor, int maxFloor, int defenseBonus) {
        super(id, name, ItemType.ARMOR, description, minFloor, maxFloor);
        this.defenseBonus = defenseBonus;
    }

    public int getDefenseBonus() {
        return defenseBonus;
    }

    @Override
    public String use(ItemUser user) {
        // TODO(B): user.equip(this) 결과 돌려주기
        throw new UnsupportedOperationException("TODO(B): Armor.use");
    }
}
