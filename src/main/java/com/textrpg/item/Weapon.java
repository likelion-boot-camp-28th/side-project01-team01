package com.textrpg.item;

/**
 * 무기. 장착하면 공격력에 attackBonus 가 더해진다.
 */
public class Weapon extends Item {

    private final int attackBonus;

    public Weapon(String id, String name, String description, int minFloor, int maxFloor, int attackBonus) {
        super(id, name, ItemType.WEAPON, description, minFloor, maxFloor);
        this.attackBonus = attackBonus;
    }

    public int getAttackBonus() {
        return attackBonus;
    }

    @Override
    public String use(ItemUser user) {
        return user.equip(this);
    }
}
