package com.textrpg.entity;

import java.util.Optional;

import com.textrpg.core.Position;
import com.textrpg.item.Armor;
import com.textrpg.item.Inventory;
import com.textrpg.item.Item;
import com.textrpg.item.Weapon;
import com.textrpg.map.Floor;

/**
 * 플레이어 캐릭터. Entity 를 상속하고, 직업·레벨·경험치·골드·장비·가방을 더 가진다.
 * 실제 공격력/방어력 = 기본 능력치 + 장비 보너스.
 */
public class Player extends Entity {

    public static final char SYMBOL = '@';
    /** 배고픔 최대치 (P1). */
    public static final int MAX_HUNGER = 100;
    /** 이 턴 수마다 HP 1 회복 (자연 회복). */
    public static final int REGEN_INTERVAL = 10;

    private final Job job;
    private int level = 1;
    private int exp;
    private int gold;
    private int hunger = MAX_HUNGER;
    private Weapon weapon;
    private Armor armor;
    private final Inventory inventory = new Inventory();
    /** 경험치 배율. 영구 강화(배움의 축복, P1)가 Profile.applyTo()다. 에서 바꾼 */
    private double expMultiplier = 1.0;

    /**
     * 새 캐릭터를 만든다. 위치는 (0,0)으로 두므로, 층에 들어갈 때 setPos(floor.getStartPos()) 로 옮긴다.
     */
    public Player(String name, Job job) {
        super(name, SYMBOL, new Position(0, 0), job.createBaseStats());
        this.job = job;
    }

    @Override
    public int getAttack() {
        int bonus = (weapon == null) ? 0 : weapon.getAttackBonus();
        return stats.getAttack() + bonus;
    }

    @Override
    public int getDefense() {
        int bonus = (armor == null) ? 0 : armor.getDefenseBonus();
        return stats.getDefense() + bonus;
    }

    public Job getJob() {
        return job;
    }

    public int getLevel() {
        return level;
    }

    public int getExp() {
        return exp;
    }

    public int getGold() {
        return gold;
    }

    public int getHunger() {
        return hunger;
    }

    public Optional<Weapon> getWeapon() {
        return Optional.ofNullable(weapon);
    }

    public Optional<Armor> getArmor() {
        return Optional.ofNullable(armor);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void addGold(int amount) {
        gold += Math.max(0, amount);
    }

    public void setExpMultiplier(double expMultiplier) {
        this.expMultiplier = expMultiplier;
    }

    /**
     * 경험치를 얻는다. 레벨이 올랐으면 true.
     */
    public boolean gainExp(int amount) {
        // TODO(B): amount * expMultiplier 만큼 exp 를 더하고, 기준을 넘으면 levelUp() 호출
        throw new UnsupportedOperationException("TODO(B): Player.gainExp");
    }

    private void levelUp() {
        // TODO(B): level 1 증가, 최대 HP·공격·방어 상승 (상승량은 상수로)
        throw new UnsupportedOperationException("TODO(B): Player.levelUp");
    }

    /**
     * 무기나 방어구를 장착한다. 결과 문장을 돌려준다.
     */
    public String equip(Item item) {
        // TODO(B): Weapon 이면 weapon 칸, Armor 이면 armor 칸에 끼우고, 원래 장비는 가방으로
        throw new UnsupportedOperationException("TODO(B): Player.equip");
    }

    /**
     * 가방의 index 번 아이템을 사용한다. 결과 문장을 돌려준다.
     */
    public String useItem(int index, Floor f) {
        // TODO(B): inventory.get(index).use(this, f)
        throw new UnsupportedOperationException("TODO(B): Player.useItem");
    }

    /**
     * 가방의 index 번 아이템을 발밑에 버린다. 결과 문장을 돌려준다.
     */
    public String dropItem(int index, Floor f) {
        // TODO(B): inventory.remove(index) → f.addItem(FloorItem.of(item, pos))
        throw new UnsupportedOperationException("TODO(B): Player.dropItem");
    }

    /**
     * 턴마다 호출한다. REGEN_INTERVAL 턴마다 HP 1 회복 (배고픔은 P1).
     */
    public void regenerate(int turn) {
        // TODO(B): turn % REGEN_INTERVAL == 0 이면 heal(1)
        throw new UnsupportedOperationException("TODO(B): Player.regenerate");
    }

    // TODO(B, A와 합의): 세이브 복원용으로 level, exp, gold, hp 등을 넣는 방법 (SaveManager 2차에서 필요)
}
