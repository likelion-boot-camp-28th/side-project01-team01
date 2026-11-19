package com.textrpg.combat;

import com.textrpg.entity.Entity;
import com.textrpg.util.RandomUtil;

public class DamageCalculator {

    public static boolean rollHit(Entity atk, Entity def){
        return RandomUtil.chance(atk.getStats().getHitRate()) && !RandomUtil.chance(def.getStats().getEvadeRate());
    }

    public static boolean rollCritical(Entity atk){
        return RandomUtil.chance(atk.getStats().getCritRate());
    }

    public static int calculate(Entity atk, Entity def, boolean crit){
        int damage = Math.max(1, atk.getAttack() - def.getDefense()) * (int) (10 * RandomUtil.nextDouble(0.9, 1.1)) / 10;
        if(crit) return damage + (damage / 2);
        else return damage;
    }
}
