package com.textrpg.save;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.textrpg.CheckRunner;
import com.textrpg.context.GameContext;
import com.textrpg.core.Position;
import com.textrpg.entity.Job;
import com.textrpg.entity.Player;
import com.textrpg.item.FloorItem;
import com.textrpg.item.Inventory;
import com.textrpg.item.ItemFactory;
import com.textrpg.map.DungeonGenerator;
import com.textrpg.map.Floor;
import com.textrpg.meta.Profile;
import com.textrpg.meta.RunResult;
import com.textrpg.meta.UpgradeType;
import com.textrpg.monster.Monster;
import com.textrpg.monster.MonsterFactory;
import com.textrpg.monster.MonsterType;
import com.textrpg.shop.Shop;

import static com.textrpg.CheckRunner.expect;
import static com.textrpg.CheckRunner.expectThrows;

/**
 * SaveManager 파일 입출력과 프로필 저장·불러오기(SlotCodec) 확인용. JUnit 대신 main 으로 실행한다 (Team_Rules 2.11).
 * 실패가 하나라도 있으면 종료 코드 1.
 *
 * <p>케이스마다 임시 폴더를 새로 만들고 끝나면 지운다. 실제 saves 폴더는 건드리지 않는다.
 */
public final class SaveManagerCheck {

    private SaveManagerCheck() {
    }

    public static void main(String[] args) throws IOException {
        withTempDir(SaveManagerCheck::checkSlotNumbers);
        withTempDir(SaveManagerCheck::checkInitialState);
        withTempDir(SaveManagerCheck::checkWriteAndRead);
        withTempDir(SaveManagerCheck::checkOverwrite);
        withTempDir(SaveManagerCheck::checkSlotsAreIndependent);
        withTempDir(SaveManagerCheck::checkLeftoverTempFile);
        withTempDir(SaveManagerCheck::checkReadFailures);
        withTempDir(SaveManagerCheck::checkWriteFailures);
        withTempDir(SaveManagerCheck::checkWithParserAndWriter);
        withTempDir(SaveManagerCheck::checkProfileRoundTrip);
        withTempDir(SaveManagerCheck::checkClearRun);
        withTempDir(SaveManagerCheck::checkHasRun);
        withTempDir(SaveManagerCheck::checkSlotSummary);
        withTempDir(SaveManagerCheck::checkBrokenProfile);
        withTempDir(SaveManagerCheck::checkRunEncode);
        withTempDir(SaveManagerCheck::checkRunRoundTrip);
        withTempDir(SaveManagerCheck::checkBrokenRun);
        withTempDir(SaveManagerCheck::checkRunModifiers);
        withTempDir(SaveManagerCheck::checkShopRoundTrip);
        withTempDir(SaveManagerCheck::checkBrokenShop);
        checkRunResultRestore();

        CheckRunner.finish();
    }

    private static void checkSlotNumbers(Path root) {
        SaveManager m = new SaveManager(root.resolve("saves"));
        for (int slot : new int[] {0, SaveManager.SLOT_COUNT + 1, -1}) {
            expectThrows("exists(" + slot + ")", IllegalArgumentException.class, () -> m.exists(slot));
            expectThrows("readLines(" + slot + ")", IllegalArgumentException.class, () -> m.readLines(slot));
            expectThrows("writeLines(" + slot + ")", IllegalArgumentException.class,
                    () -> m.writeLines(slot, List.of("[A]")));
        }
        expectThrows("lines 가 null", IllegalArgumentException.class, () -> m.writeLines(1, null));
        expect("거부된 호출은 폴더를 만들지 않음", Files.exists(root.resolve("saves")), false);
    }

    private static void checkInitialState(Path root) {
        Path saveDir = root.resolve("saves");
        SaveManager m = new SaveManager(saveDir);
        for (int slot = 1; slot <= SaveManager.SLOT_COUNT; slot++) {
            expect("처음에는 slot" + slot + " 없음", m.exists(slot), false);
        }
        expect("exists 는 폴더를 만들지 않음", Files.exists(saveDir), false);
    }

