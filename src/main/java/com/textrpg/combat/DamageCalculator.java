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
        int damage = Math.toIntExact(Math.max(1, Math.round((atk.getAttack() - def.getDefense()) * RandomUtil.nextDouble(0.9, 1.1))));
        if(crit) return Math.toIntExact(Math.round(damage * 1.5));
        else return damage;
    }
}
