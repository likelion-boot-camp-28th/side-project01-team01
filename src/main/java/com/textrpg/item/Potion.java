package com.textrpg.item;

import java.util.ArrayList;
import java.util.List;

/**
 * 물약. 마시면 최대 HP·MP 의 일정 비율(%)만큼 회복하고 사라진다.
 * 회복량은 반올림하고, 회복 비율이 있으면 최소 1 은 회복한다.
 */
public class Potion extends Item {

    private static final double PERCENT = 100.0;

    /** 최대 HP 의 몇 % 를 회복하는지. */
    private final int hpPercent;
    /** 최대 MP 의 몇 % 를 회복하는지. */
    private final int mpPercent;

    Potion(ItemKind kind, int hpPercent, int mpPercent) {
        super(kind, ItemType.POTION);
        this.hpPercent = hpPercent;
        this.mpPercent = mpPercent;
    }

    public int getHpPercent() {
        return hpPercent;
    }

    public int getMpPercent() {
        return mpPercent;
    }

    /** 회복하는 것만 적는다. 예: "HP 25% 회복", "HP 30% · MP 30% 회복" */
    @Override
    protected String effectText() {
        List<String> parts = new ArrayList<>();
        if (hpPercent > 0) {
            parts.add("HP " + hpPercent + "%");
        }
        if (mpPercent > 0) {
            parts.add("MP " + mpPercent + "%");
        }
        return String.join(" · ", parts) + " 회복";
    }

    /** 실제로 회복한 것만 문장에 넣는다. 예: "HP 를 8 회복했습니다." */
    @Override
    public String use(ItemUser user) {
        int healed = user.heal(amountOf(user.getMaxHp(), hpPercent));
        int restored = user.restoreMp(amountOf(user.getMaxMp(), mpPercent));

        int index = user.getInventory().getItems().indexOf(this);
        if (index >= 0) {
            user.getInventory().remove(index);
        }

        List<String> parts = new ArrayList<>();
        if (hpPercent > 0) {
            parts.add("HP 를 " + healed);
        }
        if (mpPercent > 0) {
            parts.add("MP 를 " + restored);
        }
        return String.join(", ", parts) + " 회복했습니다.";
    }

    /** max 의 percent% 를 반올림한 회복량. percent 가 0 이면 0, 아니면 최소 1. */
    private static int amountOf(int max, int percent) {
        if (percent <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.round(max * percent / PERCENT));
    }
}
