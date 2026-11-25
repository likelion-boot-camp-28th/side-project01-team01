package com.textrpg.save;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.textrpg.CheckRunner;

import static com.textrpg.CheckRunner.expect;
import static com.textrpg.CheckRunner.expectThrows;

/**
 * SaveFileWriter 확인용. JUnit 대신 main 으로 실행한다 (Team_Rules 2.11).
 * 실패가 하나라도 있으면 종료 코드 1.
 *
 * <p>가장 중요한 확인은 왕복이다: Writer 가 만든 줄을 파일에 쓰고 다시 읽어 SaveFileParser 로 해석했을 때 같은 값이어야 한다.
 */
public final class SaveFileWriterCheck {

    private SaveFileWriterCheck() {
    }

    public static void main(String[] args) throws IOException {
        checkFormat();
        checkRoundTripInMemory();
        checkRoundTripThroughFile();
        checkInvalidSection();
        checkInvalidPut();
        checkInvalidComment();
        checkRejectedCallsWriteNothing();
        checkToLinesCopy();

        CheckRunner.finish();
    }

    private static void checkFormat() {
        SaveFileWriter w = new SaveFileWriter();
        w.comment("Into the Dark Save File");
        w.section("PLAYER");
        w.put("name", "용사");
        w.put("level", 3);
        w.put("gold", -5);
        w.section("INVENTORY");
        w.put("item", "HP_POTION_S");
        w.put("item", "SCROLL_TELEPORT");
        w.comment("");
        expect("출력 형식", w.toLines(), List.of(
                "# Into the Dark Save File",
                "[PLAYER]",
                "name=용사",
                "level=3",
                "gold=-5",
                "[INVENTORY]",
                "item=HP_POTION_S",
                "item=SCROLL_TELEPORT",
                "# "));
        expect("새 Writer 는 빈 목록", new SaveFileWriter().toLines(), List.of());
    }

    private static void checkRoundTripInMemory() {
        verifySample(SaveFileParser.parse(writeSample().toLines()), "메모리 왕복");
    }

