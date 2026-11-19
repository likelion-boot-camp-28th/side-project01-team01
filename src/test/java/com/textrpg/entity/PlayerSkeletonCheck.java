package com.textrpg.entity;

import com.textrpg.item.FloorItem;
import com.textrpg.item.Inventory;
import com.textrpg.item.ItemFactory;
import com.textrpg.item.Weapon;
import com.textrpg.meta.Profile;
import com.textrpg.meta.RunResult;
import com.textrpg.meta.UpgradeType;

/**
 * B 파트 스켈레톤 중 이미 구현된 부분 확인용. JUnit 대신 main 으로 실행한다 (Team_Rules 2.11).
 * 실패가 하나라도 있으면 종료 코드 1.
 */
public final class PlayerSkeletonCheck {

    private static int passed;
    private static int failed;

    private PlayerSkeletonCheck() {
    }

    public static void main(String[] args) {
        checkNewPlayer();
        checkInventory();
        checkUpgradeCost();
        checkProfileAndRun();
        checkFloorItem();

        System.out.println("통과 " + passed + " / 실패 " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void checkNewPlayer() {
        Player p = new Player("용사", Job.WARRIOR);
        check("전사 시작 HP 30", p.getStats().getHp() == 30);
        check("시작 레벨 1", p.getLevel() == 1);
        check("기호 @", p.getSymbol() == '@');
        check("맨손 공격 = 기본 공격", p.getAttack() == p.getStats().getAttack());
        check("장비 없음", p.getWeapon().isEmpty() && p.getArmor().isEmpty());
        p.addGold(15);
        check("골드 증가", p.getGold() == 15);
    }

    private static void checkInventory() {
        Inventory bag = new Inventory();
        for (int i = 0; i < Inventory.CAPACITY; i++) {
            bag.add(ItemFactory.createById("HP_POTION_S"));
        }
        check("12칸 가득 참", bag.isFull());
        check("가득 차면 못 넣음", !bag.add(ItemFactory.createById("HP_POTION_S")));
        check("id 로 무기 생성", ItemFactory.createById("IRON_SWORD") instanceof Weapon);
    }

    private static void checkUpgradeCost() {
        check("MAX_HP 1단계 20", UpgradeType.MAX_HP.costFor(1) == 20);
        check("MAX_HP 최대 5단계", UpgradeType.MAX_HP.getMaxLevel() == 5);
        check("보급품 2단계 50", UpgradeType.START_POTION.costFor(2) == 50);
    }

    private static void checkProfileAndRun() {
        Profile profile = Profile.newProfile();
        check("처음엔 전사만 해금", profile.isUnlocked(Job.WARRIOR) && !profile.isUnlocked(Job.MAGE));
        check("강화 0단계", profile.getLevel(UpgradeType.ATTACK) == 0);

        RunResult run = new RunResult();
        run.addKill();
        run.reachFloor(3);
        run.reachFloor(2);
        check("처치 1", run.getKills() == 1);
        check("가장 깊은 층 3", run.getDeepestFloor() == 3);
    }

    private static void checkFloorItem() {
        FloorItem goldPile = FloorItem.gold(30, new Position(1, 1));
        check("골드 더미 기호 $", goldPile.isGold() && goldPile.symbol() == '$');
        FloorItem sword = FloorItem.of(ItemFactory.createById("IRON_SWORD"), new Position(2, 2));
        check("무기 기호 )", !sword.isGold() && sword.symbol() == ')');
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("실패: " + name);
        }
    }
}
