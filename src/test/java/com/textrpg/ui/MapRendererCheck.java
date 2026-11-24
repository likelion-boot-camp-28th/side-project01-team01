package com.textrpg.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.textrpg.core.MessageLog;
import com.textrpg.core.Position;
import com.textrpg.entity.Job;
import com.textrpg.entity.Player;
import com.textrpg.item.FloorItem;
import com.textrpg.map.DungeonGenerator;
import com.textrpg.map.Floor;
import com.textrpg.map.Tile;
import com.textrpg.meta.RunResult;
import com.textrpg.monster.Monster;
import com.textrpg.monster.MonsterFactory;
import com.textrpg.monster.MonsterType;

/**
 * MapRenderer 와 ConsoleView 의 문자열 생성(formatHeader, formatStatusBar, 결과 화면, 기호 안내, 메시지 줄바꿈) 확인용.
 * JUnit 대신 main 으로 실행한다 (Team_Rules 2.11). 실패가 하나라도 있으면 종료 코드 1.
 */
public final class MapRendererCheck {

    private static int passed;
    private static int failed;

    private MapRendererCheck() {
    }

    public static void main(String[] args) {
        checkSize();
        checkTiles();
        checkPlayer();
        checkMonsterAndItem();
        checkPriority();
        checkStatusBar();
        checkHeader();
        checkRunResult();
        checkLegend();
        checkMessages();

        System.out.println("통과 " + passed + " / 실패 " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void checkSize() {
        Floor floor = DungeonGenerator.createTestFloor();
        List<String> lines = new MapRenderer().render(floor, playerAtStart(floor));

        expect("줄 수", lines.size(), Floor.HEIGHT);
        for (int y = 0; y < lines.size(); y++) {
            expect(y + "번 줄 길이", lines.get(y).length(), Floor.WIDTH);
        }
        expectThrows("돌려준 목록은 수정 불가", UnsupportedOperationException.class, () -> lines.add("x"));
    }

    /** 아무것도 없는 칸은 타일 기호를 그대로 찍는다. */
    private static void checkTiles() {
        Floor floor = DungeonGenerator.createTestFloor();
        Player player = playerAtStart(floor);
        List<String> lines = new MapRenderer().render(floor, player);

        int mismatches = 0;
        for (int y = 0; y < Floor.HEIGHT; y++) {
            for (int x = 0; x < Floor.WIDTH; x++) {
                Position p = new Position(x, y);
                boolean occupied = player.getPos().equals(p)
                        || floor.getMonsterAt(p) != null
                        || floor.getItemAt(p) != null;
                if (!occupied && charAt(lines, p) != floor.getTile(x, y).getDisplayChar()) {
                    mismatches++;
                }
            }
        }
        expect("빈 칸은 모두 타일 기호", mismatches, 0);

        floor.getStairs().ifPresentOrElse(
                stairs -> expect("계단", charAt(lines, stairs), Tile.STAIRS_DOWN.getDisplayChar()),
                () -> expect("테스트 층에 계단이 있음", false, true));
        expect("맵 바깥 ROCK 은 공백", charAt(lines, new Position(0, 0)), ' ');
    }

    private static void checkPlayer() {
        Floor floor = DungeonGenerator.createTestFloor();
        Player player = playerAtStart(floor);
        List<String> lines = new MapRenderer().render(floor, player);

        expect("플레이어 위치에 @", charAt(lines, player.getPos()), Player.SYMBOL);
        expect("@ 는 한 번만", countChar(lines, Player.SYMBOL), 1);
    }

    private static void checkMonsterAndItem() {
        Floor floor = DungeonGenerator.createTestFloor();
        Player player = playerAtStart(floor);
        List<Position> free = freeFloorPositions(floor, player, 2);
        Monster goblin = MonsterFactory.create(MonsterType.GOBLIN, free.get(0));
        floor.addMonster(goblin);
        floor.addItem(FloorItem.gold(10, free.get(1)));

        List<String> lines = new MapRenderer().render(floor, player);
        expect("몬스터 기호", charAt(lines, free.get(0)), goblin.getSymbol());
        expect("골드 기호", charAt(lines, free.get(1)), '$');
    }

    /** 겹친 칸: 플레이어 > 몬스터 > 바닥 아이템 > 타일. */
    private static void checkPriority() {
        Floor floor = DungeonGenerator.createTestFloor();
        Player player = playerAtStart(floor);
        Position underPlayer = player.getPos();
        Position shared = freeFloorPositions(floor, player, 1).get(0);

        floor.addItem(FloorItem.gold(5, underPlayer));
        floor.addItem(FloorItem.gold(5, shared));
        Monster goblin = MonsterFactory.create(MonsterType.GOBLIN, shared);
        floor.addMonster(goblin);

        List<String> lines = new MapRenderer().render(floor, player);
        expect("플레이어 + 아이템 → @", charAt(lines, underPlayer), Player.SYMBOL);
        expect("몬스터 + 아이템 → 몬스터", charAt(lines, shared), goblin.getSymbol());
    }

    private static void checkStatusBar() {
        Player player = new Player("테스트", Job.WARRIOR);
        String expectedStart = ConsoleView.bar("HP", player.getStats().getHp(), player.getStats().getMaxHp(), 10);
        String line = ConsoleView.formatStatusBar(player);

        expect("HP 막대로 시작", line.startsWith(expectedStart), true);
        expect("MP", line.contains("MP " + player.getStats().getMp() + "/" + player.getStats().getMaxMp()), true);
        expect("레벨", line.contains("Lv." + player.getLevel()), true);
        expect("공격(장비 합산)", line.contains("공격 " + player.getAttack()), true);
        expect("방어(장비 합산)", line.contains("방어 " + player.getDefense()), true);
        expect("골드", line.contains("골드 " + player.getGold()), true);

        player.takeDamage(player.getStats().getMaxHp());
        expect("HP 0 이면 빈 막대", ConsoleView.formatStatusBar(player).contains("[----------]"), true);
        expect("막대 32/40", ConsoleView.bar("HP", 32, 40, 10), "HP 32/40 [########--]");
    }

    private static void checkHeader() {
        expect("헤더", ConsoleView.formatHeader(2, 214), " Into the Dark - 지하 2층  |  턴 214");
        expect("보스 기호 설명", ConsoleView.formatBossLegend(), "& 보스: 어둠의 파수꾼 (지하 5층)");
    }

    private static void checkRunResult() {
        Player player = new Player("테스트", Job.WARRIOR);
        player.addGold(57);

        RunResult died = new RunResult();
        died.reachFloor(3);
        died.addKill();
        died.addKill();
        died.finish(player, false, "늑대");
        expect("사망 제목", ConsoleView.formatResultTitle(died), "사망 - 늑대에게 쓰러졌습니다");
        expect("결과 요약", ConsoleView.formatResultSummary(died),
                "도달 층 지하 3층  |  처치 2  |  레벨 " + player.getLevel() + "  |  골드 57");

        RunResult cleared = new RunResult();
        cleared.finish(player, true, "");
        expect("클리어 제목", ConsoleView.formatResultTitle(cleared), "클리어 - 어둠의 파수꾼을 쓰러뜨렸습니다!");
    }

    private static void checkLegend() {
        List<String> empty = ConsoleView.formatLegend(List.of());
        expect("지형 기호 포함", String.join(" ", empty).contains(": 복도"), true);
        expectWithinWidth("기호 안내", empty);

        Position pos = new Position(1, 1);
        List<Monster> monsters = List.of(
                MonsterFactory.create(MonsterType.GOBLIN, pos),
                MonsterFactory.create(MonsterType.SLIME, pos),
                MonsterFactory.create(MonsterType.GOBLIN, pos),
                MonsterFactory.createBoss(pos));
        String joined = String.join(" ", ConsoleView.formatLegend(monsters));
        expect("층의 몬스터 표시", joined.contains("G 고블린") && joined.contains("S 슬라임"), true);
        expect("같은 종류는 한 번만", joined.indexOf("G 고블린") == joined.lastIndexOf("G 고블린"), true);
        expect("보스는 따로 표시", joined.contains("& 보스 어둠의 파수꾼"), true);
        expect("없는 몬스터는 안 보임", joined.contains("늑대"), false);
        expectWithinWidth("몬스터 포함 기호 안내", ConsoleView.formatLegend(monsters));
    }

    private static void checkMessages() {
        expect("한글 너비", ConsoleView.displayWidth("가a"), 3);
        expect("턴 표시", ConsoleView.formatMessage(new MessageLog.Entry(4, "고블린을 쓰러뜨렸습니다.")),
                List.of("> [5턴] 고블린을 쓰러뜨렸습니다."));

        String longText = "해골 병사가 사망하여 뼈 무더기가 되었습니다. 얼마 후에 해골 병사로 부활합니다. 그전에 처치하세요.";
        List<String> wrapped = ConsoleView.formatMessage(new MessageLog.Entry(9, longText));
        expect("긴 메시지는 여러 줄", wrapped.size() > 1, true);
        expectWithinWidth("긴 메시지", wrapped);
        expect("다음 줄은 내용 위치에 맞춰 들여쓰기", wrapped.get(1).startsWith("         "), true);
        expect("내용은 잘리지 않음", String.join(" ", wrapped).replaceAll("\\s+", " ").contains("그전에 처치하세요."), true);

        String noSpace = "가".repeat(40);
        expectWithinWidth("띄어쓰기 없는 긴 메시지", ConsoleView.formatMessage(new MessageLog.Entry(0, noSpace)));

        List<MessageLog.Entry> many = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            many.add(new MessageLog.Entry(1, "m" + i));
        }
        List<String> lines = ConsoleView.formatNewMessages(many);
        expect("넘치면 생략 안내", lines.get(0).contains("3개 생략"), true);
        expect("최근 것만 표시", lines.get(lines.size() - 1), "> [2턴] m7");
        expect("새 메시지가 없으면 빈 줄 없음", ConsoleView.formatNewMessages(List.of()), List.of());
    }

    private static void expectWithinWidth(String name, List<String> lines) {
        for (String line : lines) {
            expect(name + " 너비 60 이하: " + line, ConsoleView.displayWidth(line) <= ConsoleView.SCREEN_WIDTH, true);
        }
    }

    private static Player playerAtStart(Floor floor) {
        Player player = new Player("테스트", Job.WARRIOR);
        player.setPos(floor.getStartPos());
        return player;
    }

    /** 플레이어·몬스터·아이템·계단이 없는 바닥 칸을 count 개 고른다. */
    private static List<Position> freeFloorPositions(Floor floor, Player player, int count) {
        List<Position> result = new ArrayList<>();
        for (int y = 0; y < Floor.HEIGHT && result.size() < count; y++) {
            for (int x = 0; x < Floor.WIDTH && result.size() < count; x++) {
                Position p = new Position(x, y);
                if (floor.getTile(x, y) == Tile.FLOOR && !player.getPos().equals(p)
                        && floor.getMonsterAt(p) == null && floor.getItemAt(p) == null) {
                    result.add(p);
                }
            }
        }
        if (result.size() < count) {
            throw new IllegalStateException("테스트 층에 빈 바닥 칸이 부족합니다: " + result.size());
        }
        return result;
    }

    private static char charAt(List<String> lines, Position p) {
        return lines.get(p.y()).charAt(p.x());
    }

    private static int countChar(List<String> lines, char c) {
        int count = 0;
        for (String line : lines) {
            count += (int) line.chars().filter(ch -> ch == c).count();
        }
        return count;
    }

    private static void expect(String name, Object actual, Object expected) {
        if (Objects.equals(actual, expected)) {
            passed++;
        } else {
            failed++;
            System.out.println("[실패] " + name + ": 기대 " + expected + " / 실제 " + actual);
        }
    }

    private static void expectThrows(String name, Class<? extends RuntimeException> expected, Runnable action) {
        try {
            action.run();
            failed++;
            System.out.println("[실패] " + name + ": " + expected.getSimpleName() + " 이 나지 않음");
        } catch (RuntimeException e) {
            if (expected.isInstance(e)) {
                passed++;
            } else {
                failed++;
                System.out.println("[실패] " + name + ": 기대 " + expected.getSimpleName()
                        + " / 실제 " + e.getClass().getSimpleName());
            }
        }
    }
}