    private static void checkWriteAndRead(Path root) throws IOException {
        Path saveDir = root.resolve("saves");
        SaveManager m = new SaveManager(saveDir);
        List<String> lines = List.of("# 헤더", "[PLAYER]", "name=용사", "armor=", "map0=W..##..+>");

        m.writeLines(1, lines);
        expect("저장하면 폴더가 생김", Files.isDirectory(saveDir), true);
        expect("저장 후 exists", m.exists(1), true);
        expect("쓴 줄 그대로 읽음", m.readLines(1), lines);
        expect("폴더에는 슬롯 파일만", fileNames(saveDir), List.of("slot1.sav"));

        m.writeLines(2, List.of());
        expect("빈 목록도 저장됨", m.exists(2), true);
        expect("빈 목록 읽기", m.readLines(2), List.of());
    }

    private static void checkOverwrite(Path root) throws IOException {
        Path saveDir = root.resolve("saves");
        SaveManager m = new SaveManager(saveDir);
        m.writeLines(1, List.of("[A]", "k=old", "extra=1"));
        m.writeLines(1, List.of("[A]", "k=new"));
        expect("덮어쓰면 새 내용만", m.readLines(1), List.of("[A]", "k=new"));
        expect("덮어쓴 뒤 tmp 가 남지 않음", fileNames(saveDir), List.of("slot1.sav"));
    }

    private static void checkSlotsAreIndependent(Path root) {
        SaveManager m = new SaveManager(root.resolve("saves"));
        m.writeLines(1, List.of("[A]", "slot=1"));
        expect("다른 슬롯은 그대로 없음", m.exists(2), false);
        m.writeLines(2, List.of("[A]", "slot=2"));
        expect("1번 내용 유지", m.readLines(1), List.of("[A]", "slot=1"));
        expect("2번 내용", m.readLines(2), List.of("[A]", "slot=2"));
    }

    /** 저장 도중 꺼져서 tmp 만 남은 상황. */
    private static void checkLeftoverTempFile(Path root) throws IOException {
        Path saveDir = Files.createDirectories(root.resolve("saves"));
        Files.writeString(saveDir.resolve("slot3.sav.tmp"), "[A]\nk=반쯤 쓰다 만");
        SaveManager m = new SaveManager(saveDir);

        expect("tmp 만 있으면 세이브 없음", m.exists(3), false);
        m.writeLines(3, List.of("[A]", "k=v"));
        expect("남은 tmp 가 있어도 저장됨", m.readLines(3), List.of("[A]", "k=v"));
        expect("저장 후 tmp 가 사라짐", fileNames(saveDir), List.of("slot3.sav"));
    }

    private static void checkReadFailures(Path root) throws IOException {
        Path saveDir = Files.createDirectories(root.resolve("saves"));
        SaveManager m = new SaveManager(saveDir);

        expectThrows("없는 파일 읽기", SaveLoadException.class, () -> m.readLines(2)).ifPresent(missing -> {
            expect("없는 파일 원인", missing.getCause() instanceof NoSuchFileException, true);
            expect("메시지에 파일 이름", missing.getMessage().contains("slot2.sav"), true);
            expect("메시지에 폴더 경로는 없음", missing.getMessage().contains(saveDir.toString()), false);
        });

        // 0xC3 0x28 은 UTF-8 로 해석할 수 없는 바이트 조합
        Files.write(saveDir.resolve("slot1.sav"), new byte[] {'[', 'A', ']', '\n', (byte) 0xC3, 0x28});
        expectThrows("UTF-8 이 아닌 파일", SaveLoadException.class, () -> m.readLines(1))
                .ifPresent(broken -> expect("손상 파일 원인", broken.getCause() instanceof CharacterCodingException, true));
    }

