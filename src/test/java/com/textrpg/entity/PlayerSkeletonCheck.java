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
        checkPotionUse();
        checkUpgradeCost();
        checkProfileAndRun();
        checkShards();
        checkBreakdown();
        checkBuy();
        checkApplyTo();
        checkFloorItem();
        checkCreateRandom();
        checkDropItem();
        checkUseItem();
        checkEquip();
        checkStartPotionAndMpPotion();
        checkRestore();
        checkRunModifiers();

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
        check("철 검 2~3층 드랍 (장비는 2층부터)", !sword.canDropOn(1) && sword.canDropOn(2) && sword.canDropOn(3)
                && !sword.canDropOn(4));
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

    private static void checkPotionUse() {
        Item potion = ItemFactory.createById("HP_POTION_S");
        Player p = new Player("용사", Job.WARRIOR);
        p.takeDamage(15);
        String message = potion.use(p);
        check("물약 최대 HP 30 의 25% = 8 회복 (15 -> 23)", p.getStats().getHp() == 23);
        check("물약 문장에 실제 회복량 8", message.contains("HP 를 8"));
        check("치유 물약 문장에 MP 없음", !message.contains("MP"));

        p.getStats().setHp(27);
        message = potion.use(p);
        check("최대 HP 를 넘지 않음 (27 -> 30)", p.getStats().getHp() == 30);
        check("문장에는 실제로 오른 3 만", message.contains("HP 를 3"));
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
        public int getMaxHp() {
            return 0;
        }

        @Override
        public int getMaxMp() {
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

    private static void checkBreakdown() {
        Player p = new Player("용사", Job.WARRIOR);
        p.gainExp(120);
        p.addGold(57);
        RunResult run = new RunResult();
        run.reachFloor(3);
        for (int i = 0; i < 14; i++) {
            run.addKill();
        }
        run.finish(p, false, "오크에게 사망");
        java.util.List<String> lines = ShardCalculator.breakdown(run, Profile.newProfile());
        check("내역 5줄 (층·처치·레벨·골드·합계)", lines.size() == 5);
        check("첫 줄: 도달 층 3 × 10 = 30", lines.get(0).equals("도달 층 3 × 10 = 30"));
        check("처치 줄 = 14", lines.get(1).startsWith("처치 14") && lines.get(1).endsWith("= 14"));
        check("레벨 줄 = 12", lines.get(2).startsWith("레벨 4") && lines.get(2).endsWith("= 12"));
        check("골드 줄 = 5", lines.get(3).startsWith("골드 57") && lines.get(3).endsWith("= 5"));
        check("마지막 줄: 합계 = 61", lines.get(4).equals("합계 = 61"));

        RunResult clear = new RunResult();
        clear.reachFloor(5);
        clear.finish(p, true, "");
        java.util.List<String> clearLines = ShardCalculator.breakdown(clear, Profile.newProfile());
        check("클리어하면 보너스 줄이 하나 더 (6줄)", clearLines.size() == 6);
        check("보너스 줄에 100", clearLines.get(4).contains("100"));
        check("클리어 합계 = 167", clearLines.get(5).equals("합계 = 167"));
    }

    private static void checkBuy() {
        Profile profile = Profile.newProfile();
        check("파편 0 이면 못 삼", !profile.buy(UpgradeType.MAX_HP) && profile.getLevel(UpgradeType.MAX_HP) == 0);

        profile.recordRun(new RunResult(), 80);
        check("파편 80 → 최대 HP 1단계 구매(20)", profile.buy(UpgradeType.MAX_HP));
        check("단계 1, 파편 60 남음", profile.getLevel(UpgradeType.MAX_HP) == 1 && profile.getShards() == 60);

        check("마법사 해금 구매(60)", profile.buy(UpgradeType.UNLOCK_MAGE));
        check("마법사 해금됨, 파편 0", profile.isUnlocked(Job.MAGE) && profile.getShards() == 0);
        check("도적은 아직 잠김", !profile.isUnlocked(Job.ROGUE));
        check("해금은 1단계가 최대라 또 못 삼", !profile.buy(UpgradeType.UNLOCK_MAGE));
    }

    private static void checkApplyTo() {
        Player plain = new Player("용사", Job.WARRIOR);
        Profile empty = Profile.newProfile();
        Player same = new Player("용사", Job.WARRIOR);
        empty.applyTo(same);
        check("강화 없으면 스탯 그대로",
                same.getStats().getMaxHp() == plain.getStats().getMaxHp()
                        && same.getStats().getAttack() == plain.getStats().getAttack()
                        && same.getStats().getDefense() == plain.getStats().getDefense());

        Profile profile = Profile.newProfile();
        profile.recordRun(new RunResult(), 120);
        profile.buy(UpgradeType.MAX_HP);
        profile.buy(UpgradeType.MAX_HP);
        profile.buy(UpgradeType.ATTACK);
        profile.buy(UpgradeType.DEFENSE);
        check("구매: HP 2단계, 공격 1, 방어 1 (20+40+30+30 = 120)", profile.getShards() == 0);

        Player p = new Player("용사", Job.WARRIOR);
        profile.applyTo(p);
        check("최대 HP +10 (30 -> 40)", p.getStats().getMaxHp() == 40);
        check("현재 HP 도 40 으로 꽉 참", p.getStats().getHp() == 40);
        check("공격 +1", p.getStats().getAttack() == plain.getStats().getAttack() + 1);
        check("방어 +1", p.getStats().getDefense() == plain.getStats().getDefense() + 1);
    }

    private static void checkFloorItem() {
        FloorItem goldPile = FloorItem.gold(30, new Position(1, 1));
        check("골드 더미 기호 $", goldPile.isGold() && goldPile.symbol() == '$');
        FloorItem sword = FloorItem.of(ItemFactory.createById("IRON_SWORD"), new Position(2, 2));
        check("무기 기호 )", !sword.isGold() && sword.symbol() == ')');
    }

    private static void checkCreateRandom() {
        com.textrpg.util.RandomUtil.setSeed(42);
        boolean allFitFloor = true;
        java.util.Set<String> seenOnFloor1 = new java.util.HashSet<>();
        java.util.Set<String> seenOnFloor5 = new java.util.HashSet<>();
        for (int i = 0; i < 200; i++) {
            for (int floor = 1; floor <= 5; floor++) {
                Item item = ItemFactory.createRandom(floor);
                if (!item.canDropOn(floor)) {
                    allFitFloor = false;
                }
                if (floor == 1) {
                    seenOnFloor1.add(item.getId());
                }
                if (floor == 5) {
                    seenOnFloor5.add(item.getId());
                }
            }
        }
        check("무작위 아이템은 항상 그 층에서 나올 수 있는 것", allFitFloor);
        check("1층에서는 작은 물약 2종과 가죽 갑옷만 (나머지 장비는 2층부터)",
                seenOnFloor1.equals(java.util.Set.of("HP_POTION_S", "MP_POTION_S", "LEATHER_ARMOR")));
        check("5층에는 2~3층 장비(철 검·가죽 갑옷)가 없음",
                !seenOnFloor5.contains("IRON_SWORD") && !seenOnFloor5.contains("LEATHER_ARMOR"));
        Item a = ItemFactory.createRandom(1);
        Item b = ItemFactory.createRandom(1);
        check("부를 때마다 새 아이템 객체", a != b);
    }


    private static void checkDropItem() {
        Player p = new Player("용사", Job.WARRIOR);
        Item sword = ItemFactory.createById("IRON_SWORD");
        Item potion = ItemFactory.createById("HP_POTION_S");
        p.getInventory().add(sword);
        p.getInventory().add(potion);

        Item dropped = p.dropItem(0);
        check("버린 아이템을 돌려줌 (0번 = 철 검)", dropped == sword);
        check("가방에서 빠짐 (2개 -> 1개)", p.getInventory().size() == 1);
        check("남은 아이템이 0번으로 당겨짐", p.getInventory().get(0) == potion);
    }


    private static void checkUseItem() {
        Player p = new Player("용사", Job.WARRIOR);
        p.getInventory().add(ItemFactory.createById("HP_POTION_S"));
        p.takeDamage(15);
        String message = p.useItem(0);
        check("가방 0번 물약 사용 -> HP 15 -> 23 (최대 30 의 25%)", p.getStats().getHp() == 23);
        check("물약의 결과 문장을 그대로 돌려줌", message.contains("HP 를 8"));
        check("마신 물약은 가방에서 사라짐", p.getInventory().size() == 0);

        Item sword = ItemFactory.createById("IRON_SWORD");
        Item potion1 = ItemFactory.createById("HP_POTION_S");
        Item potion2 = ItemFactory.createById("HP_POTION_S");
        p.getInventory().add(sword);
        p.getInventory().add(potion1);
        p.getInventory().add(potion2);
        p.useItem(2);
        check("같은 물약 2개 중 마신 것(2번)만 빠짐",
                p.getInventory().size() == 2 && p.getInventory().get(1) == potion1);
        check("다른 아이템(철 검)은 그대로", p.getInventory().get(0) == sword);
    }


    private static void checkEquip() {
        Player p = new Player("용사", Job.WARRIOR);
        int bareAttack = p.getAttack();
        Item sword1 = ItemFactory.createById("IRON_SWORD");
        Item potion = ItemFactory.createById("HP_POTION_S");
        p.getInventory().add(sword1);
        p.getInventory().add(potion);

        String message = p.useItem(0);
        check("맨손에서 검 장착 -> 장착 문장", message.contains("장착했다"));
        check("무기 칸에 검", p.getWeapon().isPresent() && p.getWeapon().get() == sword1);
        check("공격력 +3", p.getAttack() == bareAttack + 3);
        check("검은 가방에서 빠짐 (물약만 남음)", p.getInventory().size() == 1 && p.getInventory().get(0) == potion);

        Item sword2 = ItemFactory.createById("IRON_SWORD");
        p.getInventory().add(sword2);
        p.useItem(1);
        check("검 교체: 새 검이 무기 칸에", p.getWeapon().get() == sword2);
        check("원래 검은 가방으로 돌아옴", p.getInventory().getItems().contains(sword1)
                && !p.getInventory().getItems().contains(sword2) && p.getInventory().size() == 2);

        Item armor = ItemFactory.createById("LEATHER_ARMOR");
        p.getInventory().add(armor);
        int defenseBefore = p.getDefense();
        p.useItem(2);
        check("갑옷 장착 -> 방어력 +2", p.getArmor().isPresent() && p.getDefense() == defenseBefore + 2);
        check("무기는 그대로", p.getWeapon().get() == sword2);

        String refuse = p.equip(potion);
        check("물약은 장착할 수 없음", refuse.contains("장착할 수 없다"));
    }


    private static void checkStartPotionAndMpPotion() {
        Player none = new Player("용사", Job.WARRIOR);
        Profile.newProfile().applyTo(none);
        check("보급품 강화 없으면 시작 물약 0개", none.getInventory().size() == 0);

        Profile profile = Profile.newProfile();
        profile.recordRun(new RunResult(), 75);
        profile.buy(UpgradeType.START_POTION);
        profile.buy(UpgradeType.START_POTION);
        Player p = new Player("용사", Job.WARRIOR);
        profile.applyTo(p);
        check("보급품 2단계 -> 시작 물약 2개", p.getInventory().size() == 2
                && p.getInventory().get(0).getId().equals("HP_POTION_S")
                && p.getInventory().get(1).getId().equals("HP_POTION_S"));
        check("시작 물약은 서로 다른 객체", p.getInventory().get(0) != p.getInventory().get(1));

        Player mage = new Player("마법사", Job.MAGE);
        mage.getStats().setMp(0);
        mage.getInventory().add(ItemFactory.createById("MP_POTION_S"));
        String message = mage.useItem(0);
        check("마나 물약: 최대 MP 20 의 30% = 6 (0 -> 6)", mage.getStats().getMp() == 6);
        check("마나 물약 문장에 MP 6", message.contains("MP 를 6"));
        check("마신 마나 물약은 가방에서 사라짐", mage.getInventory().size() == 0);
    }


    private static void checkRestore() {
        Item sword = ItemFactory.createById("IRON_SWORD");
        Item armor = ItemFactory.createById("LEATHER_ARMOR");
        java.util.List<Item> bag = java.util.List.of(
                ItemFactory.createById("HP_POTION_S"), ItemFactory.createById("MP_POTION_S"));
        Player p = Player.restore("용사", Job.WARRIOR, 3, 15, 42, 20, 45, 4, 9, 7, 5,
                new Position(10, 5), sword, armor, bag);
        check("복원: 이름·직업·레벨·경험치·골드", p.getName().equals("용사") && p.getJob() == Job.WARRIOR
                && p.getLevel() == 3 && p.getExp() == 15 && p.getGold() == 42);
        check("복원: HP 20/45, MP 4/9", p.getStats().getHp() == 20 && p.getStats().getMaxHp() == 45
                && p.getStats().getMp() == 4 && p.getStats().getMaxMp() == 9);
        check("복원: 기본 공격 7·방어 5", p.getBaseAttack() == 7 && p.getBaseDefense() == 5);
        check("복원: 장비 보너스 더해짐 (공격 7+3, 방어 5+2)", p.getAttack() == 10 && p.getDefense() == 7);
        check("복원: 위치 (10,5)", p.getPos().equals(new Position(10, 5)));
        check("복원: 가방 2개 순서 그대로", p.getInventory().size() == 2
                && p.getInventory().get(0).getId().equals("HP_POTION_S")
                && p.getInventory().get(1).getId().equals("MP_POTION_S"));

        Player bare = Player.restore("맨손", Job.MAGE, 1, 0, 0, 22, 22, 20, 20, 4, 1,
                new Position(1, 1), null, null, java.util.List.of());
        check("복원: 장비 없으면 맨손", bare.getWeapon().isEmpty() && bare.getArmor().isEmpty());

        java.util.List<Item> big = new java.util.ArrayList<>();
        for (int i = 0; i < Inventory.CAPACITY + 2; i++) {
            big.add(ItemFactory.createById("HP_POTION_S"));
        }
        Player loaded = Player.restore("큰가방", Job.WARRIOR, 1, 0, 0, 30, 30, 5, 5, 5, 3,
                new Position(1, 1), null, null, big);
        check("복원: 큰 가방(14개)도 전부 들어감", loaded.getInventory().size() == Inventory.CAPACITY + 2);

        check("복원 거절: 무기 칸에 물약", throwsIllegal(() -> Player.restore("x", Job.WARRIOR, 1, 0, 0, 30, 30, 5, 5, 5, 3,
                new Position(1, 1), ItemFactory.createById("HP_POTION_S"), null, java.util.List.of())));
        check("복원 거절: hp 가 maxHp 보다 큼", throwsIllegal(() -> Player.restore("x", Job.WARRIOR, 1, 0, 0, 31, 30, 5, 5, 5, 3,
                new Position(1, 1), null, null, java.util.List.of())));
        check("복원 거절: 레벨 1 인데 exp 20 (레벨업 했어야 함)", throwsIllegal(() -> Player.restore("x", Job.WARRIOR, 1, 20, 0, 30, 30, 5, 5, 5, 3,
                new Position(1, 1), null, null, java.util.List.of())));
    }

    private static void checkRunModifiers() {
        Profile profile = Profile.newProfile();
        profile.recordRun(new RunResult(), 40 + 35);
        profile.buy(UpgradeType.BAG_SIZE);
        profile.buy(UpgradeType.EXP_BOOST);
        Player p = new Player("용사", Job.WARRIOR);
        profile.applyTo(p);
        check("큰 가방 1단계 -> 가방 12 + 2 = 14칸", p.getInventory().getCapacity() == Inventory.CAPACITY + 2);
        p.gainExp(10);
        check("배움의 축복 1단계 -> 경험치 10 이 11 로", p.getExp() == 11);

        profile.applyRunModifiers(p);
        profile.applyRunModifiers(p);
        check("여러 번 불러도 14칸 그대로 (이어하기 안전)", p.getInventory().getCapacity() == Inventory.CAPACITY + 2);

        Player plain = new Player("용사", Job.WARRIOR);
        Profile.newProfile().applyTo(plain);
        check("강화 없으면 12칸, 배율 1.0", plain.getInventory().getCapacity() == Inventory.CAPACITY);
        plain.gainExp(10);
        check("강화 없으면 경험치 그대로 10", plain.getExp() == 10);
    }

    private static boolean throwsIllegal(Runnable action) {
        try {
            action.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
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
