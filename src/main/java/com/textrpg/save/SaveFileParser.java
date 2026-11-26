package com.textrpg.save;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 세이브 파일의 텍스트를 섹션별 key/value 로 나눈다. 객체로 바꾸는 일은 하지 않는다 (codec 이 맡는다).
 *
 * <p>파싱 규칙 (Project-Plan 2.7):
 * <ul>
 *   <li>각 줄은 앞뒤 공백을 지운 뒤 판단한다. 첫 줄 맨 앞의 BOM(U+FEFF)도 지운다.</li>
 *   <li>{@code #} 으로 시작하는 줄과 빈 줄은 무시한다. 줄 중간의 {@code #} 은 주석이 아니다 (맵의 복도 타일).</li>
 *   <li>{@code [이름]} 은 섹션 시작이다. 이름은 대소문자를 구분한다.</li>
 *   <li>그 외 줄은 첫 {@code =} 를 기준으로 key 와 value 로 나눈다. value 에 {@code =} 가 있어도 되고, 비어 있어도 된다.</li>
 *   <li>같은 key 가 반복되면({@code item=}, {@code monster=}) 순서대로 모두 보관한다.</li>
 * </ul>
 *
 * <p>다음 경우는 손상된 파일로 보고 SaveLoadException 을 던진다. 메시지에 줄 번호(1부터)와 원래 줄을 넣는다.
 * <ul>
 *   <li>섹션이 나오기 전에 key=value 가 있을 때</li>
 *   <li>{@code =} 가 없는 줄, key 가 빈 줄 ({@code =abc})</li>
 *   <li>섹션 이름이 비었을 때 ({@code []}), 같은 섹션이 두 번 나올 때</li>
 * </ul>
 *
 * @see SaveFileWriter 같은 형식으로 쓰는 쪽
 */
public final class SaveFileParser {

    private static final String COMMENT_PREFIX = "#";
    private static final String SECTION_START = "[";
    private static final String SECTION_END = "]";
    private static final char KEY_VALUE_SEPARATOR = '=';
    private static final String BOM = "\uFEFF";

    /** 섹션 이름 → (key → 값 목록). 파일에 나온 순서를 유지하고, 값 목록은 수정할 수 없다. */
    private final Map<String, Map<String, List<String>>> sections;

    private SaveFileParser(Map<String, Map<String, List<String>>> sections) {
        this.sections = sections;
    }

    /**
     * 세이브 파일의 줄들을 읽어 섹션별로 나눈다.
     *
     * @param lines Files.readAllLines() 로 읽은 줄들
     * @return 파싱 결과
     * @throws IllegalArgumentException lines 나 그 안의 줄이 null 일 때
     * @throws SaveLoadException        파일 형식이 잘못됐을 때 (클래스 설명 참고)
     */
    public static SaveFileParser parse(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("lines 가 null 입니다.");
        }
        Map<String, Map<String, List<String>>> parsed = new LinkedHashMap<>();
        Map<String, List<String>> current = null;

        for (int i = 0; i < lines.size(); i++) {
            int lineNumber = i + 1;
            String raw = lines.get(i);
            if (raw == null) {
                throw new IllegalArgumentException(lineNumber + "번째 줄이 null 입니다.");
            }
            String line = (i == 0 ? removeBom(raw) : raw).strip();
            if (line.isEmpty() || line.startsWith(COMMENT_PREFIX)) {
                continue;
            }

            if (line.startsWith(SECTION_START) && line.endsWith(SECTION_END)) {
                String name = line.substring(1, line.length() - 1).strip();
                if (name.isEmpty()) {
                    throw corrupted(lineNumber, raw, "섹션 이름이 비어 있습니다");
                }
                if (parsed.containsKey(name)) {
                    throw corrupted(lineNumber, raw, "[" + name + "] 섹션이 두 번 나옵니다");
                }
                current = new LinkedHashMap<>();
                parsed.put(name, current);
                continue;
            }

            int separator = line.indexOf(KEY_VALUE_SEPARATOR);
            if (separator < 0) {
                throw corrupted(lineNumber, raw, "key=value 형식이 아닙니다");
            }
            if (current == null) {
                throw corrupted(lineNumber, raw, "섹션이 시작되기 전에 값이 있습니다");
            }
            String key = line.substring(0, separator).strip();
            if (key.isEmpty()) {
                throw corrupted(lineNumber, raw, "key 가 비어 있습니다");
            }
            current.computeIfAbsent(key, k -> new ArrayList<>()).add(line.substring(separator + 1));
        }
        return new SaveFileParser(freeze(parsed));
    }

    /** 섹션이 있는지. 예: [RUN] 이 없으면 진행 중인 탐험이 없다. 내용이 없는 섹션도 있는 것으로 본다. */
    public boolean hasSection(String section) {
        return sections.containsKey(section);
    }

    /**
     * 필수 key 의 값. 같은 key 가 여러 번 있으면 첫 번째 값.
     *
     * @return key 의 값
     * @throws SaveLoadException 섹션이나 key 가 없을 때
     */
    public String getString(String section, String key) {
        if (!hasSection(section)) {
            throw new SaveLoadException("[" + section + "] 섹션이 없습니다");
        }
        List<String> values = find(section, key);
        if (values.isEmpty()) {
            throw new SaveLoadException("[" + section + "] " + key + " 이(가) 없습니다");
        }
        return values.get(0);
    }

    /**
     * 필수 key 의 정수 값.
     *
     * @return key 의 값
     * @throws SaveLoadException 섹션이나 key 가 없거나 숫자가 아닐 때
     */
    public int getInt(String section, String key) {
        return toInt(section, key, getString(section, key));
    }

    /**
     * 없어도 되는 key 의 정수 값. 예: [UPGRADES] 에 없는 강화는 0단계.
     *
     * @return key 의 값, 섹션이나 key 가 없으면 defaultValue
     * @throws SaveLoadException key 는 있는데 숫자가 아닐 때
     */
    public int getInt(String section, String key, int defaultValue) {
        List<String> values = find(section, key);
        if (values.isEmpty()) {
            return defaultValue;
        }
        return toInt(section, key, values.get(0));
    }

    /**
     * 반복되는 key 의 값 목록. 예: [INVENTORY] 의 item=.
     *
     * @return 파일에 나온 순서대로의 값들 (수정 불가). 섹션이나 key 가 없으면 빈 리스트
     */
    public List<String> getList(String section, String key) {
        return find(section, key);
    }

    /** 섹션이나 key 가 없으면 빈 리스트. */
    private List<String> find(String section, String key) {
        Map<String, List<String>> keys = sections.get(section);
        if (keys == null) {
            return List.of();
        }
        return keys.getOrDefault(key, List.of());
    }

    private static int toInt(String section, String key, String value) {
        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException e) {
            throw new SaveLoadException("[" + section + "] " + key + " 값이 숫자가 아닙니다: " + value, e);
        }
    }

    private static String removeBom(String line) {
        return line.startsWith(BOM) ? line.substring(BOM.length()) : line;
    }

    /** 파싱이 끝난 값 목록을 수정할 수 없게 바꾼다. getList() 결과를 밖에서 고쳐도 파서는 그대로다. */
    private static Map<String, Map<String, List<String>>> freeze(Map<String, Map<String, List<String>>> parsed) {
        Map<String, Map<String, List<String>>> frozen = new LinkedHashMap<>();
        parsed.forEach((name, keys) -> {
            Map<String, List<String>> copy = new LinkedHashMap<>();
            keys.forEach((key, values) -> copy.put(key, List.copyOf(values)));
            frozen.put(name, copy);
        });
        return frozen;
    }

    private static SaveLoadException corrupted(int lineNumber, String raw, String reason) {
        return new SaveLoadException("세이브 파일 " + lineNumber + "번째 줄: " + reason + " → " + raw);
    }
}
