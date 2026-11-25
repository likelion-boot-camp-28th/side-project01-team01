package com.textrpg.save;

import java.util.Arrays;
import java.util.List;

import com.textrpg.CheckRunner;

import static com.textrpg.CheckRunner.expect;
import static com.textrpg.CheckRunner.expectThrows;
import static com.textrpg.CheckRunner.fail;

/**
 * SaveFileParser 확인용. JUnit 대신 main 으로 실행한다 (Team_Rules 2.11).
 * 실패가 하나라도 있으면 종료 코드 1.
 */
public final class SaveFileParserCheck {

    /** Project-Plan 2.7 의 세이브 파일 예시 (맵 줄은 일부만). */
    private static final List<String> SAMPLE = List.of(
            "# Into the Dark Save File",
            "[META]",
            "version=1",
            "savedAt=2026-11-25T14:30:00",
            "",
            "# ===== 영구 프로필 (사망해도 유지) =====",
            "[PROFILE]",
            "shards=85",
            "unlockedJobs=WARRIOR,MAGE",
            "",
            "[UPGRADES]",
            "# 강화 id=현재 단계",
            "MAX_HP=2",
            "ATTACK=1",
            "",
            "[RUN]",
            "turn=214",
            "",
            "[PLAYER]",
            "name=용사",
            "level=3",
            "armor=",
            "",
            "[INVENTORY]",
            "item=HP_POTION_S",
            "item=SCROLL_TELEPORT",
            "",
            "[IDENTIFY]",
            "HP_POTION_S=탁한 초록 물약,true",
            "",
            "[FLOOR]",
            "level=2",
            "# 타일: W 벽, . 바닥, # 복도, + 문, > 계단",
            "map0=WWWWW..##..+>",
            "",
            "[MONSTERS]",
            "# 종류,x,y,현재HP",
            "monster=GOBLIN,33,2,9",
            "monster=BAT,40,4,6",
            "",
            "[ITEMS]",
            "item=GOLD:15,20,4");

    private SaveFileParserCheck() {
    }

    public static void main(String[] args) {
        checkSample();
        checkIgnoredLines();
        checkValues();
        checkDefaults();
        checkCorruptedFiles();
        checkMissingValues();
        checkImmutable();

        CheckRunner.finish();
    }

    private static void checkSample() {
        SaveFileParser p = SaveFileParser.parse(SAMPLE);
        expect("META version", p.getInt("META", "version"), 1);
        expect("PROFILE shards", p.getInt("PROFILE", "shards"), 85);
        expect("쉼표가 있는 값", p.getString("PROFILE", "unlockedJobs"), "WARRIOR,MAGE");
        expect("가운데 공백이 있는 값", p.getString("IDENTIFY", "HP_POTION_S"), "탁한 초록 물약,true");
        expect("한글 값", p.getString("PLAYER", "name"), "용사");
        expect("빈 값", p.getString("PLAYER", "armor"), "");
        expect("줄 중간의 # 은 주석이 아님", p.getString("FLOOR", "map0"), "WWWWW..##..+>");
        expect("같은 key 가 다른 섹션에 있음", p.getInt("FLOOR", "level"), 2);
        expect("반복 key", p.getList("MONSTERS", "monster"), List.of("GOBLIN,33,2,9", "BAT,40,4,6"));
        expect("반복 key 의 getString 은 첫 번째", p.getString("INVENTORY", "item"), "HP_POTION_S");
        expect("RUN 있음", p.hasSection("RUN"), true);
    }

    private static void checkIgnoredLines() {
        SaveFileParser p = SaveFileParser.parse(List.of(
                "\uFEFF# BOM 이 붙은 첫 줄",
                "   ",
                "  # 앞에 공백이 있는 주석",
                "  [ PLAYER ]  ",
                "  level = 3  ",
                "[EMPTY]"));
        expect("앞뒤 공백이 있는 섹션 이름", p.hasSection("PLAYER"), true);
        expect("key 와 value 앞뒤 공백", p.getInt("PLAYER", "level"), 3);
        expect("내용 없는 섹션", p.hasSection("EMPTY"), true);
        expect("섹션 이름은 대소문자 구분", p.hasSection("player"), false);
        expect("빈 파일", SaveFileParser.parse(List.of()).hasSection("META"), false);

        SaveFileParser bomSection = SaveFileParser.parse(List.of("\uFEFF[META]", "version=1"));
        expect("BOM 이 붙은 섹션 줄", bomSection.getInt("META", "version"), 1);
    }

