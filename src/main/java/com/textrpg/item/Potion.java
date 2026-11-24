package com.textrpg.item;

/**
 * 물약. 마시면 HP·MP 를 회복하고 사라진다.
 */
public class Potion extends Item {

    private final int hpRestore;
    private final int mpRestore;

    public Potion(String id, String name, String description, int minFloor, int maxFloor,
                  int hpRestore, int mpRestore) {
        super(id, name, ItemType.POTION, description, minFloor, maxFloor);
        this.hpRestore = hpRestore;
        this.mpRestore = mpRestore;
    }

    public int getHpRestore() {
        return hpRestore;
    }

    public int getMpRestore() {
        return mpRestore;
    }

    @Override
    public String use(ItemUser user) {
        int healed = user.heal(hpRestore);
        int restored = user.restoreMp(mpRestore);
        // TODO(B): 가방 작업 때 가방에서 제거
        return "HP 를 " + healed + " 회복했다. MP 를 " + restored + " 회복했다.";
    }
}
