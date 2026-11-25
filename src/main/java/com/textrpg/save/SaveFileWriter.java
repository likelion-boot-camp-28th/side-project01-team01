package com.textrpg.save;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * SaveFileParser 가 읽을 수 있는 형식으로 세이브 파일의 줄들을 만든다. 파일에 쓰는 일은 SaveManager 가 맡는다.
 *
 * <pre>
 * SaveFileWriter writer = new SaveFileWriter();
 * writer.section("PLAYER");
 * writer.put("name", "용사");
 * writer.put("level", 3);
 * List&lt;String&gt; lines = writer.toLines();   // [PLAYER], name=용사, level=3
 * </pre>
 *
 * @see SaveFileParser 같은 형식을 읽는 쪽
 */
public final class SaveFileWriter {

    private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9_]+");
    private final List<String> lines = new ArrayList<>();
    private final Set<String> sectionNames = new HashSet<>();

    /**
     * 파일 맨 위나 섹션 사이에 {@code # 설명} 주석 줄을 넣는다.
     *
     * @throws IllegalArgumentException text 가 null 이거나 줄바꿈(\n, \r)이 있을 때
     */
    public void comment(String text) {
        if (text == null || hasLineBreak(text)) {
            throw new IllegalArgumentException("text 가 null이거나 줄바꿈이 있습니다: " + text);
        }
        lines.add("# " + text);
    }

    /**
     * 새 섹션을 시작한다. 이후 put() 은 이 섹션에 들어간다.
     *
     * @throws IllegalArgumentException 이름이 영문, 숫자, _ 가 아니거나 이미 쓴 섹션일 때
     */
    public void section(String name) {
        if (name == null || !NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("섹션 이름이 유효하지 않습니다: " + name);
        }
        if (!sectionNames.add(name)) {
            throw new IllegalArgumentException("이미 존재하는 섹션 이름입니다: " + name);
        }
        lines.add("[" + name + "]");
    }

    /**
     * key=value 한 줄을 넣는다. 같은 key 를 여러 번 넣으면 반복 key 가 된다 (item=, monster=).
     *
     * @throws IllegalStateException    section() 을 한 번도 부르지 않았을 때
     * @throws IllegalArgumentException key가 영문, 숫자, _가 아닐 때, value가 null이거나 줄바꿈·앞뒤 공백이 있을 때
     */
    public void put(String key, String value) {
        if (sectionNames.isEmpty()) {
            throw new IllegalStateException("section() 을 한 번도 부르지 않았습니다: " + key);
        }
        if (key == null || !NAME_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException("key 가 null 이거나 영문, 숫자, _가 아닌 값이 들어 있습니다: " + key + "=" + value);
        }
        if (value == null || hasLineBreak(value) || !value.equals(value.strip())) {
            throw new IllegalArgumentException("[" + key + "] value 가 null 이거나 줄바꿈·앞뒤 공백이 있습니다: " + value);
        }
        lines.add(key + "=" + value);
    }

    /** 정수 값을 넣는다. put(key, String.valueOf(value)) 와 같다. */
    public void put(String key, int value) {
        put(key, String.valueOf(value));
    }

    /** 지금까지 넣은 줄들. 그대로 Files.write() 에 넘길 수 있다. */
    public List<String> toLines() {
        return List.copyOf(lines);
    }

    /** 입력된 값의 줄바꿈 검사를 진행한다. */
    private static boolean hasLineBreak(String text) {
        return text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0;
    }
}
