package com.textrpg.item;

import com.textrpg.entity.Player;
import com.textrpg.map.Floor;

/**
 * 무기. 장착하면 공격력에 attackBonus 가 더해진다.
 */
public class Weapon extends Item {

    private final int attackBonus;

    public Weapon(String id, String name, int attackBonus) {
        super(id, name, ItemType.WEAPON);
        this.attackBonus = attackBonus;
    }

    public int getAttackBonus() {
        return attackBonus;
    }

    @Override
    public String use(Player p, Floor f) {
        // TODO(B): p.equip(this) 결과 돌려주기
        throw new UnsupportedOperationException("TODO(B): Weapon.use");
    }
}
