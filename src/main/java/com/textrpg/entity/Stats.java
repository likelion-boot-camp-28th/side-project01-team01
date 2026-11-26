package com.textrpg.entity;

/**
 * 캐릭터·몬스터 공통 능력치. 확률 값(hitRate 등)은 퍼센트로 저장한다.
 * 명중률은 강화로 100 을 넘을 수 있다 (넘는 만큼은 효과가 없고, 회피는 따로 굴린다).
 * HP/MP 는 0 ~ max 범위를 벗어나지 않게 setter 에서 보정한다.
 */
public class Stats {

    private int hp;
    private int maxHp;
    private int mp;
    private int maxMp;
    private int attack;
    private int defense;
    private double hitRate;
    private double critRate;
    private double evadeRate;

    public Stats(int maxHp, int maxMp, int attack, int defense,
                 double hitRate, double critRate, double evadeRate) {
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.maxMp = maxMp;
        this.mp = maxMp;
        this.attack = attack;
        this.defense = defense;
        this.hitRate = hitRate;
        this.critRate = critRate;
        this.evadeRate = evadeRate;
    }

    /** 다른 Stats 를 그대로 복사한다 (직업 기본 스탯을 캐릭터마다 따로 갖게 할 때 사용). */
    public Stats copy() {
        Stats s = new Stats(maxHp, maxMp, attack, defense, hitRate, critRate, evadeRate);
        s.hp = hp;
        s.mp = mp;
        return s;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = clamp(hp, 0, maxHp);
    }

    public int getMaxHp() {
        return maxHp;
    }

    /** 최대 HP 를 바꾸면 현재 HP 도 같은 양만큼 함께 바뀐다(레벨업·영구 강화용). */
    public void addMaxHp(int amount) {
        this.maxHp = Math.max(1, maxHp + amount);
        setHp(hp + amount);
    }

    public int getMp() {
        return mp;
    }

    public void setMp(int mp) {
        this.mp = clamp(mp, 0, maxMp);
    }

    public int getMaxMp() {
        return maxMp;
    }

    public void addMaxMp(int amount) {
        this.maxMp = Math.max(0, maxMp + amount);
        setMp(mp + amount);
    }

    public int getAttack() {
        return attack;
    }

    public void setAttack(int attack) {
        this.attack = attack;
    }

    public int getDefense() {
        return defense;
    }

    public void setDefense(int defense) {
        this.defense = defense;
    }

    public double getHitRate() {
        return hitRate;
    }

    public void setHitRate(double hitRate) {
        this.hitRate = hitRate;
    }

    public double getCritRate() {
        return critRate;
    }

    public void setCritRate(double critRate) {
        this.critRate = critRate;
    }

    public double getEvadeRate() {
        return evadeRate;
    }

    public void setEvadeRate(double evadeRate) {
        this.evadeRate = evadeRate;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
