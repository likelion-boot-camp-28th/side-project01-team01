package com.textrpg.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.textrpg.context.GameContext;
import com.textrpg.core.MessageLog;
import com.textrpg.entity.Player;
import com.textrpg.entity.Stats;
import com.textrpg.item.Inventory;
import com.textrpg.item.Item;
import com.textrpg.item.ItemType;
import com.textrpg.map.Floor;
import com.textrpg.meta.Profile;
import com.textrpg.meta.RunResult;
import com.textrpg.meta.UpgradeType;
import com.textrpg.monster.Monster;
import com.textrpg.monster.MonsterType;
import com.textrpg.shop.Shop;

/**
 * 모든 콘솔 출력의 단일 창구. 다른 클래스는 System.out 을 직접 쓰지 않는다
 * (예외: 입력 프롬프트와 입력 오류 안내는 InputHandler 가 출력한다).
 * 맵·상태줄 렌더링(render, printStatusBar 등)은 A 파트가 이 클래스에 추가한다.
 *
 * <p>색(ANSI)을 켜면 지도의 몬스터·아이템, 기호 안내, HP·MP 막대, 화면 제목, 가방 아이템 이름에 색을 입힌다.
 * 색은 Style 에서 관리하고, 너비 계산(displayWidth)은 색 코드를 빼고 센다.
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
    private static final String LEGEND_SEPARATOR = "  ";
    /** 탐험 화면에 항상 보여 줄 지형·아이템 기호. 몬스터 기호는 층에 있는 종류만 덧붙인다. 기호 색은 지도와 같다. */
    private static final List<LegendEntry> FIXED_LEGEND = List.of(
            new LegendEntry('@', "나", Style.PLAYER),
            new LegendEntry('#', "벽", null),
            new LegendEntry(':', "복도", null),
            new LegendEntry('+', "문", Style.DOOR),
            new LegendEntry('>', "계단", Style.STAIRS),
            new LegendEntry('$', "골드", Style.GOLD),
            new LegendEntry('!', "물약", Style.POTION),
            new LegendEntry(')', "무기", Style.EQUIPMENT),
            new LegendEntry('[', "방어구", Style.EQUIPMENT));
    private static final int BAR_WIDTH = 10;
    /** 상인이 보일 때 기호 안내에 덧붙이는 항목. */
    private static final LegendEntry MERCHANT_LEGEND = new LegendEntry(Shop.MERCHANT_SYMBOL, "상인(t 대화)", Style.MERCHANT);
    /** 상점 화면에서 아이템 이름·가격 칸의 너비. 가장 긴 이름("작은 치유 물약")과 가격("160골드")이 들어간다. */
    private static final int SHOP_NAME_WIDTH = 16;
    private static final int SHOP_PRICE_WIDTH = 9;

    private final boolean ansiClear;
    private final Painter painter;
    private final MapRenderer mapRenderer = new MapRenderer();

    /**
     * @param ansiClear true 면 ANSI 코드로 화면을 지우고, false 면 빈 줄로 밀어낸다(IntelliJ 콘솔용).
     * @param color     true 면 ANSI 색을 입힌다. IntelliJ 실행 콘솔도 색은 지원하므로 ansiClear 와 따로 정한다.
     */
    public ConsoleView(boolean ansiClear, boolean color) {
        this.ansiClear = ansiClear;
        this.painter = color ? Painter.ANSI : Painter.PLAIN;
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
        printTitle(title, Style.TITLE);
    }

    private void printTitle(String title, Style style) {
        printDivider();
        System.out.println("  " + painter.paint(style, title));
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
     * 탐험 화면 한 장을 그린다: 화면 지우기 → 헤더(층·턴) → 맵 → 상태줄 → 기호 안내 → 구분선 → 새 메시지.
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
        for (String line : mapRenderer.render(floor, player, painter)) {
            printLine(line);
        }
        printStatusBar(player);
        // 기호 안내에는 지금 보이는 몬스터만 넣는다. 안 보이는 몬스터를 알려 주지 않도록
        List<Monster> visibleMonsters = floor.getMonsters().stream()
                .filter(m -> floor.isVisible(m.getPos().x(), m.getPos().y()))
                .toList();
        boolean merchantSeen = floor.getShop()
                .map(shop -> floor.isSeen(shop.getMerchantPos().x(), shop.getMerchantPos().y()))
                .orElse(false);
        for (String line : formatLegend(visibleMonsters, merchantSeen, painter)) {
            printLine(line);
        }
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
        printLine(formatStatusBar(player, painter));
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
            Item item = items.get(i);
            printLine((i + 1) + ") " + painter.paint(itemStyle(item.getType()), item.getDisplayName()));
        }
        printDivider();
        printLine("u 번호: 사용   e 번호: 장착   x 번호: 버리기");
    }

    /**
     * 상점 화면: 진열 상품(살 때 가격), 치료, 장착 중인 장비, 가방(팔 때 받는 값). 번호는 1부터 보여 준다.
     * 명령 입력 안내는 입력을 받는 GameManager 가 출력한다.
     */
    public void printShop(Shop shop, Player player) {
        printTitle(Shop.MERCHANT_NAME + "  |  골드 " + player.getGold()
                + "  |  HP " + player.getStats().getHp() + "/" + player.getMaxHp());
        printLine("판매 상품 (b 번호: 구매)");
        List<Item> stock = shop.getStock();
        if (stock.isEmpty()) {
            printLine("  모두 팔렸습니다.");
        }
        for (int i = 0; i < stock.size(); i++) {
            Item item = stock.get(i);
            printLine(formatShopLine(i + 1, item, item.getPrice()) + item.getEffectText());
        }
        printLine("치료 (h): " + formatHealLine(shop));
        printDivider();
        for (String line : formatEquipment(player)) {
            printLine(line);
        }
        printLine("가방 " + player.getInventory().size() + "/" + player.getInventory().getCapacity()
                + " (s 번호: 판매, 상점가의 " + Shop.SELL_PERCENT + "%)");
        List<Item> bag = player.getInventory().getItems();
        if (bag.isEmpty()) {
            printLine("  가방이 비어 있습니다.");
        }
        for (int i = 0; i < bag.size(); i++) {
            printLine(formatShopLine(i + 1, bag.get(i), Shop.sellPrice(bag.get(i))).stripTrailing());
        }
        printDivider();
    }

    /** 명령어 도움말. CommandParser 의 입력 규칙과 같은 내용. */
    public void printHelp() {
        printTitle("도움말");
        printLine("w a s d   이동 (몬스터 쪽으로 이동하면 공격). dddd 처럼 이어서 입력 가능");
        printLine("g         발밑의 아이템 줍기 (밟으면 자동으로 주움. 가방이 가득 찼을 때 다시 줍기)");
        printLine("i         가방 보기 (가방 화면에서 바로 u/e/x 번호 입력 가능)");
        printLine("l         지난 로그 보기 (메시지마다 몇 번째 턴인지 표시)");
        printLine("t         옆에 있는 상인과 대화 (상점: 사기, 팔기, 치료)");
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
        printLine("! 물약   ) 무기   [ 방어구   $ 골드   " + Shop.MERCHANT_SYMBOL + " 상인   영문자 몬스터");
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
        printTitle(formatResultTitle(result), result.isVictory() ? Style.VICTORY : Style.DEFEAT);
        printLine(formatResultSummary(result));
        printLine("");
        printLine("획득 파편");
        for (String line : shardBreakdown) {
            printLine("  " + line);
        }
        printDivider();
    }

    /**
     * 탐험 화면의 기호 안내. 지형·아이템 기호는 항상, 상인은 본 적이 있을 때, 몬스터는 넘겨받은 종류만 보여 준다.
     * 보스는 일반 몬스터와 헷갈리지 않게 "보스" 를 붙인다. 화면 너비를 넘으면 기호 단위로 줄을 나눈다.
     * 기호는 지도와 같은 색으로 칠한다.
     *
     * @param showMerchant 상인 기호를 넣을지 (층에 상점이 있고 상인 칸을 본 적이 있을 때)
     */
    static List<String> formatLegend(List<Monster> monsters, boolean showMerchant, Painter painter) {
        List<LegendEntry> entries = new ArrayList<>(FIXED_LEGEND);
        if (showMerchant) {
            entries.add(MERCHANT_LEGEND);
        }
        monsters.stream()
                .map(Monster::getType)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .map(type -> type == MonsterType.ABYSS_WARDEN
                        ? new LegendEntry(type.getSymbol(), "보스 " + type.getDisplayName(), Style.BOSS)
                        : new LegendEntry(type.getSymbol(), type.getDisplayName(), Style.MONSTER))
                .forEach(entries::add);
        List<String> items = entries.stream().map(entry -> entry.format(painter)).toList();
        return wrap(items, LEGEND_SEPARATOR, SCREEN_WIDTH, "");
    }

    /** 아이템 종류별 색. 지도 기호, 기호 안내, 가방 목록이 같이 쓴다. */
    static Style itemStyle(ItemType type) {
        return switch (type) {
            case WEAPON, ARMOR -> Style.EQUIPMENT;
            case POTION -> Style.POTION;
            case GOLD -> Style.GOLD;
            case SCROLL, FOOD -> Style.OTHER_ITEM;
        };
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

    /** 터미널에서 차지하는 칸 수. 한글·한자 등 전각 문자는 두 칸으로 세고, 색 코드는 세지 않는다. */
    static int displayWidth(String text) {
        int width = 0;
        for (char c : Painter.strip(text).toCharArray()) {
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

    /** 상점 화면의 상품·가방 한 줄 (효과 설명 앞까지). 예: " 1) 철 검           50골드   " */
    static String formatShopLine(int number, Item item, int price) {
        return padRight(" " + number + ") " + item.getDisplayName(), SHOP_NAME_WIDTH + 4)
                + padRight(price + "골드", SHOP_PRICE_WIDTH);
    }

    /** 상점 화면의 치료 줄. 예: "20골드로 최대 HP 의 50% 회복 (상점마다 1회)" */
    static String formatHealLine(Shop shop) {
        String text = shop.getHealCost() + "골드로 최대 HP 의 " + Shop.HEAL_PERCENT + "% 회복";
        return text + (shop.isHealUsed() ? " (이미 받음)" : " (상점마다 1회)");
    }

    /** 장착 중인 장비 두 줄. 한 줄에 쓰면 좋은 장비일 때 화면 너비를 넘는다. 예: "장착 무기    철 검(공격력 +3)" */
    static List<String> formatEquipment(Player player) {
        String weapon = player.getWeapon()
                .map(w -> w.getDisplayName() + "(" + w.getEffectText() + ")")
                .orElse("없음");
        String armor = player.getArmor()
                .map(a -> a.getDisplayName() + "(" + a.getEffectText() + ")")
                .orElse("없음");
        return List.of("장착 무기    " + weapon, "장착 방어구  " + armor);
    }

    /** 터미널 칸 수(displayWidth) 기준으로 width 칸이 되도록 뒤에 공백을 붙인다. 이미 넘으면 공백 하나만. */
    static String padRight(String text, int width) {
        return text + " ".repeat(Math.max(1, width - displayWidth(text)));
    }

    /** 결과 화면 제목. 예: "사망 - 늑대에게 쓰러졌습니다", "클리어 - 어둠의 파수꾼을 쓰러뜨렸습니다!" */
    static String formatResultTitle(RunResult result) {
        if (result.isVictory()) {
            return "클리어 - 어둠의 파수꾼을 쓰러뜨렸습니다!";
        }
        return "사망 - " + result.getDeathCause() + "에게 쓰러졌습니다";
    }

    /** 결과 요약 한 줄. 예: "지하 3층  |  214턴  |  처치 14  |  레벨 4  |  골드 57" */
    static String formatResultSummary(RunResult result) {
        return "지하 " + result.getDeepestFloor() + "층  |  " + result.getTurns() + "턴  |  처치 " + result.getKills()
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

    /**
     * 예: "HP 32/40 [########--]  MP 5/10  Lv.3  공격 9  방어 4  골드 57". 공격·방어는 장비를 합산한 값.
     * 색을 쓰면 HP 막대는 남은 비율에 따라 초록(절반 이상)·노랑(4분의 1 이상)·빨강, MP 는 파랑, 골드는 노랑이다.
     */
    static String formatStatusBar(Player player, Painter painter) {
        Stats stats = player.getStats();
        return painter.paint(hpStyle(stats.getHp(), stats.getMaxHp()), bar("HP", stats.getHp(), stats.getMaxHp(), BAR_WIDTH))
                + "  " + painter.paint(Style.MP, "MP " + stats.getMp() + "/" + stats.getMaxMp())
                + "  Lv." + player.getLevel()
                + "  공격 " + player.getAttack()
                + "  방어 " + player.getDefense()
                + "  " + painter.paint(Style.GOLD, "골드 " + player.getGold());
    }

    /** HP 막대 색. 절반 이상 초록, 4분의 1 이상 노랑, 그 아래 빨강. */
    static Style hpStyle(int hp, int maxHp) {
        if (hp * 2 >= maxHp) {
            return Style.HP_HIGH;
        }
        return hp * 4 >= maxHp ? Style.HP_MID : Style.HP_LOW;
    }

    /** "HP 32/40 [########--]" 형태의 막대. */
    public static String bar(String label, int current, int max, int width) {
        int filled = max <= 0 ? 0 : (int) Math.round((double) current / max * width);
        filled = Math.max(0, Math.min(width, filled));
        return label + " " + current + "/" + max + " [" + "#".repeat(filled) + "-".repeat(width - filled) + "]";
    }

    /** 기호 안내 한 칸. 예: "$ 골드" (기호만 색을 입힌다). style 이 null 이면 기본 글자색. */
    private record LegendEntry(char symbol, String label, Style style) {

        String format(Painter painter) {
            String mark = String.valueOf(symbol);
            return (style == null ? mark : painter.paint(style, mark)) + " " + label;
        }
    }
}
