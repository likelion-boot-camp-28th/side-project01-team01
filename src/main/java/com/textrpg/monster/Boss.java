package com.textrpg.monster;

import com.textrpg.entity.Position;

public class Boss extends Monster{
    public Boss(Position pos) {
        super(MonsterType.ABYSS_WARDEN, pos);
    }

    public int attacksPerTurn(){
        if(this.getStats().getHp() <= this.getStats().getMaxHp() / 2) return 2;
        else return 1;
    }
}
