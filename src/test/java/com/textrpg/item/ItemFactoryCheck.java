package com.textrpg.item;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * ItemKind·ItemFactory 확인용: id 로 만들기, 설명의 수치, 층별 무작위 생성, 세이브 id 유지.
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
        expect("치유 물약 설명", ItemFactory.createById("HP_POTION_S").getDescription(), "마시면 상처가 아문다. HP 10 회복");
        expect("마나 물약 설명", ItemFactory.createById("MP_POTION_S").getDescription(), "마시면 정신이 맑아진다. MP 5 회복");
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
        expectThrows("나올 아이템이 없는 층", () -> ItemFactory.createRandom(99));
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
