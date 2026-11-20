package com.textrpg.monster;

import com.textrpg.core.Position;
import com.textrpg.util.RandomUtil;

public class MonsterFactory {

    public static Monster create(MonsterType t, Position pos){
        if(t == MonsterType.ABYSS_WARDEN) return createBoss(pos);
        else return new Monster(t, pos);
    }

    public static Monster createRandom(int floorLevel, Position pos){

        int maxOrd = MonsterType.ABYSS_WARDEN.ordinal() - 1;
        int randOrd = RandomUtil.nextInt(0, maxOrd);

        if(floorLevel <= 0 || floorLevel >= 6) throw new IllegalArgumentException("Impossible floorLevel");

        while(!(floorLevel >= MonsterType.values()[randOrd].minFloor && floorLevel <= MonsterType.values()[randOrd].maxFloor)) randOrd = RandomUtil.nextInt(0, maxOrd);

        return new Monster(MonsterType.values()[randOrd], pos);
    }

    public static Monster createBoss(Position pos){return new Boss(pos);}
}
