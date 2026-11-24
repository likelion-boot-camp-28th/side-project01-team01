package com.textrpg.ui;

import java.util.ArrayList;
import java.util.List;

import com.textrpg.context.GameContext;
import com.textrpg.core.MessageLog;
import com.textrpg.entity.Player;
import com.textrpg.entity.Stats;
import com.textrpg.item.Inventory;
import com.textrpg.item.Item;
import com.textrpg.map.Floor;
import com.textrpg.meta.Profile;
import com.textrpg.meta.RunResult;
import com.textrpg.meta.UpgradeType;
import com.textrpg.monster.MonsterType;

/**
 * 모든 콘솔 출력의 단일 창구. 다른 클래스는 System.out 을 직접 쓰지 않는다.
 * 맵·상태줄 렌더링(render, printStatusBar 등)은 A 파트가 이 클래스에 추가한다.
 */
public class ConsoleView {

    /** 화면 너비(칸). 맵 너비와 같다. 한글은 두 칸으로 센다. */
    static final int SCREEN_WIDTH = Floor.WIDTH;
    private static final String DIVIDER = "-".repeat(SCREEN_WIDTH);
    public static final String GAME_TITLE = "Into the Dark";
    /** 탐험 화면에 보여 줄 새 메시지 최대 개수. 넘치면 앞쪽을 생략하고 로그 화면(l)으로 안내한다. */
    private static final int MAX_NEW_MESSAGES = 5;
    /** 로그 화면(l)에 보여 줄 지난 메시지 수. */
    private static final int LOG_SCREEN_MESSAGES = 30;
    private static final String MESSAGE_PREFIX = "> ";
    private static final int BAR_WIDTH = 10;

    private final boolean ansiClear;
    private final MapRenderer mapRenderer = new MapRenderer();

    /** @param ansiClear true 면 ANSI 코드로 화면을 지우고, false 면 빈 줄로 밀어낸다(IntelliJ 콘솔용). */
    public ConsoleView(boolean ansiClear) {
        this.ansiClear = ansiClear;
    }

    public void clearScreen() {
        if (ansiClear) {
            System.out.print("\033[H\033[2J");
            System.out.flush();
        } else {
            System.out.print("\n".repeat(30));
        }
    }

    public void printLine(String message) {
        System.out.println(message);
    }

    public void printDivider() {
        System.out.println(DIVIDER);
    }

    public void printTitle(String title) {
        printDivider();
        System.out.println("  " + title);
        printDivider();
    }

    /** 번호 메뉴 출력. 예: "1) 새 탐험" */
    public void printMenu(String title, List<String> options) {
        printTitle(title);
        for (int i = 0; i < options.size(); i++) {
            System.out.println((i + 1) + ") " + options.get(i));
        }
    }

    /**
     * 탐험 화면 한 장을 그린다: 화면 지우기 → 헤더(층·턴) → 맵 → 상태줄 → 구분선 → 새 메시지.
     * 메시지는 마지막 행동 이후에 생긴 것(MessageLog.unread)만 보여 주고, 지난 메시지는 로그 화면(l)에서 본다.
     * 명령 입력 안내는 입력을 받는 GameManager 가 출력한다.
     *
     * @param ctx 이번 탐험의 컨텍스트 (층, 플레이어, 턴, 메시지 로그)
     */
    public void render(GameContext ctx) {
        Floor floor = ctx.getFloor();
        Player player = ctx.getPlayer();

        clearScreen();
        printLine(formatHeader(floor.getLevel(), ctx.getTurn()));
        for (String line : mapRenderer.render(floor, player)) {
            printLine(line);
        }
        printStatusBar(player);
        printDivider();
        for (String line : formatNewMessages(ctx.getLog().unread())) {
            printLine(line);
        }
    }

    /** 지난 메시지 화면 (l). 오래된 것부터, 메시지마다 몇 번째 턴에 생겼는지 붙인다. */
    public void printLogHistory(MessageLog log) {
        printTitle("지난 로그 (최근 " + LOG_SCREEN_MESSAGES + "개까지)");
        List<MessageLog.Entry> entries = log.recentEntries(LOG_SCREEN_MESSAGES);
        if (entries.isEmpty()) {
            printLine("아직 기록이 없습니다.");
        }
        for (MessageLog.Entry entry : entries) {
            for (String line : formatMessage(entry)) {
                printLine(line);
            }
        }
        printDivider();
    }

    /** 기획서 1.1 의 상태줄 한 줄: HP 막대, MP, 레벨, 공격, 방어, 골드. */
    public void printStatusBar(Player player) {
        printLine(formatStatusBar(player));
    }

    /** 가방 목록. 번호는 1부터 보여 준다 (입력 u 1 → index 0). 빈 가방이면 안내 문장. */
    public void printInventory(Inventory inventory) {
        printTitle("가방 (" + inventory.size() + "/" + inventory.getCapacity() + ")");
        if (inventory.size() == 0) {
            printLine("가방이 비어 있습니다.");
            return;
        }
        List<Item> items = inventory.getItems();
        for (int i = 0; i < items.size(); i++) {
            printLine((i + 1) + ") " + items.get(i).getDisplayName());
        }
        printDivider();
        printLine("u 번호: 사용   e 번호: 장착   x 번호: 버리기");
    }

