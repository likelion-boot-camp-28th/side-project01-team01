package com.textrpg.entity;

import com.textrpg.core.Position;
import com.textrpg.item.FloorItem;
import com.textrpg.item.Inventory;
import com.textrpg.item.Item;
import com.textrpg.item.ItemFactory;
import com.textrpg.item.ItemUser;
import com.textrpg.item.Weapon;
import com.textrpg.meta.Profile;
import com.textrpg.meta.RunResult;
import com.textrpg.meta.ShardCalculator;
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
        checkStatSum();
        checkLevelUp();
        checkRegenerate();
        checkEquipUse();
        checkUpgradeCost();
        checkProfileAndRun();
        checkShards();
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

        Item sword = ItemFactory.createById("IRON_SWORD");
        check("아이템 설명 있음", !sword.getDescription().isBlank());
        check("철 검 1~3층 드랍", sword.canDropOn(1) && sword.canDropOn(3) && !sword.canDropOn(4));
    }

    private static void checkStatSum() {
        Player p = new Player("용사", Job.WARRIOR);
        check("맨손 무기 공격 0", p.getWeaponAttack() == 0);
        check("공격 = 기본 + 무기", p.getAttack() == p.getBaseAttack() + p.getWeaponAttack());
        check("방어 = 기본 + 방어구", p.getDefense() == p.getBaseDefense() + p.getArmorDefense());
    }

    private static void checkLevelUp() {
        Player p = new Player("용사", Job.WARRIOR);
        check("경험치 10 은 레벨업 아님", !p.gainExp(10) && p.getLevel() == 1 && p.getExp() == 10);

        Player q = new Player("용사", Job.WARRIOR);
        check("경험치 딱 20 이면 레벨업", q.gainExp(20) && q.getLevel() == 2 && q.getExp() == 0);

        Player r = new Player("용사", Job.WARRIOR);
        r.gainExp(65);
        check("경험치 65 → 3레벨, 5 남음", r.getLevel() == 3 && r.getExp() == 5);
        check("2번 레벨업 → 최대 HP 30+12", r.getStats().getMaxHp() == 42);
        check("2번 레벨업 → 최대 MP 5+4", r.getStats().getMaxMp() == 9);
        check("2번 레벨업 → 공격 5+2, 방어 3+2", r.getBaseAttack() == 7 && r.getBaseDefense() == 5);
    }

    private static void checkRegenerate() {
        Player p = new Player("용사", Job.WARRIOR);
        p.takeDamage(5);
        p.regenerate(9);
        check("9턴째는 회복 안 함", p.getStats().getHp() == 25);
        p.regenerate(10);
        check("10턴째 HP 1 회복", p.getStats().getHp() == 26);
    }

    private static void checkEquipUse() {
        Weapon sword = (Weapon) ItemFactory.createById("IRON_SWORD");
        FakeUser user = new FakeUser();
        sword.use(user);
        check("무기 사용 = 장착 요청", user.equipped == sword);
        Item armor = ItemFactory.createById("LEATHER_ARMOR");
        armor.use(user);
        check("방어구 사용 = 장착 요청", user.equipped == armor);
    }

    /** equip() 이 아직 TODO 라서, 무기·방어구 use() 만 확인하려고 쓰는 가짜 사용자. */
    private static final class FakeUser implements ItemUser {
        private Item equipped;

        @Override
        public int heal(int amount) {
            return 0;
        }

        @Override
        public int restoreMp(int amount) {
            return 0;
        }

        @Override
        public String equip(Item item) {
            equipped = item;
            return "장착";
        }

        @Override
        public Inventory getInventory() {
            return new Inventory();
        }
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

        check("파편 0 이면 강화 못 삼", !profile.canBuy(UpgradeType.MAX_HP));

        Player p = new Player("용사", Job.WARRIOR);
        p.gainExp(20);
        p.addGold(15);
        run.finish(p, false, "고블린에게 사망");
        check("끝난 판 레벨 2, 골드 15 저장", run.getFinalLevel() == 2 && run.getGold() == 15);
        check("끝난 판 패배, 사망 원인 저장",
                !run.isVictory() && run.getDeathCause().equals("고블린에게 사망"));

        RunResult clear = new RunResult();
        clear.finish(p, true, "");
        check("클리어면 victory true", clear.isVictory());
    }

    private static void checkShards() {
        Player p = new Player("용사", Job.WARRIOR);
        p.gainExp(120);
        p.addGold(57);
        RunResult run = new RunResult();
        run.reachFloor(3);
        for (int i = 0; i < 14; i++) {
            run.addKill();
        }
        run.finish(p, false, "오크에게 사망");
        check("예시: 3층·처치 14·레벨 4·골드 57 → 파편 61",
                ShardCalculator.calculate(run, Profile.newProfile()) == 61);

        RunResult clear = new RunResult();
        clear.reachFloor(5);
        clear.finish(p, true, "");
        check("클리어 보너스 100: 5층·레벨 4·골드 57 → 50 + 12 + 5 + 100 = 167",
                ShardCalculator.calculate(clear, Profile.newProfile()) == 167);

        Profile profile = Profile.newProfile();
        profile.recordRun(run, 61);
        profile.recordRun(clear, 167);
        RunResult shallow = new RunResult();
        shallow.reachFloor(2);
        shallow.finish(p, false, "박쥐에게 사망");
        profile.recordRun(shallow, 0);
        check("파편 누적 61 + 167", profile.getShards() == 228);
        check("판 수 3, 처치 누적 14, 클리어 1",
                profile.getTotalRuns() == 3 && profile.getTotalKills() == 14 && profile.getClears() == 1);
        check("최고 층은 5 그대로 (2층 판이 덮어쓰지 않음)", profile.getBestFloor() == 5);
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
