package com.textrpg.shop;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import com.textrpg.core.Position;
import com.textrpg.entity.Job;
import com.textrpg.entity.Player;
import com.textrpg.item.Inventory;
import com.textrpg.item.Item;
import com.textrpg.item.ItemFactory;
import com.textrpg.item.ItemKind;
import com.textrpg.map.DungeonGenerator;
import com.textrpg.util.RandomUtil;

/**
 * Shop 확인용: 진열 상품 고르기, 구매, 판매, 치료. JUnit 대신 main 으로 실행한다 (Team_Rules 2.11).
 * 실패가 하나라도 있으면 종료 코드 1.
 */
public final class ShopCheck {

    /** 진열 확률을 볼 때 상점을 몇 번 열어 볼지. */
    private static final int ROLLS = 4000;
    private static final Position MERCHANT = new Position(5, 5);

    private static int passed;
    private static int failed;

    private ShopCheck() {
    }

    public static void main(String[] args) {
        RandomUtil.setSeed(42);
        checkPrices();
        checkStockRules();
        checkNextFloorRate();
        checkDuplicateReroll();
        checkBuy();
        checkSell();
        checkHeal();
        checkRestore();

        System.out.println("통과 " + passed + " / 실패 " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    /** 모든 아이템에 상점가가 있고, 팔 때는 상점가의 20% (소수점 버림). 가장 싼 물약도 1골드 이상 받는다. */
    private static void checkPrices() {
        for (ItemKind kind : ItemKind.values()) {
            Item item = ItemFactory.createById(kind.name());
            expect(kind + " 상점가 있음", item.getPrice() > 0, true);
            expect(kind + " 판매가는 구매가보다 낮음", Shop.sellPrice(item) < item.getPrice(), true);
            expect(kind + " 판매가 1골드 이상", Shop.sellPrice(item) >= 1, true);
        }
        expect("작은 치유 물약 20 → 4", Shop.sellPrice(ItemFactory.createById("HP_POTION_S")), 4);
        expect("큰 치유 물약 45 → 9", Shop.sellPrice(ItemFactory.createById("HP_POTION_L")), 9);
        expect("수호자의 판금 200 → 40", Shop.sellPrice(ItemFactory.createById("GUARDIAN_PLATE")), 40);
        expect("룬 대검 160 → 32", Shop.sellPrice(ItemFactory.createById("RUNE_BLADE")), 32);
    }

    /**
     * 진열은 항상 STOCK_SIZE 개, 이 층이나 바로 다음 층 아이템만, ItemKind 순서로 정렬.
     * 2층 아이템은 4종뿐이라 다음 층 아이템이 섞이지 않았으면 반드시 겹친다.
     */
    private static void checkStockRules() {
        for (int level = Shop.MIN_FLOOR; level <= DungeonGenerator.MAX_FLOOR; level++) {
            for (int i = 0; i < ROLLS; i++) {
                List<ItemKind> stock = Shop.rollStock(level);
                if (stock.size() != Shop.STOCK_SIZE) {
                    fail(level + "층 진열 개수", stock.size());
                    return;
                }
                int finalLevel = level;
                if (!stock.stream().allMatch(k -> k.canDropOn(finalLevel) || k.canDropOn(finalLevel + 1))) {
                    fail(level + "층 진열에 맞지 않는 층 아이템", stock);
                    return;
                }
                if (!stock.equals(stock.stream().sorted().toList())) {
                    fail(level + "층 진열 정렬", stock);
                    return;
                }
                boolean onlyThisFloor = stock.stream().allMatch(k -> k.canDropOn(finalLevel));
                if (level == Shop.MIN_FLOOR && onlyThisFloor && new HashSet<>(stock).size() == stock.size()) {
                    fail("2층 아이템만 있으면 4종이라 겹쳐야 함", stock);
                    return;
                }
            }
            passed++;
        }
        Shop shop = Shop.open(3, MERCHANT);
        expect("open: 상품 5개", shop.getStock().size(), Shop.STOCK_SIZE);
        expect("open: 치료 안 받음", shop.isHealUsed(), false);
        expect("open: 3층 치료 30골드", shop.getHealCost(), 30);
        expect("보스층에서는 5층 아이템만",
                Shop.rollStock(DungeonGenerator.MAX_FLOOR).stream().allMatch(k -> k.canDropOn(DungeonGenerator.MAX_FLOOR)),
                true);
    }

    /**
     * 다음 층 아이템이 하나라도 있는 상점은 1 - (1 - 2%)^5 ≈ 9.6%. 다시 고를 때는 다음 층 아이템을 빼므로,
     * 종류가 적어 자주 겹치는 2층도 다른 층과 비슷해야 한다.
     */
    private static void checkNextFloorRate() {
        double expected = 100 * (1 - Math.pow(1 - Shop.NEXT_FLOOR_PERCENT / 100.0, Shop.STOCK_SIZE));
        for (int level = Shop.MIN_FLOOR; level < DungeonGenerator.MAX_FLOOR; level++) {
            int withNext = 0;
            for (int i = 0; i < ROLLS; i++) {
                int finalLevel = level;
                if (Shop.rollStock(level).stream().anyMatch(kind -> !kind.canDropOn(finalLevel))) {
                    withNext++;
                }
            }
            double percent = withNext * 100.0 / ROLLS;
            expect(level + "층 다음 층 아이템이 있는 상점 " + String.format("%.1f", percent) + "% 가 "
                    + String.format("%.1f", expected) + "% ±3%p", Math.abs(percent - expected) <= 3, true);
        }
    }

    /** 겹치면 한 번 다시 고르므로, 그냥 5번 고를 때(3층 8종: 평균 약 3.9종)보다 서로 다른 종류가 많다. */
    private static void checkDuplicateReroll() {
        int distinct = 0;
        for (int i = 0; i < ROLLS; i++) {
            distinct += new HashSet<>(Shop.rollStock(3)).size();
        }
        double average = (double) distinct / ROLLS;
        expect("3층 평균 종류 수 " + String.format("%.2f", average) + " 가 4.2 이상", average >= 4.2, true);
    }

    private static void checkBuy() {
        Shop shop = sampleShop();
        Player player = new Player("테스트", Job.WARRIOR);
        player.addGold(55);

        String bought = shop.buy(player, 0);
        expect("구매 메시지", bought, "철 검을(를) 50골드에 샀습니다.");
        expect("구매: 골드 차감", player.getGold(), 5);
        expect("구매: 가방에 들어감", player.getInventory().get(0).getId(), "IRON_SWORD");
        expect("구매: 진열에서 빠짐", ids(shop.getStock()), List.of("HP_POTION_S"));

        String poor = shop.buy(player, 0);
        expect("골드 부족 메시지", poor.startsWith("골드가 모자랍니다."), true);
        expect("골드 부족: 그대로", List.of(player.getGold(), shop.getStock().size()), List.of(5, 1));

        expect("없는 번호", shop.buy(player, 3), "4번 상품은 없습니다.");
        expect("음수 번호", shop.buy(player, -1), "0번 상품은 없습니다.");

        Player full = new Player("가득", Job.WARRIOR);
        full.addGold(1000);
        for (int i = 0; i < Inventory.CAPACITY; i++) {
            full.getInventory().add(ItemFactory.createById("MP_POTION_S"));
        }
        expect("가방이 가득 차면 못 삼", shop.buy(full, 0).startsWith("가방이 가득 차서"), true);
        expect("가방이 가득: 골드·진열 그대로", List.of(full.getGold(), shop.getStock().size()), List.of(1000, 1));
    }

    private static void checkSell() {
        Shop shop = sampleShop();
        Player player = new Player("테스트", Job.WARRIOR);
        player.getInventory().add(ItemFactory.createById("HP_POTION_S"));
        player.getInventory().add(ItemFactory.createById("STEEL_SWORD"));

        expect("판매 메시지", shop.sell(player, 1), "강철 검을(를) 18골드에 팔았습니다.");
        expect("판매: 골드", player.getGold(), 18);
        expect("판매: 가방에서 빠짐", ids(player.getInventory().getItems()), List.of("HP_POTION_S"));
        expect("판매: 판 아이템은 진열되지 않음", shop.getStock().size(), 2);
        expect("없는 칸", shop.sell(player, 1), "가방 2번 칸에는 아이템이 없습니다.");

        Item sword = ItemFactory.createById("IRON_SWORD");
        player.getInventory().add(sword);
        player.equip(sword);
        expect("장착한 장비는 가방에 없어 팔 수 없음", ids(player.getInventory().getItems()), List.of("HP_POTION_S"));
    }

    private static void checkHeal() {
        Shop shop = Shop.open(2, MERCHANT);
        Player player = new Player("테스트", Job.WARRIOR);
        player.addGold(100);

        expect("HP 가 가득하면 치료 안 함", shop.heal(player), "HP 가 이미 가득합니다.");
        expect("HP 가 가득: 골드·사용 여부 그대로", List.of(player.getGold(), shop.isHealUsed()), List.of(100, false));

        int maxHp = player.getMaxHp();
        player.takeDamage(maxHp - 1);
        String healed = shop.heal(player);
        int expected = maxHp * Shop.HEAL_PERCENT / 100;
        expect("치료 메시지", healed, "치료를 받아 HP 를 " + expected + " 회복했습니다. (20골드)");
        expect("치료: HP", player.getStats().getHp(), 1 + expected);
        expect("치료: 골드", player.getGold(), 80);
        expect("치료: 사용함", shop.isHealUsed(), true);
        expect("두 번째 치료는 안 됨", shop.heal(player), "치료는 이 상점에서 이미 받았습니다.");
        expect("두 번째 치료: 골드 그대로", player.getGold(), 80);

        Player almostFull = new Player("거의", Job.WARRIOR);
        almostFull.addGold(20);
        almostFull.takeDamage(1);
        Shop other = Shop.open(2, MERCHANT);
        expect("치료한 만큼만 알려 줌", other.heal(almostFull), "치료를 받아 HP 를 1 회복했습니다. (20골드)");
        expect("최대 HP 를 넘지 않음", almostFull.getStats().getHp(), almostFull.getMaxHp());

        Shop deep = Shop.open(5, MERCHANT);
        Player poor = new Player("가난", Job.WARRIOR);
        poor.addGold(49);
        poor.takeDamage(5);
        expect("5층 치료 50골드: 모자람", deep.heal(poor), "골드가 모자랍니다. (치료 50골드, 보유 49골드)");
        expect("골드 부족: 사용 안 함", deep.isHealUsed(), false);
    }

    private static void checkRestore() {
        Shop shop = Shop.restore(4, MERCHANT, List.of(ItemFactory.createById("RUNE_BLADE")), true);
        expect("restore: 상품", ids(shop.getStock()), List.of("RUNE_BLADE"));
        expect("restore: 치료 여부", shop.isHealUsed(), true);
        expect("restore: 4층 치료 40골드", shop.getHealCost(), 40);
        expect("restore: 빈 진열 허용", Shop.restore(2, MERCHANT, List.of(), false).getStock().size(), 0);
        try {
            Shop.restore(2, MERCHANT, List.of(
                    ItemFactory.createById("HP_POTION_S"), ItemFactory.createById("HP_POTION_S"),
                    ItemFactory.createById("HP_POTION_S"), ItemFactory.createById("HP_POTION_S"),
                    ItemFactory.createById("HP_POTION_S"), ItemFactory.createById("HP_POTION_S")), false);
            fail("상품 6개는 예외", "예외 없음");
        } catch (IllegalArgumentException e) {
            passed++;
        }
    }

    /** 철 검(50), 작은 치유 물약(20)이 진열된 2층 상점. */
    private static Shop sampleShop() {
        return Shop.restore(2, MERCHANT,
                List.of(ItemFactory.createById("IRON_SWORD"), ItemFactory.createById("HP_POTION_S")), false);
    }

    private static List<String> ids(List<Item> items) {
        return items.stream().map(Item::getId).toList();
    }

    private static void fail(String name, Object actual) {
        failed++;
        System.out.println("[실패] " + name + ": " + actual);
    }

    private static void expect(String name, Object actual, Object expected) {
        if (Objects.equals(actual, expected)) {
            passed++;
        } else {
            failed++;
            System.out.println("[실패] " + name + ": 기대 " + expected + " / 실제 " + actual);
        }
    }
}