    /** Files.write → readAllLines 를 거친다. \r 같은 줄바꿈 문제는 파일을 거쳐야 드러난다. */
    private static void checkRoundTripThroughFile() throws IOException {
        Path file = Files.createTempFile("save-writer-check", ".sav");
        try {
            Files.write(file, writeSample().toLines(), StandardCharsets.UTF_8);
            verifySample(SaveFileParser.parse(Files.readAllLines(file, StandardCharsets.UTF_8)), "파일 왕복");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    /** Project-Plan 2.7 의 세이브 예시와 같은 구성 (맵 줄은 일부만). */
    private static SaveFileWriter writeSample() {
        SaveFileWriter w = new SaveFileWriter();
        w.comment("Into the Dark Save File");
        w.section("META");
        w.put("version", 1);
        w.put("savedAt", "2026-11-25T14:30:00");
        w.comment("===== 영구 프로필 (사망해도 유지) =====");
        w.section("PROFILE");
        w.put("shards", 85);
        w.put("unlockedJobs", "WARRIOR,MAGE");
        w.section("UPGRADES");
        w.comment("강화 id=현재 단계");
        w.put("MAX_HP", 2);
        w.section("RUN");
        w.put("turn", 214);
        w.section("PLAYER");
        w.put("name", "용사");
        w.put("armor", "");
        w.section("IDENTIFY");
        w.put("HP_POTION_S", "탁한 초록 물약,true");
        w.section("FLOOR");
        w.comment("타일: W 벽, . 바닥, # 복도, + 문, > 계단");
        w.put("map0", "#WWW..##..+>");
        w.put("formula", "a=b=c");
        w.section("MONSTERS");
        w.put("monster", "GOBLIN,33,2,9");
        w.put("monster", "BAT,40,4,6");
        return w;
    }

    private static void verifySample(SaveFileParser p, String label) {
        expect(label + ": version", p.getInt("META", "version"), 1);
        expect(label + ": savedAt", p.getString("META", "savedAt"), "2026-11-25T14:30:00");
        expect(label + ": shards", p.getInt("PROFILE", "shards"), 85);
        expect(label + ": 쉼표 값", p.getString("PROFILE", "unlockedJobs"), "WARRIOR,MAGE");
        expect(label + ": 섹션 안의 주석은 값이 아님", p.getList("UPGRADES", "강화 id"), List.of());
        expect(label + ": 강화", p.getInt("UPGRADES", "MAX_HP", 0), 2);
        expect(label + ": RUN 있음", p.hasSection("RUN"), true);
        expect(label + ": 한글 값", p.getString("PLAYER", "name"), "용사");
        expect(label + ": 빈 값", p.getString("PLAYER", "armor"), "");
        expect(label + ": 가운데 공백", p.getString("IDENTIFY", "HP_POTION_S"), "탁한 초록 물약,true");
        expect(label + ": # 으로 시작하는 값", p.getString("FLOOR", "map0"), "#WWW..##..+>");
        expect(label + ": = 이 있는 값", p.getString("FLOOR", "formula"), "a=b=c");
        expect(label + ": 반복 key", p.getList("MONSTERS", "monster"), List.of("GOBLIN,33,2,9", "BAT,40,4,6"));
    }

    private static void checkInvalidSection() {
        expectThrows("섹션 이름 null", IllegalArgumentException.class, () -> new SaveFileWriter().section(null));
        expectThrows("빈 섹션 이름", IllegalArgumentException.class, () -> new SaveFileWriter().section(""));
        expectThrows("공백이 있는 섹션 이름", IllegalArgumentException.class, () -> new SaveFileWriter().section("MY RUN"));
        expectThrows("[ ] 가 있는 섹션 이름", IllegalArgumentException.class, () -> new SaveFileWriter().section("[RUN]"));
        expectThrows("# 으로 시작하는 섹션 이름", IllegalArgumentException.class, () -> new SaveFileWriter().section("#RUN"));
        expectThrows("중복 섹션", IllegalArgumentException.class, () -> {
            SaveFileWriter w = new SaveFileWriter();
            w.section("PLAYER");
            w.section("RUN");
            w.section("PLAYER");
        });
    }

    private static void checkInvalidPut() {
        expectThrows("section 전에 put", IllegalStateException.class, () -> new SaveFileWriter().put("k", "v"));
        expectThrows("section 전에 put(int)", IllegalStateException.class, () -> new SaveFileWriter().put("k", 1));
        expectThrows("section 전에 주석만 있음", IllegalStateException.class, () -> {
            SaveFileWriter w = new SaveFileWriter();
            w.comment("헤더");
            w.put("k", "v");
        });

        expectPutRejected("key null", null, "v");
        expectPutRejected("빈 key", "", "v");
        expectPutRejected("= 이 있는 key", "a=b", "v");
        expectPutRejected("# 으로 시작하는 key", "#a", "v");
        expectPutRejected("[ 로 시작하는 key", "[a", "b]");
        expectPutRejected("앞 공백 key", " level", "3");
        expectPutRejected("줄바꿈 key", "a\nb", "v");

        expectPutRejected("value null", "k", null);
        expectPutRejected("value 에 \\n", "k", "a\nb=1");
        expectPutRejected("value 에 \\r", "k", "a\rb=1");
        expectPutRejected("value 앞 공백", "name", " 용사");
        expectPutRejected("value 끝 공백", "name", "용사  ");
    }

    private static void checkInvalidComment() {
        expectThrows("주석 null", IllegalArgumentException.class, () -> new SaveFileWriter().comment(null));
        expectThrows("주석에 \\n", IllegalArgumentException.class, () -> new SaveFileWriter().comment("메모\nshards=9999"));
        expectThrows("주석에 \\r", IllegalArgumentException.class, () -> new SaveFileWriter().comment("메모\rshards=9999"));
    }

    /** 거부된 호출은 아무 줄도 남기지 않고, 실패한 section() 은 섹션으로 등록되지 않는다. */
    private static void checkRejectedCallsWriteNothing() {
        SaveFileWriter w = new SaveFileWriter();
        ignore(() -> w.section("MY RUN"));
        expectThrows("실패한 section 뒤의 put", IllegalStateException.class, () -> w.put("k", "v"));

        w.section("A");
        w.put("k", "v");
        ignore(() -> w.section("A"));
        ignore(() -> w.put("#k", "v"));
        ignore(() -> w.put("k", "v "));
        ignore(() -> w.comment("a\nb"));
        expect("거부된 호출은 줄을 남기지 않음", w.toLines(), List.of("[A]", "k=v"));
    }

    private static void checkToLinesCopy() {
        SaveFileWriter w = new SaveFileWriter();
        w.section("A");
        List<String> before = w.toLines();
        expectThrows("toLines 결과는 수정할 수 없음", UnsupportedOperationException.class, () -> before.add("x=1"));
        w.put("k", "v");
        expect("이미 받은 목록은 그대로", before, List.of("[A]"));
        expect("새로 받은 목록에는 반영", w.toLines(), List.of("[A]", "k=v"));
    }

    private static void expectPutRejected(String name, String key, String value) {
        expectThrows(name, IllegalArgumentException.class, () -> {
            SaveFileWriter w = new SaveFileWriter();
            w.section("A");
            w.put(key, value);
        });
    }

    /** 예외가 나는 호출을 실행만 한다. 예외 확인은 다른 메서드에서 한다. */
    private static void ignore(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException e) {
            // 거부되는 것이 정상. 여기서는 거부된 뒤의 상태만 확인한다.
        }
    }
}