    private static void checkValues() {
        SaveFileParser p = SaveFileParser.parse(List.of("[A]", "a=b=c", "n=-5", "m= 7 "));
        expect("첫 = 기준으로 나눔", p.getString("A", "a"), "b=c");
        expect("음수", p.getInt("A", "n"), -5);
        expect("숫자 앞뒤 공백", p.getInt("A", "m"), 7);
    }

    private static void checkDefaults() {
        SaveFileParser p = SaveFileParser.parse(List.of("[PROFILE]", "shards=0", "[UPGRADES]", "MAX_HP=2"));
        expect("RUN 없음", p.hasSection("RUN"), false);
        expect("있는 강화", p.getInt("UPGRADES", "MAX_HP", 0), 2);
        expect("없는 강화는 기본값", p.getInt("UPGRADES", "ATTACK", 0), 0);
        expect("없는 섹션은 기본값", p.getInt("RUN", "turn", 7), 7);
        expect("없는 섹션의 목록", p.getList("INVENTORY", "item"), List.of());
        expect("없는 key 의 목록", p.getList("PROFILE", "item"), List.of());
        expectThrows("기본값이 있어도 숫자가 아니면 오류", SaveLoadException.class,
                () -> SaveFileParser.parse(List.of("[U]", "MAX_HP=abc")).getInt("U", "MAX_HP", 0));
    }

    private static void checkCorruptedFiles() {
        expectCorrupted("섹션 전에 값", List.of("version=1", "[META]"), "1번째 줄");
        expectCorrupted("= 이 없는 줄", List.of("[PLAYER]", "level 3"), "2번째 줄");
        expectCorrupted("빈 key", List.of("[PLAYER]", "=abc"), "2번째 줄");
        expectCorrupted("빈 섹션 이름", List.of("# 주석", "[]"), "2번째 줄");
        expectCorrupted("공백뿐인 섹션 이름", List.of("[ ]"), "1번째 줄");
        expectCorrupted("중복 섹션", List.of("[PLAYER]", "level=1", "", "[PLAYER]"), "4번째 줄");
        expectCorrupted("닫히지 않은 섹션", List.of("[PLAYER"), "1번째 줄");

        expectThrows("lines 가 null", IllegalArgumentException.class, () -> SaveFileParser.parse(null));
        expectThrows("null 줄", IllegalArgumentException.class, () -> SaveFileParser.parse(Arrays.asList("[A]", null)));
    }

    private static void checkMissingValues() {
        SaveFileParser p = SaveFileParser.parse(List.of("[PLAYER]", "level=abc"));
        expectThrows("없는 섹션", SaveLoadException.class, () -> p.getString("RUN", "turn"));
        expectThrows("없는 key", SaveLoadException.class, () -> p.getString("PLAYER", "name"));
        expectThrows("없는 key 의 getInt", SaveLoadException.class, () -> p.getInt("PLAYER", "exp"));
        expectThrows("숫자가 아님", SaveLoadException.class, () -> p.getInt("PLAYER", "level"));

        try {
            p.getInt("PLAYER", "level");
        } catch (SaveLoadException e) {
            expect("오류 메시지 형식", e.getMessage(), "[PLAYER] level 값이 숫자가 아닙니다: abc");
            expect("원인 예외 보관", e.getCause() instanceof NumberFormatException, true);
        }
    }

    private static void checkImmutable() {
        SaveFileParser p = SaveFileParser.parse(List.of("[INVENTORY]", "item=A"));
        List<String> items = p.getList("INVENTORY", "item");
        expectThrows("getList 결과는 수정할 수 없음", UnsupportedOperationException.class, () -> items.add("B"));
        expect("파서 내부는 그대로", p.getList("INVENTORY", "item"), List.of("A"));
    }

    /** SaveLoadException 이 나고, 메시지에 줄 번호가 들어 있는지. */
    private static void expectCorrupted(String name, List<String> lines, String lineLabel) {
        try {
            SaveFileParser.parse(lines);
            fail(name + ": SaveLoadException 이 나지 않음");
        } catch (SaveLoadException e) {
            expect(name + " 메시지에 줄 번호", e.getMessage().contains(lineLabel), true);
        }
    }
}
