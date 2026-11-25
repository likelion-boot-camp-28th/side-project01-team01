package com.textrpg.shop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import com.textrpg.core.Position;
import com.textrpg.entity.Player;
import com.textrpg.item.Item;
import com.textrpg.item.ItemFactory;
import com.textrpg.item.ItemKind;
import com.textrpg.util.RandomUtil;

/**
 * 층에 하나 있을 수 있는 상점. 상인 위치, 진열 상품, 치료를 받았는지를 가진다.
 * 상점방 배치는 DungeonGenerator 가, 상점 화면과 입력은 GameManager 가 맡고, 사고팔기 규칙은 여기서 정한다.
 *
 * <ul>
 *   <li>진열: 칸마다 그 층에서 나올 수 있는 아이템 중 하나를 같은 확률로 고른다. 이미 진열된 종류면 그 층 아이템 중에서
 *       한 번만 다시 고르고, 또 겹치면 그대로 들인다. 칸마다 처음 고를 때만 NEXT_FLOOR_PERCENT 확률로 다음 층부터 나오는
 *       아이템을 고른다 (상점 하나에 하나라도 들어올 확률은 약 10%).</li>
 *   <li>구매: 아이템마다 고정 가격(ItemKind 상점가). 산 상품은 진열에서 빠진다.</li>
 *   <li>판매: 상점가의 SELL_PERCENT % (소수점 버림). 장착 중인 장비는 가방에 없으므로 팔 수 없다.</li>
 *   <li>치료: 층 × HEAL_COST_PER_FLOOR 골드로 최대 HP 의 HEAL_PERCENT % 회복. 상점마다 한 번.</li>
 * </ul>
 */
public class Shop {

    public static final char MERCHANT_SYMBOL = 'M';
    public static final String MERCHANT_NAME = "떠돌이 상인";
    /** 상점이 나올 수 있는 가장 얕은 층. */
    public static final int MIN_FLOOR = 2;
    /** MIN_FLOOR 층부터 층마다 상점이 나올 확률(%). */
    public static final int APPEAR_PERCENT = 25;
    public static final int STOCK_SIZE = 5;
    /** 진열 칸마다 (처음 고를 때만) 다음 층부터 나오는 아이템이 들어올 확률(%). 5칸이면 상점 하나에 약 10%. */
    public static final int NEXT_FLOOR_PERCENT = 2;
    /** 팔 때 받는 값. 상점가의 몇 % 인지. */
    public static final int SELL_PERCENT = 20;
    public static final int HEAL_COST_PER_FLOOR = 10;
    /** 치료로 회복하는 양. 최대 HP 의 몇 % 인지. */
    public static final int HEAL_PERCENT = 50;

    private final int level;
    private final Position merchantPos;
    private final List<Item> stock;
    private boolean healUsed;

    private Shop(int level, Position merchantPos, List<Item> stock, boolean healUsed) {
        this.level = level;
        this.merchantPos = merchantPos;
        this.stock = new ArrayList<>(stock);
        this.healUsed = healUsed;
    }

    /**
     * 새 상점을 열고 진열 상품 STOCK_SIZE 개를 고른다. DungeonGenerator 가 부른다.
     *
     * @param level       층 번호 (진열 상품과 치료 가격이 정해진다)
     * @param merchantPos 상인이 서 있는 칸
     */
    public static Shop open(int level, Position merchantPos) {
        List<Item> items = rollStock(level).stream()
                .map(kind -> ItemFactory.createById(kind.name()))
                .toList();
        return new Shop(level, merchantPos, items, false);
    }

    /**
     * 세이브 파일([SHOP])에서 읽은 값으로 상점을 되살린다. 이미 팔린 상품은 stock 에 없다.
     *
     * @throws IllegalArgumentException 진열 상품이 STOCK_SIZE 개보다 많을 때 (SlotCodec 이 SaveLoadException 으로 감싼다)
     */
    public static Shop restore(int level, Position merchantPos, List<Item> stock, boolean healUsed) {
        if (stock.size() > STOCK_SIZE) {
            throw new IllegalArgumentException("진열 상품은 " + STOCK_SIZE + "개까지입니다: " + stock.size());
        }
        return new Shop(level, merchantPos, stock, healUsed);
    }

    /**
     * 진열할 아이템 종류를 STOCK_SIZE 개 고른다. 화면에서 찾기 쉽도록 ItemKind 선언 순서(무기 → 방어구 → 물약)로 정렬한다.
     */
    static List<ItemKind> rollStock(int level) {
        List<ItemKind> kinds = new ArrayList<>();
        for (int i = 0; i < STOCK_SIZE; i++) {
            ItemKind kind = rollKind(level, true);
            if (kinds.contains(kind)) {
                // 겹치면 그 층 아이템 중에서 한 번만 다시 고른다. 또 겹치면 그대로 들인다
                kind = rollKind(level, false);
            }
            kinds.add(kind);
        }
        kinds.sort(Comparator.naturalOrder());
        return kinds;
    }