    /** 명령어 도움말. CommandParser 의 입력 규칙과 같은 내용. */
    public void printHelp() {
        printTitle("도움말");
        printLine("w a s d   이동 (몬스터 쪽으로 이동하면 공격). dddd 처럼 이어서 입력 가능");
        printLine("g         발밑의 아이템 줍기 (밟으면 자동으로 주움. 가방이 가득 찼을 때 다시 줍기)");
        printLine("i         가방 보기");
        printLine("l         지난 로그 보기 (메시지마다 몇 번째 턴인지 표시)");
        printLine("u 번호    아이템 사용 (번호는 가방 화면 기준 1부터)");
        printLine("e 번호    무기·방어구 장착");
        printLine("x 번호    아이템 버리기");
        printLine(">         계단 위에서 아래층으로 내려가기");
        printLine(".         한 턴 쉬기");
        printLine("save      저장 후 종료");
        printLine("?         도움말");
        printLine("명령은 대소문자를 구분하지 않습니다.");
        printDivider();
        printLine("@ 플레이어   # 벽   : 복도   . 바닥   + 문   > 계단");
        printLine("! 물약   ) 무기   [ 방어구   $ 골드   영문자 몬스터");
        printLine(formatBossLegend());
    }

    /** 어둠의 제단: 보유 파편과 강화 목록(현재 단계 / 최대 단계 / 다음 비용). 번호는 1부터. */
    public void printAltar(Profile profile) {
        printTitle("어둠의 제단  |  보유 파편 " + profile.getShards());
        UpgradeType[] types = UpgradeType.values();
        for (int i = 0; i < types.length; i++) {
            printLine((i + 1) + ") " + formatUpgrade(types[i], profile.getLevel(types[i])));
        }
        printDivider();
    }

    /** 프로필 누적 기록: 총 탐험 수, 최고 층, 클리어 수, 누적 처치, 보유 파편. */
    public void printRecords(Profile profile) {
        printTitle("기록");
        printLine("총 탐험 수   " + profile.getTotalRuns());
        String bestFloor = profile.getBestFloor() == 0 ? "-" : "지하 " + profile.getBestFloor() + "층";
        printLine("최고 층      " + bestFloor);
        printLine("클리어       " + profile.getClears());
        printLine("누적 처치    " + profile.getTotalKills());
        printLine("보유 파편    " + profile.getShards());
        printDivider();
    }

    /**
     * 사망·클리어 결과 화면.
     *
     * @param result         이번 탐험 결과
     * @param shardBreakdown ShardCalculator.breakdown() 결과 (항목별 파편 내역)
     */
    public void printRunResult(RunResult result, List<String> shardBreakdown) {
        printTitle(formatResultTitle(result));
        printLine(formatResultSummary(result));
        printLine("");
        printLine("획득 파편");
        for (String line : shardBreakdown) {
            printLine("  " + line);
        }
        printDivider();
    }

    /**
     * 탐험 화면의 새 메시지 줄들. MAX_NEW_MESSAGES 개를 넘으면 앞쪽을 생략하고 로그 화면으로 안내한다.
     */
    static List<String> formatNewMessages(List<MessageLog.Entry> entries) {
        List<String> lines = new ArrayList<>();
        int skipped = Math.max(0, entries.size() - MAX_NEW_MESSAGES);
        if (skipped > 0) {
            lines.add(MESSAGE_PREFIX + "(앞 메시지 " + skipped + "개 생략, l 로 전체 보기)");
        }
        for (MessageLog.Entry entry : entries.subList(skipped, entries.size())) {
            lines.addAll(formatMessage(entry));
        }
        return lines;
    }

