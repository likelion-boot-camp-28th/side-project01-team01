package com.textrpg.game;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.textrpg.core.MessageLog;
import com.textrpg.entity.Player;
import com.textrpg.shop.Shop;
import com.textrpg.shop.TradeResult;
import com.textrpg.ui.ConsoleView;
import com.textrpg.ui.InputHandler;

/**
 * 상점 화면 (t). 진열 상품과 가방을 보여 주고, Enter 를 누를 때까지 b 번호(구매), s 번호(판매), h(치료)를 반복해서 받는다.
 * 상점에서 하는 일은 턴을 쓰지 않는다. 사고팔기 규칙은 Shop 이 정하고, 이 클래스는 화면과 입력만 맡는다.
 */
final class ShopScreen {

    private static final String PROMPT = "명령 (b 번호: 구매, s 번호: 판매, h: 치료, Enter: 나가기): ";
    /** 상점 명령: "b 2", "s3", "h". 대소문자를 구분하지 않는다. */
    private static final Pattern COMMAND = Pattern.compile("([bsh])\\s*(\\d*)", Pattern.CASE_INSENSITIVE);

    private final ConsoleView view;
    private final InputHandler input;

    ShopScreen(ConsoleView view, InputHandler input) {
        this.view = view;
        this.input = input;
    }

    /**
     * 상점 화면을 연다. 결과 문장은 화면 아래에 보여 주고, 이루어진 거래는 탐험 로그에도 남긴다.
     *
     * @param log 거래 결과를 남길 탐험 로그
     */
    void open(Shop shop, Player player, MessageLog log) {
        String message = "";
        while (true) {
            view.clearScreen();
            view.printShop(shop, player);
            if (!message.isEmpty()) {
                view.printLine(message);
            }
            String line = input.readLine(PROMPT);
            if (line.isEmpty()) {
                return;
            }
            TradeResult result = run(shop, player, line);
            if (result.success()) {
                log.add(result.message());
            }
            message = result.message();
        }
    }

    /**
     * 상점 명령 한 줄을 실행하고 결과를 돌려준다. 번호는 화면 기준 1부터 받는다.
     * 입력 형식이 틀리면 거래 없이 rejected. 콘솔 입력 없이 확인할 수 있도록 static 으로 둔다 (ShopScreenCheck).
     */
    static TradeResult run(Shop shop, Player player, String line) {
        Matcher m = COMMAND.matcher(line.strip());
        TradeResult error = TradeResult.rejected("상점에서는 b 번호, s 번호, h 만 입력할 수 있습니다: " + line);
        if (!m.matches()) {
            return error;
        }
        char key = Character.toLowerCase(m.group(1).charAt(0));
        String number = m.group(2);
        if (key == 'h') {
            return number.isEmpty() ? shop.heal(player) : error;
        }
        int index;
        try {
            index = Integer.parseInt(number) - 1;
        } catch (NumberFormatException e) {
            // 번호가 없거나 int 범위를 넘을 때
            return error;
        }
        return key == 'b' ? shop.buy(player, index) : shop.sell(player, index);
    }
}