    private static void checkWriteFailures(Path root) throws IOException {
        // 세이브 폴더 자리에 일반 파일이 있으면 폴더를 만들 수 없다
        Path notADir = Files.writeString(root.resolve("saves"), "폴더가 아님");
        SaveManager blocked = new SaveManager(notADir);
        expectThrows("폴더를 만들 수 없음", SaveLoadException.class,
                () -> blocked.writeLines(1, List.of("[A]"))).ifPresent(noDir -> {
            expect("폴더 실패 원인 보관", noDir.getCause() instanceof IOException, true);
            expect("폴더 실패 메시지에 파일 이름", noDir.getMessage().contains("slot1.sav"), true);
        });

        // 슬롯 파일 자리에 비어 있지 않은 폴더가 있으면 교체(move)가 실패한다
        Path saveDir = Files.createDirectories(root.resolve("saves2"));
        Files.createDirectories(saveDir.resolve("slot2.sav").resolve("x"));
        SaveManager m = new SaveManager(saveDir);
        m.writeLines(1, List.of("[A]", "k=keep"));
        expectThrows("교체 실패", SaveLoadException.class, () -> m.writeLines(2, List.of("[A]")));
        expect("교체 실패 후 tmp 정리", fileNames(saveDir), List.of("slot1.sav", "slot2.sav"));
        expect("교체 실패해도 다른 슬롯 유지", m.readLines(1), List.of("[A]", "k=keep"));
    }

    private static void checkWithParserAndWriter(Path root) {
        SaveFileWriter w = new SaveFileWriter();
        w.comment("Into the Dark Save File");
        w.section("META");
        w.put("version", 1);
        w.section("PLAYER");
        w.put("name", "용사");
        w.section("MONSTERS");
        w.put("monster", "GOBLIN,33,2,9");
        w.put("monster", "BAT,40,4,6");

        SaveManager m = new SaveManager(root.resolve("saves"));
        m.writeLines(1, w.toLines());
        SaveFileParser p = SaveFileParser.parse(m.readLines(1));
        expect("통합: version", p.getInt("META", "version"), 1);
        expect("통합: 한글", p.getString("PLAYER", "name"), "용사");
        expect("통합: 반복 key", p.getList("MONSTERS", "monster"), List.of("GOBLIN,33,2,9", "BAT,40,4,6"));
        expect("통합: RUN 없음", p.hasSection("RUN"), false);
    }

