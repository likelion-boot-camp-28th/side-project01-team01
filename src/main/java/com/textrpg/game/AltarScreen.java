package com.textrpg.game;

import com.textrpg.meta.Profile;
import com.textrpg.meta.UpgradeType;
import com.textrpg.ui.ConsoleView;
import com.textrpg.ui.InputHandler;

/**
 * 어둠의 제단: 보유 파편으로 영구 강화를 산다. 0 을 입력하면 돌아간다.
 * 저장은 하지 않는다. 언제 저장할지는 GameManager 가 정한다 (onPurchase).
 */
final class AltarScreen {

    private final ConsoleView view;
    private final InputHandler input;

    AltarScreen(ConsoleView view, InputHandler input) {
        this.view = view;
        this.input = input;
    }

    /**
     * 강화 번호를 0 이 나올 때까지 입력받는다.
     *
     * @param profile    강화를 살 프로필
     * @param onPurchase 강화를 하나 살 때마다 부른다. GameManager 가 이때 바로 저장한다 (기획서 2.7 제단 구매 저장)
     */
    void open(Profile profile, Runnable onPurchase) {
        view.clearScreen();
        UpgradeType[] types = UpgradeType.values();
        while (true) {
            view.printAltar(profile);
            int choice = input.readInt("강화 번호 (0: 돌아가기): ", 0, types.length);
            if (choice == 0) {
                return;
            }
            UpgradeType type = types[choice - 1];
            if (profile.buy(type)) {
                onPurchase.run();
                view.printLine(type.getDisplayName() + " " + profile.getLevel(type) + "단계를 얻었습니다.");
            } else if (profile.getLevel(type) >= type.getMaxLevel()) {
                view.printLine(type.getDisplayName() + " 은(는) 이미 최대 단계입니다.");
            } else {
                view.printLine("파편이 부족합니다.");
            }
        }
    }
}
