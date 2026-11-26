package com.textrpg.game;

import java.util.List;

import com.textrpg.CheckRunner;
import com.textrpg.core.Position;
import com.textrpg.entity.Job;
import com.textrpg.entity.Player;
import com.textrpg.item.ItemFactory;
import com.textrpg.shop.Shop;
import com.textrpg.shop.TradeResult;

import static com.textrpg.CheckRunner.expect;

/**
 * ShopScreen.run (상점 명령 한 줄 해석) 확인용. JUnit 대신 main 으로 실행한다 (Team_Rules 2.11).
 * 사고팔기 규칙 자체는 ShopCheck 가 확인하고, 여기서는 입력이 어느 거래로 이어지는지만 본다.
 * 실패가 하나라도 있으면 종료 코드 1.
 */
public final class ShopScreenCheck {

    private static final String ERROR = "상점에서는 b 번호, s 번호, h 만 입력할 수 있습니다: ";

    private ShopScreenCheck() {
    }

    public static void main(String[] args) {
        checkBuy();
        checkSell();
        checkHeal();
        checkWrongInput();

        CheckRunner.finish();
    }

    private static void checkBuy() {
        Shop shop = sampleShop();
        Player player = playerWithGold(100);
        expect("b 1 → 1번 상품 구매", ShopScreen.run(shop, player, "b 1"),
                TradeResult.done("철 검을(를) 50골드에 샀습니다."));
        expect("구매 후 골드", player.getGold(), 50);

        shop = sampleShop();
        player = playerWithGold(100);
        expect("대문자·붙여 쓰기 B2", ShopScreen.run(shop, player, "B2"),
                TradeResult.done("작은 치유 물약을(를) 20골드에 샀습니다."));
        expect("앞뒤 공백은 지운다", ShopScreen.run(shop, player, "  b 1  "),
                TradeResult.done("철 검을(를) 50골드에 샀습니다."));
        expect("없는 상품 번호는 Shop 안내", ShopScreen.run(shop, player, "b 0"),
                TradeResult.rejected("0번 상품은 없습니다."));
    }

    private static void checkSell() {
        Shop shop = sampleShop();
        Player player = playerWithGold(0);
        player.getInventory().add(ItemFactory.createById("IRON_SWORD"));
        expect("s 1 → 가방 1번 판매", ShopScreen.run(shop, player, "s 1"),
                TradeResult.done("철 검을(를) 10골드에 팔았습니다."));
        expect("판매 후 골드", player.getGold(), 10);
        expect("없는 가방 칸은 Shop 안내", ShopScreen.run(shop, player, "S9"),
                TradeResult.rejected("가방 9번 칸에는 아이템이 없습니다."));
    }

    private static void checkHeal() {
        Player player = playerWithGold(100);
        expect("h → 치료 (HP 가득)", ShopScreen.run(sampleShop(), player, "h"),
                TradeResult.rejected("HP 가 이미 가득합니다."));
        expect("H 대문자", ShopScreen.run(sampleShop(), player, "H"),
                TradeResult.rejected("HP 가 이미 가득합니다."));
    }

    private static void checkWrongInput() {
        Shop shop = sampleShop();
        Player player = playerWithGold(100);
        for (String line : List.of("h1", "b", "s", "x 1", "b 1 2", "b 99999999999", "buy 1", "b -1")) {
            expect("오류: " + line, ShopScreen.run(shop, player, line), TradeResult.rejected(ERROR + line));
        }
        expect("오류에는 거래 없음: 골드", player.getGold(), 100);
        expect("오류에는 거래 없음: 진열", shop.getStock().size(), 2);
    }

    /** 2층 상점: 1) 철 검 50골드, 2) 작은 치유 물약 20골드. */
    private static Shop sampleShop() {
        return Shop.restore(2, new Position(10, 5),
                List.of(ItemFactory.createById("IRON_SWORD"), ItemFactory.createById("HP_POTION_S")), false);
    }

    private static Player playerWithGold(int gold) {
        Player player = new Player("테스트", Job.WARRIOR);
        player.addGold(gold);
        return player;
    }
}
