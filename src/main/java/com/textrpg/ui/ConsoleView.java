package com.textrpg.ui;

import java.util.List;

import com.textrpg.context.GameContext;
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

    private static final String DIVIDER = "-".repeat(60);
    public static final String GAME_TITLE = "Into the Dark";
    /** 탐험 화면 아래에 보여 줄 최근 메시지 수 (기획서 기능 표 "화면 렌더링": 최근 메시지 3줄). */
    private static final int RECENT_MESSAGES = 3;
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
     * 탐험 화면 한 장을 그린다: 화면 지우기 → 헤더(층·턴) → 맵 → 상태줄 → 구분선 → 최근 메시지.
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
        for (String message : ctx.getLog().recent(RECENT_MESSAGES)) {
            printLine(MESSAGE_PREFIX + message);
        }
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
        printLine("g         발밑의 골드·아이템 줍기");
        printLine("i         가방 보기");
        printLine("u 번호    아이템 사용 (번호는 가방 화면 기준 1부터)");
        printLine("e 번호    무기·방어구 장착");
        printLine("x 번호    아이템 버리기");
        printLine(">         계단 위에서 아래층으로 내려가기");
        printLine(".         한 턴 쉬기");
        printLine("save      저장 후 종료");
        printLine("?         도움말");
        printLine("명령은 대소문자를 구분하지 않습니다.");
        printDivider();
        printLine("@ 플레이어   # 벽·복도   . 바닥   + 문   > 계단");
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
