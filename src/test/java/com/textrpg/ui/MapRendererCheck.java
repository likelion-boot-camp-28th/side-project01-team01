package com.textrpg.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.textrpg.core.MessageLog;
import com.textrpg.core.Position;
import com.textrpg.entity.Job;
import com.textrpg.entity.Player;
import com.textrpg.item.FloorItem;
import com.textrpg.item.ItemFactory;
import com.textrpg.item.ItemType;
import com.textrpg.map.DungeonGenerator;
import com.textrpg.map.FieldOfView;
import com.textrpg.map.Floor;
import com.textrpg.map.Tile;
import com.textrpg.meta.RunResult;
import com.textrpg.monster.Monster;
import com.textrpg.monster.MonsterFactory;
import com.textrpg.monster.MonsterType;

/**
 * MapRenderer(시야·색 포함)와 ConsoleView 의 문자열 생성(formatHeader, formatStatusBar, 결과 화면, 기호 안내, 메시지 줄바꿈, 색) 확인용.
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
        checkFieldOfView();
        checkStatusBar();
        checkHeader();
        checkRunResult();
        checkLegend();
        checkMessages();
        checkMapColors();
        checkTextColors();

        System.out.println("통과 " + passed + " / 실패 " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void checkSize() {
        Floor floor = visibleTestFloor();
        List<String> lines = new MapRenderer().render(floor, playerAtStart(floor));

        expect("줄 수", lines.size(), Floor.HEIGHT);
        for (int y = 0; y < lines.size(); y++) {
            expect(y + "번 줄 길이", lines.get(y).length(), Floor.WIDTH);
        }
        expectThrows("돌려준 목록은 수정 불가", UnsupportedOperationException.class, () -> lines.add("x"));
    }

    /** 아무것도 없는 칸은 타일 기호를 그대로 찍는다. */
    private static void checkTiles() {
        Floor floor = visibleTestFloor();
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
        Floor floor = visibleTestFloor();
        Player player = playerAtStart(floor);
        List<String> lines = new MapRenderer().render(floor, player);

        expect("플레이어 위치에 @", charAt(lines, player.getPos()), Player.SYMBOL);
        expect("@ 는 한 번만", countChar(lines, Player.SYMBOL), 1);
    }

    private static void checkMonsterAndItem() {
        Floor floor = visibleTestFloor();
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
        Floor floor = visibleTestFloor();
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

    /** 안 본 칸은 공백, 본 적 있지만 안 보이는 칸은 타일만(몬스터·아이템 숨김), 보이는 칸은 전부. */
    private static void checkFieldOfView() {
        Floor floor = DungeonGenerator.createTestFloor();
        Player player = playerAtStart(floor);
        List<Position> free = freeFloorPositions(floor, player, 2);
        Position monsterPos = free.get(0);
        Position itemPos = free.get(1);
        Monster goblin = MonsterFactory.create(MonsterType.GOBLIN, monsterPos);
        floor.addMonster(goblin);
        floor.addItem(FloorItem.gold(10, itemPos));
        MapRenderer renderer = new MapRenderer();

        List<String> unseen = renderer.render(floor, player);
        expect("안 본 칸은 공백", charAt(unseen, monsterPos), ' ');
        expect("아무것도 안 봤으면 @ 만 보임", countNonBlank(unseen), 1);

        floor.markSeen(monsterPos.x(), monsterPos.y());
        floor.markSeen(itemPos.x(), itemPos.y());
        List<String> remembered = renderer.render(floor, player);
        expect("본 적 있는 칸의 몬스터는 숨김", charAt(remembered, monsterPos), Tile.FLOOR.getDisplayChar());
        expect("본 적 있는 칸의 아이템은 숨김", charAt(remembered, itemPos), Tile.FLOOR.getDisplayChar());

        floor.setVisible(monsterPos.x(), monsterPos.y());
        floor.setVisible(itemPos.x(), itemPos.y());
        List<String> visible = renderer.render(floor, player);
        expect("보이는 칸의 몬스터", charAt(visible, monsterPos), goblin.getSymbol());
        expect("보이는 칸의 아이템", charAt(visible, itemPos), '$');

        floor.clearVisible();
        List<String> cleared = renderer.render(floor, player);
        expect("시야를 벗어나면 다시 숨김", charAt(cleared, monsterPos), Tile.FLOOR.getDisplayChar());

        Floor fresh = DungeonGenerator.createTestFloor();
        Player walker = playerAtStart(fresh);
        FieldOfView.update(fresh, walker.getPos());
        List<String> start = renderer.render(fresh, walker);
        expect("시작 위치 주변은 보임", countNonBlank(start) > 1, true);
        expect("시작할 때 층 전체가 보이지는 않음",
                countNonBlank(start) < countNonBlank(renderer.render(visibleTestFloor(), walker)), true);
    }

    private static void checkStatusBar() {
        Player player = new Player("테스트", Job.WARRIOR);
        String expectedStart = ConsoleView.bar("HP", player.getStats().getHp(), player.getStats().getMaxHp(), 10);
        String line = ConsoleView.formatStatusBar(player, Painter.PLAIN);

        expect("HP 막대로 시작", line.startsWith(expectedStart), true);
        expect("MP", line.contains("MP " + player.getStats().getMp() + "/" + player.getStats().getMaxMp()), true);
        expect("레벨", line.contains("Lv." + player.getLevel()), true);
        expect("공격(장비 합산)", line.contains("공격 " + player.getAttack()), true);
        expect("방어(장비 합산)", line.contains("방어 " + player.getDefense()), true);
        expect("골드", line.contains("골드 " + player.getGold()), true);

        player.takeDamage(player.getStats().getMaxHp());
        expect("HP 0 이면 빈 막대", ConsoleView.formatStatusBar(player, Painter.PLAIN).contains("[----------]"), true);
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
        died.finish(player, 214, false, "늑대");
        expect("사망 제목", ConsoleView.formatResultTitle(died), "사망 - 늑대에게 쓰러졌습니다");
        expect("결과 요약", ConsoleView.formatResultSummary(died),
                "지하 3층  |  214턴  |  처치 2  |  레벨 " + player.getLevel() + "  |  골드 57");
        expectWithinWidth("결과 요약", List.of(ConsoleView.formatResultSummary(died)));

        RunResult cleared = new RunResult();
        cleared.finish(player, 0, true, "");
        expect("클리어 제목", ConsoleView.formatResultTitle(cleared), "클리어 - 어둠의 파수꾼을 쓰러뜨렸습니다!");
    }

    private static void checkLegend() {
        List<String> empty = ConsoleView.formatLegend(List.of(), Painter.PLAIN);
        expect("지형 기호 포함", String.join(" ", empty).contains(": 복도"), true);
        expectWithinWidth("기호 안내", empty);

        Position pos = new Position(1, 1);
        List<Monster> monsters = List.of(
                MonsterFactory.create(MonsterType.GOBLIN, pos),
                MonsterFactory.create(MonsterType.SLIME, pos),
                MonsterFactory.create(MonsterType.GOBLIN, pos),
                MonsterFactory.createBoss(pos));
        String joined = String.join(" ", ConsoleView.formatLegend(monsters, Painter.PLAIN));
        expect("층의 몬스터 표시", joined.contains("G 고블린") && joined.contains("S 슬라임"), true);
        expect("같은 종류는 한 번만", joined.indexOf("G 고블린") == joined.lastIndexOf("G 고블린"), true);
        expect("보스는 따로 표시", joined.contains("& 보스 어둠의 파수꾼"), true);
        expect("없는 몬스터는 안 보임", joined.contains("늑대"), false);
        expectWithinWidth("몬스터 포함 기호 안내", ConsoleView.formatLegend(monsters, Painter.PLAIN));
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

    /** 색을 입혀도 글자는 같고(색 코드만 더해짐), 몬스터·아이템·플레이어·기억한 칸이 각자 색을 가진다. */
    private static void checkMapColors() {
        Floor floor = visibleTestFloor();
        Player player = playerAtStart(floor);
        List<Position> free = freeFloorPositions(floor, player, 3);
        floor.addMonster(MonsterFactory.create(MonsterType.GOBLIN, free.get(0)));
        floor.addItem(FloorItem.gold(10, free.get(1)));
        floor.addItem(FloorItem.of(ItemFactory.createById("HP_POTION_S"), free.get(2)));
        MapRenderer renderer = new MapRenderer();

        List<String> plain = renderer.render(floor, player);
        List<String> colored = renderer.render(floor, player, Painter.ANSI);
        expect("PLAIN 은 색 코드 없음", String.join("", renderer.render(floor, player, Painter.PLAIN)).contains("\033"), false);
        expect("색 코드를 지우면 같은 글자", colored.stream().map(Painter::strip).toList(), plain);
        String all = String.join("\n", colored);
        expect("플레이어는 굵게", all.contains(ansi(Style.PLAYER, "@")), true);
        expect("몬스터는 빨강", all.contains(ansi(Style.MONSTER, "G")), true);
        expect("골드는 노랑", all.contains(ansi(Style.GOLD, "$")), true);
        expect("물약은 초록", all.contains(ansi(Style.POTION, "!")), true);
        expect("계단 색", all.contains(ansi(Style.STAIRS, ">")), true);
        expect("보이는 바닥은 기본색", all.contains(ansi(Style.REMEMBERED, ".")), false);

        Floor fog = DungeonGenerator.createTestFloor();
        Player walker = playerAtStart(fog);
        Position remembered = freeFloorPositions(fog, walker, 1).get(0);
        fog.markSeen(remembered.x(), remembered.y());
        fog.getStairs().ifPresent(st -> fog.markSeen(st.x(), st.y()));
        String foggy = String.join("\n", renderer.render(fog, walker, Painter.ANSI));
        expect("기억한 칸은 회색", foggy.contains(ansi(Style.REMEMBERED, ".")), true);
        expect("기억한 계단도 계단 색", foggy.contains(ansi(Style.STAIRS, ">")), true);

        Position bossPos = freeFloorPositions(floor, player, 1).get(0);
        floor.addMonster(MonsterFactory.createBoss(bossPos));
        expect("보스는 따로", String.join("", renderer.render(floor, player, Painter.ANSI)).contains(ansi(Style.BOSS, "&")), true);
    }

    /** 상태줄·기호 안내의 색, 너비 계산은 색 코드를 세지 않음, HP 색 기준. */
    private static void checkTextColors() {
        expect("색 코드는 너비에 안 셈", ConsoleView.displayWidth(Painter.ANSI.paint(Style.GOLD, "골드 5")), 6);

        Player player = new Player("테스트", Job.WARRIOR);
        String plain = ConsoleView.formatStatusBar(player, Painter.PLAIN);
        String colored = ConsoleView.formatStatusBar(player, Painter.ANSI);
        expect("상태줄: 색 코드를 지우면 같음", Painter.strip(colored), plain);
        expect("상태줄: 너비 같음", ConsoleView.displayWidth(colored), ConsoleView.displayWidth(plain));
        expect("상태줄: HP 가득이면 초록 막대", colored.startsWith(ansi(Style.HP_HIGH, ConsoleView.bar("HP", 30, 30, 10))), true);

        expect("HP 20/40 은 초록", ConsoleView.hpStyle(20, 40), Style.HP_HIGH);
        expect("HP 19/40 은 노랑", ConsoleView.hpStyle(19, 40), Style.HP_MID);
        expect("HP 10/40 은 노랑", ConsoleView.hpStyle(10, 40), Style.HP_MID);
        expect("HP 9/40 은 빨강", ConsoleView.hpStyle(9, 40), Style.HP_LOW);
        expect("HP 0 은 빨강", ConsoleView.hpStyle(0, 40), Style.HP_LOW);

        Position pos = new Position(1, 1);
        List<Monster> monsters = List.of(
                MonsterFactory.create(MonsterType.GOBLIN, pos),
                MonsterFactory.create(MonsterType.SHADOW_KNIGHT, pos),
                MonsterFactory.create(MonsterType.SKELETON, pos),
                MonsterFactory.createBoss(pos));
        List<String> legend = ConsoleView.formatLegend(monsters, Painter.ANSI);
        expect("기호 안내: 색 코드를 지우면 같음", legend.stream().map(Painter::strip).toList(),
                ConsoleView.formatLegend(monsters, Painter.PLAIN));
        expectWithinWidth("색 입힌 기호 안내", legend);
        String joined = String.join(" ", legend);
        expect("기호 안내: 몬스터 기호 색", joined.contains(ansi(Style.MONSTER, "G") + " 고블린"), true);
        expect("기호 안내: 보스 기호 색", joined.contains(ansi(Style.BOSS, "&") + " 보스"), true);

        expect("무기 색", ConsoleView.itemStyle(ItemType.WEAPON), Style.EQUIPMENT);
        expect("물약 색", ConsoleView.itemStyle(ItemType.POTION), Style.POTION);
    }

    private static String ansi(Style style, String text) {
        return Painter.ANSI.paint(style, text);
    }

    private static void expectWithinWidth(String name, List<String> lines) {
        for (String line : lines) {
            expect(name + " 너비 60 이하: " + line, ConsoleView.displayWidth(line) <= ConsoleView.SCREEN_WIDTH, true);
        }
    }

    /** 시야와 상관없이 그리기를 확인할 때 쓰는, 모든 칸이 보이는 테스트 층. */
    private static Floor visibleTestFloor() {
        Floor floor = DungeonGenerator.createTestFloor();
        for (int y = 0; y < Floor.HEIGHT; y++) {
            for (int x = 0; x < Floor.WIDTH; x++) {
                floor.setVisible(x, y);
            }
        }
        return floor;
    }

    private static int countNonBlank(List<String> lines) {
        int count = 0;
        for (String line : lines) {
            count += (int) line.chars().filter(ch -> ch != ' ').count();
        }
        return count;
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
