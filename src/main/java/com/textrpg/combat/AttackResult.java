package com.textrpg.combat;

/**
 * 공격 한 번의 결과.
 *
 * @param hit      명중했으면 true
 * @param critical 치명타였으면 true (빗나갔으면 항상 false)
 * @param damage   실제로 준 데미지 (빗나갔으면 0)
 * @param killed   이 공격으로 대상이 죽었으면 true
 */
public record AttackResult(boolean hit, boolean critical, int damage, boolean killed) {
}
