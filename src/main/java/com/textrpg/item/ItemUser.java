package com.textrpg.item;

/**
 * 아이템을 쓰는 쪽. Item.use() 는 Player 대신 이 인터페이스를 받는다.
 *
 * <p>Item 이 Player 를 직접 알면 Player → Inventory → Item → Player 로 패키지가 서로를 참조하게 된다.
 * 그래서 아이템에 필요한 기능만 이 인터페이스로 열어 두고, Player 가 구현한다.
 * 새 아이템(P1)에 기능이 더 필요하면 여기에 메서드를 추가한다.
 */
public interface ItemUser {

    /**
     * HP 를 회복한다.
     *
     * @param amount 회복량
     * @return 실제로 회복된 양
     */
    int heal(int amount);

    /**
     * MP 를 회복한다.
     *
     * @param amount 회복량
     * @return 실제로 회복된 양
     */
    int restoreMp(int amount);

    /**
     * 최대 HP. 최대치의 비율로 회복하는 물약이 회복량을 계산할 때 쓴다.
     *
     * @return 최대 HP
     */
    int getMaxHp();

    /**
     * 최대 MP. 최대치의 비율로 회복하는 물약이 회복량을 계산할 때 쓴다.
     *
     * @return 최대 MP
     */
    int getMaxMp();

    /**
     * 무기나 방어구를 장착한다.
     *
     * @param item 장착할 아이템
     * @return 결과 문장
     */
    String equip(Item item);

    /**
     * 가방. 쓰고 나면 사라지는 아이템(물약)이 자신을 빼낼 때 쓴다.
     *
     * @return 가방
     */
    Inventory getInventory();
}