    /**
     * 메시지 하나를 "> [3턴] 내용" 으로 만들고 화면 너비에 맞춰 단어 단위로 줄을 나눈다. 다음 줄은 내용 시작 위치에 맞춰 들여 쓴다.
     * 턴 번호는 그 일이 벌어진 턴(지난 턴 수 + 1)이라, 행동 직후 헤더의 "턴 N" 과 같다.
     */
    static List<String> formatMessage(MessageLog.Entry entry) {
        String prefix = MESSAGE_PREFIX + "[" + (entry.turn() + 1) + "턴] ";
        List<String> words = List.of(entry.text().split(" "));
        List<String> lines = wrap(words, " ", SCREEN_WIDTH - displayWidth(prefix), "");
        String indent = " ".repeat(displayWidth(prefix));
        List<String> result = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            result.add((i == 0 ? prefix : indent) + lines.get(i));
        }
        return result;
    }

    /**
     * 조각(단어·기호 안내)들을 sep 으로 이어 붙이되, 한 줄이 width 칸을 넘으면 조각 단위로 다음 줄로 넘긴다.
     * 조각 하나가 width 보다 길면 글자 단위로 자른다.
     *
     * @param indent 둘째 줄부터 앞에 붙일 문자열 (width 에 포함)
     */
    static List<String> wrap(List<String> pieces, String sep, int width, String indent) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String piece : pieces) {
            for (String part : splitByWidth(piece, width - displayWidth(indent))) {
                String candidate = line.isEmpty() ? part : line + sep + part;
                if (!line.isEmpty() && displayWidth(candidate) > width) {
                    lines.add(line.toString());
                    line = new StringBuilder(indent + part);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
        }
        if (!line.isEmpty() || lines.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    /** width 칸보다 긴 문자열을 width 칸 이하 조각들로 자른다. */
    private static List<String> splitByWidth(String text, int width) {
        List<String> parts = new ArrayList<>();
        StringBuilder part = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (!part.isEmpty() && displayWidth(part.toString()) + charWidth(c) > width) {
                parts.add(part.toString());
                part = new StringBuilder();
            }
            part.append(c);
        }
        parts.add(part.toString());
        return parts;
    }

    /** 터미널에서 차지하는 칸 수. 한글·한자 등 전각 문자는 두 칸으로 센다. */
    static int displayWidth(String text) {
        int width = 0;
        for (char c : text.toCharArray()) {
            width += charWidth(c);
        }
        return width;
    }

    private static int charWidth(char c) {
        boolean wide = (c >= 0x1100 && c <= 0x115F)
                || (c >= 0x2E80 && c <= 0xA4CF)
                || (c >= 0xAC00 && c <= 0xD7A3)
                || (c >= 0xF900 && c <= 0xFAFF)
                || (c >= 0xFF00 && c <= 0xFF60)
                || (c >= 0xFFE0 && c <= 0xFFE6);
        return wide ? 2 : 1;
    }

    /** 결과 화면 제목. 예: "사망 - 늑대에게 쓰러졌습니다", "클리어 - 어둠의 파수꾼을 쓰러뜨렸습니다!" */
    static String formatResultTitle(RunResult result) {
        if (result.isVictory()) {
            return "클리어 - 어둠의 파수꾼을 쓰러뜨렸습니다!";
        }
        return "사망 - " + result.getDeathCause() + "에게 쓰러졌습니다";
    }

    /** 결과 요약 한 줄. 예: "도달 층 지하 3층  |  처치 14  |  레벨 4  |  골드 57" */
    static String formatResultSummary(RunResult result) {
        return "도달 층 지하 " + result.getDeepestFloor() + "층  |  처치 " + result.getKills()
                + "  |  레벨 " + result.getFinalLevel() + "  |  골드 " + result.getGold();
    }

    /** 도움말의 보스 기호 줄. 일반 몬스터(영문자)와 헷갈리지 않게 따로 보여 준다. 예: "& 보스: 어둠의 파수꾼 (지하 5층)" */
    static String formatBossLegend() {
        MonsterType boss = MonsterType.ABYSS_WARDEN;
        return boss.getSymbol() + " 보스: " + boss.getDisplayName() + " (지하 " + boss.getMinFloor() + "층)";
    }

    /** 제단 한 줄. 예: "강인한 육체 - 최대 HP +5  (2/5단계, 다음 60 파편)". 최대 단계면 "완료". */
    static String formatUpgrade(UpgradeType type, int level) {
        String progress = level >= type.getMaxLevel()
                ? "완료"
                : "다음 " + type.costFor(level + 1) + " 파편";
        return type.getDisplayName() + " - " + type.getDescription()
                + "  (" + level + "/" + type.getMaxLevel() + "단계, " + progress + ")";
    }

    /** 탐험 화면 맨 윗줄. 한글은 터미널에서 두 칸을 차지해 길이로 정렬하면 어긋나므로 구분자로 나눈다. */
    static String formatHeader(int floorLevel, int turn) {
        return " " + GAME_TITLE + " - 지하 " + floorLevel + "층  |  턴 " + turn;
    }

    /** 예: "HP 32/40 [########--]  MP 5/10  Lv.3  공격 9  방어 4  골드 57". 공격·방어는 장비를 합산한 값. */
    static String formatStatusBar(Player player) {
        Stats stats = player.getStats();
        return bar("HP", stats.getHp(), stats.getMaxHp(), BAR_WIDTH)
                + "  MP " + stats.getMp() + "/" + stats.getMaxMp()
                + "  Lv." + player.getLevel()
                + "  공격 " + player.getAttack()
                + "  방어 " + player.getDefense()
                + "  골드 " + player.getGold();
    }

    /** "HP 32/40 [########--]" 형태의 막대. */
    public static String bar(String label, int current, int max, int width) {
        int filled = max <= 0 ? 0 : (int) Math.round((double) current / max * width);
        filled = Math.max(0, Math.min(width, filled));
        return label + " " + current + "/" + max + " [" + "#".repeat(filled) + "-".repeat(width - filled) + "]";
    }
}
