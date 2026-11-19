package com.textrpg.entity;

/**
 * 맵 위에 존재하는 생명체(플레이어, 몬스터)의 공통 부모.
 * 장비 보너스 등이 붙는 실제 공격력·방어력은 하위 클래스가 getAttack()/getDefense() 로 계산한다.
 */
public abstract class Entity {

    protected final String name;
    protected final char symbol;
    protected Position pos;
    protected final Stats stats;

    protected Entity(String name, char symbol, Position pos, Stats stats) {
        this.name = name;
        this.symbol = symbol;
        this.pos = pos;
        this.stats = stats;
    }

    public String getName() {
        return name;
    }

    public char getSymbol() {
        return symbol;
    }

    public Position getPos() {
        return pos;
    }

    public void setPos(Position pos) {
        this.pos = pos;
    }

    public Stats getStats() {
        return stats;
    }

    public boolean isAlive() {
        return stats.getHp() > 0;
    }

    /** 피해를 받고, 실제로 깎인 HP 양을 돌려준다. */
    public int takeDamage(int amount) {
        int before = stats.getHp();
        stats.setHp(before - Math.max(0, amount));
        return before - stats.getHp();
    }

    /** 회복하고, 실제로 회복된 HP 양을 돌려준다. */
    public int heal(int amount) {
        int before = stats.getHp();
        stats.setHp(before + Math.max(0, amount));
        return stats.getHp() - before;
    }

    public abstract int getAttack();

    public abstract int getDefense();
}
