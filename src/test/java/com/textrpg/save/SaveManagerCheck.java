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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.textrpg.context.GameContext;
import com.textrpg.entity.Job;
import com.textrpg.entity.Player;
import com.textrpg.map.DungeonGenerator;
import com.textrpg.meta.Profile;
import com.textrpg.meta.RunResult;
import com.textrpg.meta.UpgradeType;

/**
 * SaveManager 파일 입출력과 프로필 저장·불러오기(SlotCodec) 확인용. JUnit 대신 main 으로 실행한다 (Team_Rules 2.11).
 * 실패가 하나라도 있으면 종료 코드 1.
 *
 * <p>케이스마다 임시 폴더를 새로 만들고 끝나면 지운다. 실제 saves 폴더는 건드리지 않는다.
 */
public final class SaveManagerCheck {

    private static int passed;
    private static int failed;

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
        withTempDir(SaveManagerCheck::checkSlotSummary);
        withTempDir(SaveManagerCheck::checkBrokenProfile);
        checkRunNotSupportedYet();

        System.out.println("통과 " + passed + " / 실패 " + failed);
        if (failed > 0) {
            System.exit(1);
        }
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

        SaveLoadException missing = expectThrows("없는 파일 읽기", SaveLoadException.class, () -> m.readLines(2));
        if (missing != null) {
            expect("없는 파일 원인", missing.getCause() instanceof NoSuchFileException, true);
            expect("메시지에 파일 이름", missing.getMessage().contains("slot2.sav"), true);
            expect("메시지에 폴더 경로는 없음", missing.getMessage().contains(saveDir.toString()), false);
        }

        // 0xC3 0x28 은 UTF-8 로 해석할 수 없는 바이트 조합
        Files.write(saveDir.resolve("slot1.sav"), new byte[] {'[', 'A', ']', '\n', (byte) 0xC3, 0x28});
        SaveLoadException broken = expectThrows("UTF-8 이 아닌 파일", SaveLoadException.class, () -> m.readLines(1));
        if (broken != null) {
            expect("손상 파일 원인", broken.getCause() instanceof CharacterCodingException, true);
        }
    }

    private static void checkWriteFailures(Path root) throws IOException {
        // 세이브 폴더 자리에 일반 파일이 있으면 폴더를 만들 수 없다
        Path notADir = Files.writeString(root.resolve("saves"), "폴더가 아님");
        SaveManager blocked = new SaveManager(notADir);
        SaveLoadException noDir = expectThrows("폴더를 만들 수 없음", SaveLoadException.class,
                () -> blocked.writeLines(1, List.of("[A]")));
        if (noDir != null) {
            expect("폴더 실패 원인 보관", noDir.getCause() instanceof IOException, true);
            expect("폴더 실패 메시지에 파일 이름", noDir.getMessage().contains("slot1.sav"), true);
        }

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

    private static void checkRunNotSupportedYet() {
        Player player = new Player("테스트", Job.WARRIOR);
        GameContext run = new GameContext(player, DungeonGenerator.createTestFloor(), new RunResult());
        expectThrows("탐험 저장은 2단계 전까지 막음", UnsupportedOperationException.class,
                () -> SlotCodec.encode(SlotData.withRun(Profile.newProfile(), run)));
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

    private static void expect(String name, Object actual, Object expected) {
        if (Objects.equals(actual, expected)) {
            passed++;
        } else {
            failed++;
            System.out.println("[실패] " + name + ": 기대 " + expected + " / 실제 " + actual);
        }
    }

    /**
     * expected 종류의 예외가 나야 통과. 잡은 예외를 돌려주므로 원인·메시지를 이어서 확인할 수 있다.
     *
     * @return 잡은 예외, 예외가 없거나 다른 종류면 null (실패로 집계됨)
     */
    private static <T extends RuntimeException> T expectThrows(String name, Class<T> expected, Runnable action) {
        try {
            action.run();
            failed++;
            System.out.println("[실패] " + name + ": " + expected.getSimpleName() + " 이 나지 않음");
        } catch (RuntimeException e) {
            if (expected.isInstance(e)) {
                passed++;
                return expected.cast(e);
            }
            failed++;
            System.out.println("[실패] " + name + ": 기대 " + expected.getSimpleName()
                    + " / 실제 " + e.getClass().getSimpleName());
        }
        return null;
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
