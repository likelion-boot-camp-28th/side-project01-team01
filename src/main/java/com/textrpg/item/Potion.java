package com.textrpg.item;

/**
 * 물약. 마시면 HP·MP 를 회복하고 사라진다.
 */
public class Potion extends Item {

    private final int hpRestore;
    private final int mpRestore;

    public Potion(String id, String name, int hpRestore, int mpRestore) {
        super(id, name, ItemType.POTION);
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
        // TODO(B): user.heal(hpRestore), user.restoreMp(mpRestore), user.getInventory() 에서 제거 후 "HP 를 10 회복했다." 같은 문장 돌려주기
        throw new UnsupportedOperationException("TODO(B): Potion.use");
    }
}
