package com.textrpg.item;

import java.util.ArrayList;
import java.util.List;

/**
 * 물약. 마시면 HP·MP 를 회복하고 사라진다.
 */
public class Potion extends Item {

    private final int hpRestore;
    private final int mpRestore;

    Potion(ItemKind kind, int hpRestore, int mpRestore) {
        super(kind, ItemType.POTION);
        this.hpRestore = hpRestore;
        this.mpRestore = mpRestore;
    }

    public int getHpRestore() {
        return hpRestore;
    }

    public int getMpRestore() {
        return mpRestore;
    }

    /** 회복하는 것만 적는다. 예: "HP 10 회복", "HP 10 · MP 5 회복" */
    @Override
    protected String effectText() {
        List<String> parts = new ArrayList<>();
        if (hpRestore > 0) {
            parts.add("HP " + hpRestore);
        }
        if (mpRestore > 0) {
            parts.add("MP " + mpRestore);
        }
        return String.join(" · ", parts) + " 회복";
    }

    @Override
    public String use(ItemUser user) {
        int healed = user.heal(hpRestore);
        int restored = user.restoreMp(mpRestore);

        int index = user.getInventory().getItems().indexOf(this);
        if (index >= 0) {
            user.getInventory().remove(index);
        }

        return "HP 를 " + healed + " 회복했다. MP 를 " + restored + " 회복했다.";
    }
}