    /**
     * 진열 한 칸. allowNextFloor 이고 다음 층에서 처음 나오는 아이템이 있으면 NEXT_FLOOR_PERCENT 확률로 그중에서 고른다.
     * 다시 고를 때 다음 층 아이템을 빼는 것은, 2층처럼 종류가 적어 자주 겹치는 층에서 확률이 커지지 않게 하기 위해서다.
     */
    private static ItemKind rollKind(int level, boolean allowNextFloor) {
        List<ItemKind> nextFloorOnly = kindsWhere(kind -> kind.canDropOn(level + 1) && !kind.canDropOn(level));
        if (allowNextFloor && !nextFloorOnly.isEmpty() && RandomUtil.chance(NEXT_FLOOR_PERCENT)) {
            return RandomUtil.pick(nextFloorOnly);
        }
        return RandomUtil.pick(kindsWhere(kind -> kind.canDropOn(level)));
    }

    private static List<ItemKind> kindsWhere(Predicate<ItemKind> condition) {
        return Arrays.stream(ItemKind.values()).filter(condition).toList();
    }

    /** 팔 때 받는 골드. 상점가의 SELL_PERCENT % (소수점 버림). */
    public static int sellPrice(Item item) {
        return item.getPrice() * SELL_PERCENT / 100;
    }

    public int getLevel() {
        return level;
    }

    public Position getMerchantPos() {
        return merchantPos;
    }

    /** 남은 진열 상품. 번호(index)는 0부터, 화면에는 1부터 보여 준다. */
    public List<Item> getStock() {
        return List.copyOf(stock);
    }

    public boolean isHealUsed() {
        return healUsed;
    }

    /** 치료 가격. 층 × HEAL_COST_PER_FLOOR. */
    public int getHealCost() {
        return level * HEAL_COST_PER_FLOOR;
    }

    /**
     * index 번 상품을 산다. 가방에 넣고 골드를 낸 뒤 진열에서 뺀다. 결과 문장을 돌려준다.
     * 번호가 없거나, 가방이 가득 찼거나, 골드가 모자라면 아무것도 바뀌지 않는다.
     */
    public String buy(Player player, int index) {
        if (index < 0 || index >= stock.size()) {
            return (index + 1) + "번 상품은 없습니다.";
        }
        Item item = stock.get(index);
        if (player.getInventory().isFull()) {
            return "가방이 가득 차서 " + item.getDisplayName() + "을(를) 살 수 없습니다.";
        }
        if (!player.spendGold(item.getPrice())) {
            return "골드가 모자랍니다. (" + item.getDisplayName() + " " + item.getPrice()
                    + "골드, 보유 " + player.getGold() + "골드)";
        }
        player.getInventory().add(item);
        stock.remove(index);
        return item.getDisplayName() + "을(를) " + item.getPrice() + "골드에 샀습니다.";
    }

    /**
     * 가방의 index 번 아이템을 판다. 판 아이템은 진열되지 않고 사라진다. 결과 문장을 돌려준다.
     */
    public String sell(Player player, int index) {
        if (index < 0 || index >= player.getInventory().size()) {
            return "가방 " + (index + 1) + "번 칸에는 아이템이 없습니다.";
        }
        Item item = player.getInventory().remove(index);
        int price = sellPrice(item);
        player.addGold(price);
        return item.getDisplayName() + "을(를) " + price + "골드에 팔았습니다.";
    }

    /**
     * 치료를 받는다: getHealCost() 골드를 내고 최대 HP 의 HEAL_PERCENT % 를 회복한다. 결과 문장을 돌려준다.
     * 이미 받았거나, HP 가 가득하거나, 골드가 모자라면 골드를 내지 않는다.
     */
    public String heal(Player player) {
        if (healUsed) {
            return "치료는 이 상점에서 이미 받았습니다.";
        }
        if (player.getStats().getHp() >= player.getMaxHp()) {
            return "HP 가 이미 가득합니다.";
        }
        if (!player.spendGold(getHealCost())) {
            return "골드가 모자랍니다. (치료 " + getHealCost() + "골드, 보유 " + player.getGold() + "골드)";
        }
        healUsed = true;
        int healed = player.heal(Math.max(1, player.getMaxHp() * HEAL_PERCENT / 100));
        return "치료를 받아 HP 를 " + healed + " 회복했습니다. (" + getHealCost() + "골드)";
    }
}