    private static List<String> fileNames(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(f -> f.getFileName().toString()).sorted().collect(Collectors.toList());
        }
    }

    private static void checkProfileRoundTrip(Path root) {
        SaveManager m = new SaveManager(root.resolve("saves"));
        Profile saved = sampleProfile();
        m.save(1, SlotData.profileOnly(saved));

        SlotData loaded = m.load(1);
        expect("불러온 슬롯에 탐험 없음", loaded.hasRun(), false);
        expectSameProfile("저장 → 불러오기", loaded.profile(), saved);

        Profile played = Profile.newProfile();
        RunResult run = new RunResult();
        run.reachFloor(2);
        run.addKill();
        played.recordRun(run, 50);
        played.buy(UpgradeType.MAX_HP);
        m.save(2, SlotData.profileOnly(played));
        expectSameProfile("플레이 후 프로필", m.load(2).profile(), played);
        expectSameProfile("새 프로필", roundTrip(m, Profile.newProfile()), Profile.newProfile());
    }

    private static void checkClearRun(Path root) {
        SaveManager m = new SaveManager(root.resolve("saves"));
        Profile p = sampleProfile();
        m.clearRun(1, p);
        expect("clearRun 후 탐험 없음", m.load(1).hasRun(), false);
        expectSameProfile("clearRun 은 프로필을 저장", m.load(1).profile(), p);
    }

    private static void checkSlotSummary(Path root) {
        SaveManager m = new SaveManager(root.resolve("saves"));
        expect("빈 슬롯 요약", m.getSlotSummary(1), "(새 슬롯)");

        m.save(1, SlotData.profileOnly(sampleProfile()));
        expect("프로필 요약", m.getSlotSummary(1), "파편 200 | 최고 3층 | 탐험 6회");
        m.save(2, SlotData.profileOnly(Profile.newProfile()));
        expect("최고 층 없음", m.getSlotSummary(2), "파편 0 | 최고 - | 탐험 0회");

        m.writeLines(3, List.of("[META]", "version=99"));
        expect("손상된 세이브 요약", m.getSlotSummary(3), "(손상된 세이브)");
    }

    private static void checkBrokenProfile(Path root) {
        SaveManager m = new SaveManager(root.resolve("saves"));
        List<String> valid = List.of("[META]", "version=1", "[PROFILE]", "shards=10", "totalRuns=1",
                "bestFloor=1", "clears=0", "totalKills=2", "unlockedJobs=WARRIOR", "[UPGRADES]", "MAX_HP=1");
        m.writeLines(1, valid);
        expect("직접 쓴 파일 읽기", m.load(1).profile().getLevel(UpgradeType.MAX_HP), 1);

        m.writeLines(1, with(valid, "MAX_HP=1", "FUTURE_UPGRADE=3"));
        expect("모르는 강화 id 는 무시", m.load(1).profile().getShards(), 10);
        m.writeLines(1, with(valid, "MAX_HP=1", "# 강화 없음"));
        expect("없는 강화는 0단계", m.load(1).profile().getLevel(UpgradeType.MAX_HP), 0);

        expectBroken(m, "버전 불일치", with(valid, "version=1", "version=2"));
        expectBroken(m, "META 없음", valid.subList(2, valid.size()));
        expectBroken(m, "필수 key 없음", with(valid, "shards=10", "# shards 없음"));
        expectBroken(m, "숫자가 아님", with(valid, "shards=10", "shards=많음"));
        expectBroken(m, "음수", with(valid, "shards=10", "shards=-1"));
        expectBroken(m, "모르는 직업", with(valid, "unlockedJobs=WARRIOR", "unlockedJobs=WARRIOR,KNIGHT"));
        expectBroken(m, "최대 단계 초과", with(valid, "MAX_HP=1", "MAX_HP=" + (UpgradeType.MAX_HP.getMaxLevel() + 1)));
    }

    private static void checkRunEncode(Path root) {
        GameContext run = sampleRun();
        Player player = run.getPlayer();
        Floor floor = run.getFloor();

        SaveManager m = new SaveManager(root.resolve("saves"));
        m.save(1, SlotData.withRun(sampleProfile(), run));
        SaveFileParser p = SaveFileParser.parse(m.readLines(1));

        expect("탐험 저장: 프로필도 함께", p.getInt("PROFILE", "shards"), 200);
        expect("[RUN] turn", p.getInt("RUN", "turn"), 3);
        expect("[RUN] kills", p.getInt("RUN", "kills"), 1);
        expect("[RUN] deepestFloor", p.getInt("RUN", "deepestFloor"), 2);

        expect("[PLAYER] name", p.getString("PLAYER", "name"), "용사");
        expect("[PLAYER] job", p.getString("PLAYER", "job"), "WARRIOR");
        expect("[PLAYER] gold", p.getInt("PLAYER", "gold"), 15);
        expect("[PLAYER] hp", p.getInt("PLAYER", "hp"), player.getStats().getHp());
        expect("[PLAYER] maxHp", p.getInt("PLAYER", "maxHp"), player.getStats().getMaxHp());
        expect("[PLAYER] attack 는 장비 뺀 값", p.getInt("PLAYER", "attack"), player.getBaseAttack());
        expect("[PLAYER] 위치", new Position(p.getInt("PLAYER", "x"), p.getInt("PLAYER", "y")), player.getPos());
        expect("[PLAYER] weapon", p.getString("PLAYER", "weapon"), "IRON_SWORD");
        expect("[PLAYER] 방어구 없으면 빈 값", p.getString("PLAYER", "armor"), "");
        expect("[INVENTORY]", p.getList("INVENTORY", "item"), List.of("HP_POTION_S"));

        expect("[FLOOR] level", p.getInt("FLOOR", "level"), floor.getLevel());
        List<String> tiles = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (int y = 0; y < Floor.HEIGHT; y++) {
            tiles.add(p.getString("FLOOR", "map" + y));
            seen.add(p.getString("FLOOR", "seen" + y));
        }
        expect("[FLOOR] 타일", tiles, floor.exportTiles());
        expect("[FLOOR] 탐험 기록", seen, floor.exportSeen());
        expect("[FLOOR] 방", p.getList("FLOOR", "room"), floor.exportRooms());

        List<String> monsters = p.getList("MONSTERS", "monster");
        expect("[MONSTERS] 깨어난 고블린", monsters.contains("GOBLIN,1,1,5,true"), true);
        expect("[MONSTERS] 뼈 무더기는 탄생 턴까지", monsters.contains("PILE_OF_BONES,2,1,10,true,7"), true);
        List<String> items = p.getList("ITEMS", "item");
        expect("[ITEMS] 골드", items.contains("GOLD:15,3,1"), true);
        expect("[ITEMS] 아이템", items.contains("LEATHER_ARMOR,4,1"), true);
    }

    private static void checkRunRoundTrip(Path root) {
        SaveManager m = new SaveManager(root.resolve("saves"));
        GameContext saved = sampleRun();
        m.save(1, SlotData.withRun(sampleProfile(), saved));

        SlotData loaded = m.load(1);
        expect("불러온 슬롯에 탐험 있음", loaded.hasRun(), true);
        expectSameProfile("탐험과 함께 저장한 프로필", loaded.profile(), sampleProfile());
        GameContext run = loaded.run().orElseThrow();
        Player p = run.getPlayer();
        expect("복원: 이름·직업·레벨", List.of(p.getName(), p.getJob(), p.getLevel()), List.of("용사", Job.WARRIOR, 1));
        expect("복원: HP", p.getStats().getHp(), saved.getPlayer().getStats().getHp());
        expect("복원: 공격력은 철 검 포함", p.getAttack(), saved.getPlayer().getAttack());
        expect("복원: 장착 무기는 가방에 없음", p.getInventory().size(), 1);
        expect("복원: 위치", p.getPos(), saved.getPlayer().getPos());
        expect("복원: 턴", run.getTurn(), 3);
        expect("복원: 처치·도달 층", List.of(run.getRunResult().getKills(), run.getRunResult().getDeepestFloor()),
                List.of(1, 2));
        expect("복원: 몬스터 수", run.getFloor().getMonsters().size(), saved.getFloor().getMonsters().size());
        expect("복원: 계단", run.getFloor().getStairs(), saved.getFloor().getStairs());
        // 다시 저장한 내용이 처음 저장한 내용과 같으면 모든 값이 그대로 복원된 것이다 (저장 시각 제외)
        expect("다시 저장해도 같은 내용", withoutSavedAt(SlotCodec.encode(loaded)),
                withoutSavedAt(m.readLines(1)));

        expect("탐험 요약", m.getSlotSummary(1), "파편 200 | 최고 3층 | 탐험 6회 | 진행 중: 용사 Lv.1 전사 지하 "
                + saved.getFloor().getLevel() + "층");
        m.clearRun(1, loaded.profile());
        expect("clearRun 후 탐험 삭제", m.load(1).hasRun(), false);
    }

    private static void checkBrokenRun(Path root) {
        SaveManager m = new SaveManager(root.resolve("saves"));
        m.save(1, SlotData.withRun(sampleProfile(), sampleRun()));
        List<String> valid = m.readLines(1);
        String goblin = valid.stream().filter(l -> l.startsWith("monster=GOBLIN")).findFirst().orElseThrow();

        expectBrokenRun(m, "모르는 몬스터", with(valid, goblin, goblin.replace("GOBLIN", "DRAGON")));
        expectBrokenRun(m, "몬스터 HP 0", with(valid, goblin, "monster=GOBLIN,1,1,0,true"));
        expectBrokenRun(m, "몬스터 칸 수", with(valid, goblin, "monster=GOBLIN,1,1,5"));
        expectBrokenRun(m, "맵 밖 아이템", with(valid, "item=GOLD:15,3,1", "item=GOLD:15,99,1"));
        expectBrokenRun(m, "모르는 아이템", with(valid, "item=HP_POTION_S", "item=MAGIC_RING"));
        expectBrokenRun(m, "무기 칸에 물약", with(valid, "weapon=IRON_SWORD", "weapon=HP_POTION_S"));
        expectBrokenRun(m, "경험치 초과", with(valid, "exp=0", "exp=" + Player.EXP_PER_LEVEL));
        expectBrokenRun(m, "플레이어 HP 0", replaceKey(valid, "hp=", "hp=0"));
        expectBrokenRun(m, "모르는 직업", with(valid, "job=WARRIOR", "job=KNIGHT"));
        expectBrokenRun(m, "맵 줄 누락", valid.stream().filter(l -> !l.startsWith("map5=")).toList());
        expectBrokenRun(m, "turn 없음", valid.stream().filter(l -> !l.startsWith("turn=")).toList());

        m.writeLines(1, with(valid, goblin, goblin.replace("GOBLIN", "DRAGON")));
        expect("탐험만 손상된 요약", m.getSlotSummary(1), "파편 200 | 최고 3층 | 탐험 6회 | 탐험 기록 손상");
        m.writeLines(1, replaceKey(valid, "shards=", "shards=-1"));
        expectThrows("프로필이 손상되면 전체 손상", SaveLoadException.class, () -> m.load(1))
                .ifPresent(e -> expect("프로필 손상은 CorruptedRunException 아님", e instanceof CorruptedRunException, false));
    }

    /** hasRun: 객체를 복원하지 않고 [RUN] 섹션만 본다. load(slot).hasRun() 과 같은 답이어야 한다. */
    private static void checkHasRun(Path root) {
        SaveManager m = new SaveManager(root.resolve("saves"));
        expect("파일이 없으면 탐험 없음", m.hasRun(1), false);
        m.save(1, SlotData.profileOnly(sampleProfile()));
        expect("프로필만 있으면 탐험 없음", m.hasRun(1), false);
        expect("load 와 같은 답 (탐험 없음)", m.hasRun(1), m.load(1).hasRun());
        m.save(1, SlotData.withRun(sampleProfile(), sampleRun()));
        expect("탐험이 있으면 true", m.hasRun(1), true);
        expect("load 와 같은 답 (탐험 있음)", m.hasRun(1), m.load(1).hasRun());
        m.writeLines(2, List.of("version=1"));
        expectThrows("형식이 깨진 파일은 SaveLoadException", SaveLoadException.class, () -> m.hasRun(2));
    }

    /** 상점이 있는 층: [SHOP] 에 상인 위치, 치료 여부, 남은 진열 상품을 쓰고 그대로 되살린다. 상점이 없으면 섹션도 없다. */
    private static void checkShopRoundTrip(Path root) {
        SaveManager m = new SaveManager(root.resolve("saves"));
        m.save(1, SlotData.withRun(sampleProfile(), sampleRun()));
        expect("상점 없는 층은 [SHOP] 없음", SaveFileParser.parse(m.readLines(1)).hasSection("SHOP"), false);
        expect("상점 없는 층을 불러오면 상점 없음", m.load(1).run().orElseThrow().getFloor().getShop().isPresent(), false);

        GameContext saved = sampleRun();
        saved.getFloor().setShop(sampleShop());
        m.save(1, SlotData.withRun(sampleProfile(), saved));
        SaveFileParser p = SaveFileParser.parse(m.readLines(1));
        expect("[SHOP] 상인 위치", new Position(p.getInt("SHOP", "x"), p.getInt("SHOP", "y")), new Position(33, 7));
        expect("[SHOP] 치료 여부", p.getString("SHOP", "healUsed"), "true");
        expect("[SHOP] 남은 상품", p.getList("SHOP", "item"), List.of("IRON_SWORD", "HP_POTION_S"));

        Shop shop = m.load(1).run().orElseThrow().getFloor().getShop().orElseThrow();
        expect("복원: 상인 위치", shop.getMerchantPos(), new Position(33, 7));
        expect("복원: 치료 여부", shop.isHealUsed(), true);
        expect("복원: 상품", shop.getStock().stream().map(i -> i.getId()).toList(), List.of("IRON_SWORD", "HP_POTION_S"));
        expect("복원: 치료 가격은 층 기준", shop.getHealCost(), saved.getFloor().getLevel() * Shop.HEAL_COST_PER_FLOOR);
    }

    private static void checkBrokenShop(Path root) {
        SaveManager m = new SaveManager(root.resolve("saves"));
        GameContext run = sampleRun();
        run.getFloor().setShop(sampleShop());
        m.save(1, SlotData.withRun(sampleProfile(), run));
        List<String> valid = m.readLines(1);

        expectBrokenRun(m, "상인이 방 밖", with(valid, "x=33", "x=0"));
        expectBrokenRun(m, "치료 여부가 true/false 아님", with(valid, "healUsed=true", "healUsed=yes"));
        expectBrokenRun(m, "모르는 상품", with(valid, "item=IRON_SWORD", "item=MAGIC_RING"));
        Position start = run.getPlayer().getPos();
        expectBrokenRun(m, "플레이어가 상인 칸에 있음",
                with(with(valid, "x=33", "x=" + start.x()), "y=7", "y=" + start.y()));
        List<String> tooMany = new ArrayList<>(valid);
        int last = tooMany.lastIndexOf("item=HP_POTION_S");
        for (int i = 0; i < Shop.STOCK_SIZE; i++) {
            tooMany.add(last, "item=HP_POTION_S");
        }
        expectBrokenRun(m, "진열 상품이 너무 많음", tooMany);
    }

    /** 테스트 층 두 번째 방(25,4 ~ 34,9)의 상점. 철 검·작은 치유 물약이 남았고 치료는 받았다. */
    private static Shop sampleShop() {
        return Shop.restore(1, new Position(33, 7),
                List.of(ItemFactory.createById("IRON_SWORD"), ItemFactory.createById("HP_POTION_S")), true);
    }

    /** 가방 칸·경험치 배율은 세이브에 없어서 불러올 때 프로필로 다시 맞춘다. 강화가 두 번 붙으면 안 된다. */
    private static void checkRunModifiers(Path root) {
        SaveManager m = new SaveManager(root.resolve("saves"));
        Profile profile = Profile.restore(0, 1, 1, 0, 0, EnumSet.of(Job.WARRIOR),
                Map.of(UpgradeType.BAG_SIZE, 1, UpgradeType.EXP_BOOST, 1, UpgradeType.MAX_HP, 1,
                        UpgradeType.ACCURACY, 1));
        GameContext run = sampleRun();
        Player saved = run.getPlayer();
        profile.applyRunModifiers(saved);
        while (!saved.getInventory().isFull()) {
            saved.getInventory().add(ItemFactory.createById("HP_POTION_S"));
        }
        m.save(1, SlotData.withRun(profile, run));

        Player loaded = m.load(1).run().orElseThrow().getPlayer();
        expect("큰 가방 칸 유지", loaded.getInventory().getCapacity(), Inventory.CAPACITY + 2);
        expect("가득 찬 가방 그대로", loaded.getInventory().size(), saved.getInventory().size());
        expect("최대 HP 강화는 다시 붙지 않음", loaded.getStats().getMaxHp(), saved.getStats().getMaxHp());
        loaded.gainExp(10);
        expect("배움의 축복 +10% 유지", loaded.getExp(), 11);
        expect("매의 눈 명중률 유지", loaded.getStats().getHitRate(), saved.getStats().getHitRate());
        expect("매의 눈 1단계 = 전사 90 + 3", loaded.getStats().getHitRate(), 93.0);

        Player rogue = new Player("도적", Job.ROGUE);
        Profile maxAccuracy = Profile.restore(0, 0, 0, 0, 0, EnumSet.of(Job.WARRIOR, Job.ROGUE),
                Map.of(UpgradeType.ACCURACY, UpgradeType.ACCURACY.getMaxLevel()));
        maxAccuracy.applyTo(rogue);
        maxAccuracy.applyRunModifiers(rogue);
        expect("명중률은 100% 를 넘을 수 있음 (도적 95 + 9)", rogue.getStats().getHitRate(), 104.0);
    }

    /**
     * 저장해 볼 탐험: 철 검 장착, 물약 1개, 골드 15, HP -3, 시작 위치.
     * 층에는 깨어난 고블린(HP 5), 탄생 턴 7 인 뼈 무더기, 골드 15, 가죽 갑옷. 3턴 진행, 2층 도달, 1처치.
     */
    private static GameContext sampleRun() {
        Player player = new Player("용사", Job.WARRIOR);
        player.getInventory().add(ItemFactory.createById("IRON_SWORD"));
        player.equip(player.getInventory().get(0));
        player.getInventory().add(ItemFactory.createById("HP_POTION_S"));
        player.addGold(15);
        player.takeDamage(3);
        Floor floor = DungeonGenerator.createTestFloor();
        player.setPos(floor.getStartPos());
        Monster goblin = MonsterFactory.create(MonsterType.GOBLIN, new Position(1, 1));
        goblin.getStats().setHp(5);
        goblin.wakeUp();
        floor.addMonster(goblin);
        floor.addMonster(MonsterFactory.create(MonsterType.PILE_OF_BONES, new Position(2, 1), 7));
        floor.addItem(FloorItem.gold(15, new Position(3, 1)));
        floor.addItem(FloorItem.of(ItemFactory.createById("LEATHER_ARMOR"), new Position(4, 1)));

        RunResult result = new RunResult();
        result.reachFloor(2);
        result.addKill();
        GameContext run = new GameContext(player, floor, result);
        run.nextTurn();
        run.nextTurn();
        run.nextTurn();
        return run;
    }

    private static void checkRunResultRestore() {
        RunResult r = RunResult.restore(3, 9);
        expect("RunResult.restore 층", r.getDeepestFloor(), 3);
        expect("RunResult.restore 처치", r.getKills(), 9);
        r.reachFloor(2);
        expect("복원 후 얕은 층은 무시", r.getDeepestFloor(), 3);
        expectThrows("층 0", IllegalArgumentException.class, () -> RunResult.restore(0, 0));
        expectThrows("처치 음수", IllegalArgumentException.class, () -> RunResult.restore(1, -1));
    }

    /** 파편 200, 탐험 6회, 최고 3층, 클리어 1, 처치 71, 마법사 해금, 강화 몇 개. */
    private static Profile sampleProfile() {
        return Profile.restore(200, 6, 3, 1, 71, EnumSet.of(Job.WARRIOR, Job.MAGE),
                Map.of(UpgradeType.MAX_HP, 2, UpgradeType.ATTACK, 1, UpgradeType.UNLOCK_MAGE, 1));
    }

    private static Profile roundTrip(SaveManager m, Profile p) {
        m.save(3, SlotData.profileOnly(p));
        return m.load(3).profile();
    }

    private static void expectSameProfile(String name, Profile actual, Profile expected) {
        expect(name + " 파편", actual.getShards(), expected.getShards());
        expect(name + " 탐험 수", actual.getTotalRuns(), expected.getTotalRuns());
        expect(name + " 최고 층", actual.getBestFloor(), expected.getBestFloor());
        expect(name + " 클리어", actual.getClears(), expected.getClears());
        expect(name + " 누적 처치", actual.getTotalKills(), expected.getTotalKills());
        for (Job job : Job.values()) {
            expect(name + " 해금 " + job, actual.isUnlocked(job), expected.isUnlocked(job));
        }
        for (UpgradeType t : UpgradeType.values()) {
            expect(name + " 강화 " + t, actual.getLevel(t), expected.getLevel(t));
        }
    }

    private static void expectBroken(SaveManager m, String name, List<String> lines) {
        m.writeLines(1, lines);
        expectThrows(name, SaveLoadException.class, () -> m.load(1));
    }

    private static void expectBrokenRun(SaveManager m, String name, List<String> lines) {
        m.writeLines(1, lines);
        expectThrows("탐험 손상: " + name, CorruptedRunException.class, () -> m.load(1))
                .ifPresent(e -> expect("탐험 손상(" + name + ")에도 프로필 유지", e.getProfile().getShards(), 200));
    }

    /** key= 로 시작하는 첫 줄을 replacement 로 바꾼 사본. */
    private static List<String> replaceKey(List<String> lines, String keyPrefix, String replacement) {
        return with(lines, lines.stream().filter(l -> l.startsWith(keyPrefix)).findFirst().orElseThrow(), replacement);
    }

    private static List<String> withoutSavedAt(List<String> lines) {
        return lines.stream().filter(l -> !l.startsWith("savedAt=")).toList();
    }

    /** lines 에서 target 줄 하나를 replacement 로 바꾼 사본. */
    private static List<String> with(List<String> lines, String target, String replacement) {
        List<String> copy = new ArrayList<>(lines);
        int index = copy.indexOf(target);
        if (index < 0) {
            throw new IllegalArgumentException("바꿀 줄이 없습니다: " + target);
        }
        copy.set(index, replacement);
        return copy;
    }

    /** 케이스 하나를 새 임시 폴더에서 실행하고, 끝나면 폴더를 통째로 지운다. */
    private static void withTempDir(TempDirCase check) throws IOException {
        Path root = Files.createTempDirectory("save-manager-check");
        try {
            check.run(root);
        } finally {
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path p : paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                    Files.deleteIfExists(p);
                }
            }
        }
    }

    @FunctionalInterface
    private interface TempDirCase {
        void run(Path root) throws IOException;
    }
}
