package com.textrpg.monster;

import com.textrpg.entity.Position;

public class MonsterFactory {

    public static Monster create(MonsterType t, Position pos){
        if(t.equals(MonsterType.ABYSS_WARDEN)) return createBoss(pos);
        else return new Monster(t, pos);
    }

    public static Monster create(int floorLevel, Position pos){return null;}

    public static Monster createBoss(Position pos){return new Boss(pos);}
}
