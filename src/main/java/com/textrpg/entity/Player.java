package com.textrpg.entity;

import java.util.List;
import java.util.Optional;

import com.textrpg.core.Position;
import com.textrpg.item.Armor;
import com.textrpg.item.Inventory;
import com.textrpg.item.Item;
import com.textrpg.item.ItemFactory;
import com.textrpg.item.ItemUser;
import com.textrpg.item.Weapon;

/**
 * 플레이어 캐릭터. Entity 를 상속하고, 직업·레벨·경험치·골드·장비·가방을 더 가진다.
 * 실제 공격력 = 기본 공격력 + 무기 공격력, 실제 방어력 = 기본 방어력 + 방어구 방어력.
 */
public class Player extends Entity implements ItemUser {
    public static final char SYMBOL = '@';
    /**
     * 배고픔 최대치 (P1).
     */
    public static final int MAX_HUNGER = 100;
    /**
     * 이 턴 수마다 HP 1 회복 (자연 회복).
     */
    public static final int REGEN_INTERVAL = 10;
    public static final int LEVEL_UP_HP = 6;
    public static final int LEVEL_UP_MP = 2;
    public static final int LEVEL_UP_ATTACK = 1;
    public static final int LEVEL_UP_DEFENSE = 1;
    public static final int EXP_PER_LEVEL = 20;

    private final Job job;
    private int level = 1;
    private int exp;
    private int gold;
    private int hunger = MAX_HUNGER;
    private Weapon weapon;
    private Armor armor;
    private final Inventory inventory = new Inventory();
    /**
     * 경험치 배율. 영구 강화(배움의 축복, P1)에 따라 Profile.applyRunModifiers() 에서 바꾼다.
     */
    private double expMultiplier = 1.0;

    /**
     * 새 캐릭터를 만든다. 위치는 (0,0)으로 두므로, 층에 들어갈 때 setPos(floor.getStartPos()) 로 옮긴다.
     */
    public Player(String name, Job job) {
        super(name, SYMBOL, new Position(0, 0), job.createBaseStats());
        this.job = job;
    }

    /**
     * 세이브 파일([PLAYER], [INVENTORY])에서 읽은 값으로 플레이어를 되살린다. SaveManager(SlotCodec) 가 호출한다.
     *
     * <p>attack·defense 는 장비 보너스를 뺀 기본값이다 (getBaseAttack()·getBaseDefense() 로 저장한 값).
     * 무기·방어구와 가방 아이템은 SlotCodec 이 ItemFactory.createById() 로 만들어 넘긴다 (팀 규칙: Factory 로만 생성).
     * 강화로 늘어난 가방 칸·경험치 배율은 저장하지 않으므로, 불러온 뒤 Profile.applyRunModifiers(player) 를 부른다.
     *
     * @param weapon 끼고 있던 무기. 없으면 null
     * @param armor  입고 있던 방어구. 없으면 null
     * @param items  가방 안 아이템 (순서대로)
     * @throws IllegalArgumentException 값이 범위를 벗어나거나 무기 칸에 무기가 아닌 아이템이 올 때
     *                                  (SlotCodec 이 SaveLoadException 으로 감싼다)
     */
    public static Player restore(String name, Job job, int level, int exp, int gold,
                                 int hp, int maxHp, int mp, int maxMp, int attack, int defense,
                                 Position pos, Item weapon, Item armor, List<Item> items) {
        if (level < 1) {
            throw new IllegalArgumentException("level 은 1 이상입니다: " + level);
        }
        if (exp < 0 || exp >= level * EXP_PER_LEVEL) {
            throw new IllegalArgumentException(
                    "exp 는 0 ~ " + (level * EXP_PER_LEVEL - 1) + " 입니다: " + exp);
        }
        if (gold < 0) {
            throw new IllegalArgumentException("gold 는 0 이상입니다: " + gold);
        }
        if (maxHp < 1 || hp < 1 || hp > maxHp) {
            throw new IllegalArgumentException("hp 는 1 ~ maxHp 입니다: " + hp + "/" + maxHp);
        }
        if (maxMp < 0 || mp < 0 || mp > maxMp) {
            throw new IllegalArgumentException("mp 는 0 ~ maxMp 입니다: " + mp + "/" + maxMp);
        }
        if (attack < 0 || defense < 0) {
            throw new IllegalArgumentException("attack·defense 는 0 이상입니다: " + attack + ", " + defense);
        }
        if (weapon != null && !(weapon instanceof Weapon)) {
            throw new IllegalArgumentException("무기 칸에 무기가 아닌 아이템이 있습니다: " + weapon.getId());
        }
        if (armor != null && !(armor instanceof Armor)) {
            throw new IllegalArgumentException("방어구 칸에 방어구가 아닌 아이템이 있습니다: " + armor.getId());
        }

        Player p = new Player(name, job);
        p.level = level;
        p.exp = exp;
        p.gold = gold;

        Stats stats = p.getStats();
        stats.addMaxHp(maxHp - stats.getMaxHp());
        stats.setHp(hp);
        stats.addMaxMp(maxMp - stats.getMaxMp());
        stats.setMp(mp);
        stats.setAttack(attack);
        stats.setDefense(defense);

        p.setPos(pos);
        p.weapon = (Weapon) weapon;
        p.armor = (Armor) armor;

        // 큰 가방 강화로 기본 칸보다 많이 들고 있었을 수 있으니, 우선 다 들어가게 칸을 맞춘다.
        // 정확한 칸 수는 Profile.applyRunModifiers() 가 다시 맞춘다.
        p.inventory.setExtraCapacity(items.size() - Inventory.CAPACITY);
        for (Item item : items) {
            p.inventory.add(item);
        }
        return p;
    }

