package com.textrpg.monster;

import com.textrpg.core.Position;
import com.textrpg.entity.Entity;
import com.textrpg.entity.Stats;

public class Monster extends Entity {

    private MonsterType type;
    private boolean awake;

    public Monster(MonsterType type, Position pos) {
        super(type.displayName, type.symbol, pos, new Stats(type.hp, 0, type.attack, type.defense, 70 + (15 * type.speed), 5 * type.speed, 5 * type.speed));
        this.type = type;
        this.awake = false;
    }

    public MonsterType getType() {
        return type;
    }

    @Override
    public int getAttack() {return type.attack;}

    @Override
    public int getDefense() {return type.defense;}

    public int getExpReward(){return type.exp;}

    public int getGoldReward(){return type.gold;}

    public boolean isAwake() {
        return awake;
    }

    public void wakeUp(){awake = true;}
}
