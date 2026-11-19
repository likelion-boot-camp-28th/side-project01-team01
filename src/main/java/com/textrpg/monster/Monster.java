package com.textrpg.monster;

import com.textrpg.entity.Entity;
import com.textrpg.entity.Position;
import com.textrpg.entity.Stats;

public class Monster extends Entity {

    private MonsterType type;
    private boolean awake;

    public Monster(MonsterType type, Position pos) {
        super(type.name(), type.symbol, pos, new Stats(type.hp, 0, type.attack, type.defense, 50 + (25 * type.speed), 50, 25 * type.speed));
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

    public boolean isAwake() {
        return awake;
    }

    public void wakeUp(){awake = true;}
}