    /**
     * 최종 공격력 = 기본 공격력 + 무기 공격력. 전투(DamageCalculator)는 이 값을 쓴다.
     */
    @Override
    public int getAttack() {
        return getBaseAttack() + getWeaponAttack();
    }

    /**
     * 최종 방어력 = 기본 방어력 + 방어구 방어력.
     */
    @Override
    public int getDefense() {
        return getBaseDefense() + getArmorDefense();
    }

    /**
     * 장비를 뺀 기본 공격력 (직업 시작값 + 레벨업 + 영구 강화).
     */
    public int getBaseAttack() {
        return getStats().getAttack();
    }

    /**
     * 무기로 늘어난 공격력. 맨손이면 0.
     */
    public int getWeaponAttack() {
        return (weapon == null) ? 0 : weapon.getAttackBonus();
    }

    /**
     * 장비를 뺀 기본 방어력.
     */
    public int getBaseDefense() {
        return getStats().getDefense();
    }

    /**
     * 방어구로 늘어난 방어력. 방어구가 없으면 0.
     */
    public int getArmorDefense() {
        return (armor == null) ? 0 : armor.getDefenseBonus();
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

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void addGold(int amount) {
        gold += Math.max(0, amount);
    }

    /**
     * 시작 보급품: 치유 물약 count 개를 가방에 넣는다 (영구 강화 START_POTION).
     * meta 패키지는 item 을 import 할 수 없어서(패키지 의존 방향) Profile.applyTo() 가 이 메서드를 부른다.
     */
    public void giveStartPotions(int count) {
        for (int i = 0; i < count; i++) {
            inventory.add(ItemFactory.createById("HP_POTION_S"));
        }
    }

    public void setExpMultiplier(double expMultiplier) {
        this.expMultiplier = expMultiplier;
    }

    /**
     * 경험치를 얻는다. 레벨이 올랐으면 true.
     */
    public boolean gainExp(int amount) {
        exp += (int) (amount * expMultiplier);
        boolean leveledUp = false;

        while (exp >= (level * EXP_PER_LEVEL)) {
            exp -= (level * EXP_PER_LEVEL);
            levelUp();
            leveledUp = true;
        }
        return leveledUp;
    }

    private void levelUp() {
        level++;
        getStats().addMaxHp(LEVEL_UP_HP);
        getStats().addMaxMp(LEVEL_UP_MP);
        getStats().setAttack(getStats().getAttack() + LEVEL_UP_ATTACK);
        getStats().setDefense(getStats().getDefense() + LEVEL_UP_DEFENSE);
    }

    @Override
    public int getMaxHp() {
        return getStats().getMaxHp();
    }

    @Override
    public int getMaxMp() {
        return getStats().getMaxMp();
    }

    /**
     * MP 를 회복하고, 실제로 회복된 MP 양을 돌려준다.
     */
    @Override
    public int restoreMp(int amount) {
        Stats stats = getStats();
        int before = stats.getMp();
        stats.setMp(before + Math.max(0, amount));
        return stats.getMp() - before;
    }

    /**
     * 무기나 방어구를 장착한다. 결과 문장을 돌려준다.
     */
    @Override
    public String equip(Item item) {
        if (item instanceof Weapon) {
            Weapon newWeapon = (Weapon) item;
            Weapon oldWeapon = weapon;                   // ① 원래 무기 기억해 두기 (없으면 null)

            int index = inventory.getItems().indexOf(newWeapon);   // ② 새 무기를 가방에서 빼기
            if (index >= 0) {
                inventory.remove(index);
            }

            weapon = newWeapon;

            if (oldWeapon != null) {                     // ④ 원래 무기가 있었으면 가방으로
                inventory.add(oldWeapon);
            }
            return newWeapon.getDisplayName() + "을(를) 장착했다.";

        }

        if (item instanceof Armor) {
            Armor newArmor = (Armor) item;
            Armor oldArmor = armor;

            int index = inventory.getItems().indexOf(newArmor);
            if (index >= 0) {
                inventory.remove(index);
            }

            armor = newArmor;

            if (oldArmor != null) {
                inventory.add(oldArmor);
            }
            return newArmor.getDisplayName() + "을(를) 장착했다.";
        }

        return item.getDisplayName() + "은(는) 장착할 수 없다.";
    }

    /**
     * 가방의 index 번 아이템을 사용한다. 결과 문장을 돌려준다.
     */
    public String useItem(int index) {
        return inventory.get(index).use(this);
    }

    /**
     * 가방의 index 번 아이템을 꺼내서 돌려준다. 바닥에 놓는 일(Floor.addItem)은 TurnSystem 이 한다.
     * Player 가 Floor 를 알면 entity → map → monster → entity 로 패키지가 서로를 참조하게 되기 때문이다.
     */
    public Item dropItem(int index) {
        return inventory.remove(index);
    }

    /**
     * 턴마다 호출한다. REGEN_INTERVAL 턴마다 HP 1 회복 (배고픔은 P1).
     */
    public void regenerate(int turn) {
        if (turn % REGEN_INTERVAL == 0) {
            heal(1);
        }
    }
}
