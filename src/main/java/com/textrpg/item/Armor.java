package com.textrpg.item;

/**
 * 방어구. 장착하면 방어력에 defenseBonus 가 더해진다.
 */
public class Armor extends Item {

    private final int defenseBonus;

    Armor(ItemKind kind, int defenseBonus) {
        super(kind, ItemType.ARMOR);
        this.defenseBonus = defenseBonus;
    }

    public int getDefenseBonus() {
        return defenseBonus;
    }

    @Override
    protected String effectText() {
        return "방어력 +" + defenseBonus;
    }

    @Override
    public String use(ItemUser user) {
        return user.equip(this);
    }
}
