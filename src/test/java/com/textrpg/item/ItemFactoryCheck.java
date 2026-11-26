package com.textrpg.item;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * ItemKind·ItemFactory 확인용: id 로 만들기, 설명의 수치, 층별 무작위 생성, 장비 성장, 물약 비율 회복, 세이브 id 유지.
 * JUnit 대신 main 으로 실행한다 (Team_Rules 2.11). 실패가 하나라도 있으면 종료 코드 1.
 */
public final class ItemFactoryCheck {

    private static final int RANDOM_TRIES = 300;

    private static int passed;
    private static int failed;

    private ItemFactoryCheck() {
    }

    public static void main(String[] args) {
        checkCreateById();
        checkDescription();
        checkCreateRandom();
        checkProgression();
        checkPotion();
        checkSaveIds();

        System.out.println("통과 " + passed + " / 실패 " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void checkCreateById() {
        for (ItemKind kind : ItemKind.values()) {
            Item item = ItemFactory.createById(kind.name());
            expect(kind + " id", item.getId(), kind.name());
            expect(kind + " 종류", item.getKind(), kind);
            expect(kind + " 이름", item.getDisplayName(), kind.getDisplayName());
            expect(kind + " 기호는 분류를 따름", item.getSymbol(), item.getType().getSymbol());
            expect(kind + " 부를 때마다 새 아이템", item != ItemFactory.createById(kind.name()), true);
        }
        expect("철 검은 무기", ItemFactory.createById("IRON_SWORD") instanceof Weapon, true);
        expect("가죽 갑옷은 방어구", ItemFactory.createById("LEATHER_ARMOR") instanceof Armor, true);
        expect("철 검 공격력", ((Weapon) ItemFactory.createById("IRON_SWORD")).getAttackBonus(), 3);
        expectThrows("모르는 id", () -> ItemFactory.createById("MAGIC_RING"));
        expectThrows("소문자 id", () -> ItemFactory.createById("iron_sword"));
        expectThrows("null id", () -> ItemFactory.createById(null));
    }

    private static void checkDescription() {
        expect("무기 설명", ItemFactory.createById("IRON_SWORD").getDescription(), "평범한 철로 만든 검. 공격력 +3");
        expect("방어구 설명", ItemFactory.createById("LEATHER_ARMOR").getDescription(), "질긴 가죽으로 만든 갑옷. 방어력 +2");
        expect("치유 물약 설명", ItemFactory.createById("HP_POTION_S").getDescription(), "마시면 상처가 아문다. HP 25% 회복");
        expect("마나 물약 설명", ItemFactory.createById("MP_POTION_S").getDescription(), "마시면 정신이 맑아진다. MP 30% 회복");
    }

    private static void checkCreateRandom() {
        for (int floor = 1; floor <= 5; floor++) {
            boolean allDroppable = true;
            for (int i = 0; i < RANDOM_TRIES; i++) {
                allDroppable &= ItemFactory.createRandom(floor).canDropOn(floor);
            }
            expect(floor + "층 무작위 아이템은 그 층에서 나오는 것만", allDroppable, true);
        }
        boolean swordOnFloor4 = false;
        for (int i = 0; i < RANDOM_TRIES; i++) {
            swordOnFloor4 |= ItemFactory.createRandom(4).getKind() == ItemKind.IRON_SWORD;
        }
        expect("철 검은 4층에서 안 나옴", swordOnFloor4, false);
        expect("가죽 갑옷은 1층부터", ItemKind.LEATHER_ARMOR.canDropOn(1), true);
        for (ItemKind kind : ItemKind.values()) {
            Item item = ItemFactory.createById(kind.name());
            // 가죽 갑옷만 1층부터 나온다 (맨몸으로 2층에 들어가지 않게)
            if (item.getType() != ItemType.POTION && kind != ItemKind.LEATHER_ARMOR) {
                expect(kind + " 장비는 2층부터", kind.getMinFloor() >= 2, true);
            }
        }
        expectThrows("나올 아이템이 없는 층", () -> ItemFactory.createRandom(99));
    }

    /** 같은 분류의 장비는 나오기 시작하는 층이 깊을수록 수치가 높다. */
    private static void checkProgression() {
        List<ItemKind> kinds = Arrays.stream(ItemKind.values())
                .sorted(Comparator.comparingInt(ItemKind::getMinFloor))
                .toList();
        int lastAttack = 0;
        int lastDefense = 0;
        boolean weaponsGrow = true;
        boolean armorsGrow = true;
        for (ItemKind kind : kinds) {
            Item item = ItemFactory.createById(kind.name());
            if (item instanceof Weapon weapon) {
                weaponsGrow &= weapon.getAttackBonus() > lastAttack;
                lastAttack = weapon.getAttackBonus();
            } else if (item instanceof Armor armor) {
                armorsGrow &= armor.getDefenseBonus() > lastDefense;
                lastDefense = armor.getDefenseBonus();
            }
        }
        expect("깊은 층 무기일수록 공격력이 높음", weaponsGrow, true);
        expect("깊은 층 방어구일수록 방어력이 높음", armorsGrow, true);
    }

    /** 물약은 최대 HP·MP 의 비율만큼 회복한다. 마나 물약은 같은 크기의 치유 물약보다 비율이 높다. */
    private static void checkPotion() {
        expect("작은 치유 물약 25%", potion("HP_POTION_S").getHpPercent(), 25);
        expect("작은 마나 물약은 치유보다 조금 더",
                potion("MP_POTION_S").getMpPercent() > potion("HP_POTION_S").getHpPercent(), true);
        expect("큰 마나 물약은 큰 치유보다 조금 더",
                potion("MP_POTION_L").getMpPercent() > potion("HP_POTION_L").getHpPercent(), true);

        TestUser user = new TestUser(30, 20);
        String message = potion("HP_POTION_S").use(user);
        expect("최대 HP 30 의 25% = 8 (7.5 반올림)", user.healed, 8);
        expect("치유 물약 문장", message, "HP 를 8 회복했습니다.");
        potion("MP_POTION_S").use(user);
        expect("최대 MP 20 의 30% = 6", user.restored, 6);

        TestUser tiny = new TestUser(2, 0);
        potion("HP_POTION_S").use(tiny);
        expect("비율이 있으면 최소 1 회복", tiny.healed, 1);
    }

    private static Potion potion(String id) {
        return (Potion) ItemFactory.createById(id);
    }

    /** 받은 회복량만 기록하는 아이템 사용자. 실제 HP 상한은 따지지 않는다. */
    private static final class TestUser implements ItemUser {
        private final int maxHp;
        private final int maxMp;
        private final Inventory inventory = new Inventory();
        private int healed;
        private int restored;

        TestUser(int maxHp, int maxMp) {
            this.maxHp = maxHp;
            this.maxMp = maxMp;
        }

        @Override
        public int heal(int amount) {
            healed = amount;
            return amount;
        }

        @Override
        public int restoreMp(int amount) {
            restored = amount;
            return amount;
        }

        @Override
        public int getMaxHp() {
            return maxHp;
        }

        @Override
        public int getMaxMp() {
            return maxMp;
        }

        @Override
        public String equip(Item item) {
            return "";
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    /** 상수 이름이 세이브 파일의 id 다. 바꾸면 예전 세이브를 읽지 못한다. */
    private static void checkSaveIds() {
        List<String> ids = Arrays.stream(ItemKind.values()).map(Enum::name).toList();
        expect("세이브 id 유지", ids.containsAll(List.of("IRON_SWORD", "LEATHER_ARMOR", "HP_POTION_S", "MP_POTION_S")), true);
    }

    private static void expect(String name, Object actual, Object expected) {
        if (Objects.equals(actual, expected)) {
            passed++;
        } else {
            failed++;
            System.out.println("[실패] " + name + ": 기대 " + expected + " / 실제 " + actual);
        }
    }

    private static void expectThrows(String name, Runnable action) {
        try {
            action.run();
            failed++;
            System.out.println("[실패] " + name + ": IllegalArgumentException 이 나지 않음");
        } catch (IllegalArgumentException e) {
            passed++;
        }
    }
}
