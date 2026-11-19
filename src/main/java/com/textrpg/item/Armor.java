package com.textrpg.item;

import com.textrpg.entity.Player;
import com.textrpg.map.Floor;

/**
 * 방어구. 장착하면 방어력에 defenseBonus 가 더해진다.
 */
public class Armor extends Item {

    private final int defenseBonus;

    public Armor(String id, String name, int defenseBonus) {
        super(id, name, ItemType.ARMOR);
        this.defenseBonus = defenseBonus;
    }

    public int getDefenseBonus() {
        return defenseBonus;
    }

    @Override
    public String use(Player p, Floor f) {
        // TODO(B): p.equip(this) 결과 돌려주기
        throw new UnsupportedOperationException("TODO(B): Armor.use");
    }
}
