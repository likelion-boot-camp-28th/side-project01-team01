package com.textrpg.item;

/**
 * 무기. 장착하면 공격력에 attackBonus 가 더해진다.
 */
public class Weapon extends Item {

    private final int attackBonus;

    Weapon(ItemKind kind, int attackBonus) {
        super(kind, ItemType.WEAPON);
        this.attackBonus = attackBonus;
    }

    public int getAttackBonus() {
        return attackBonus;
    }

    @Override
    protected String effectText() {
        return "공격력 +" + attackBonus;
    }

    @Override
    public String use(ItemUser user) {
        return user.equip(this);
    }
}
