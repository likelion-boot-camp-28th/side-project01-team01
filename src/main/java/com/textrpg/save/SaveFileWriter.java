package com.textrpg.save;

import java.util.ArrayList;
import java.util.List;

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

    private final List<String> lines = new ArrayList<>();

    /** 파일 맨 위나 섹션 사이에 {@code # 설명} 주석 줄을 넣는다. */
    public void comment(String text) {
        // TODO(A)
        throw new UnsupportedOperationException("SaveFileWriter.comment 는 아직 구현되지 않았습니다.");
    }

    /**
     * 새 섹션을 시작한다. 이후 put() 은 이 섹션에 들어간다.
     *
     * @throws IllegalArgumentException 이름이 비었거나 [ ] 가 들어 있을 때
     */
    public void section(String name) {
        // TODO(A)
        throw new UnsupportedOperationException("SaveFileWriter.section 은 아직 구현되지 않았습니다.");
    }

    /**
     * key=value 한 줄을 넣는다. 같은 key 를 여러 번 넣으면 반복 key 가 된다 (item=, monster=).
     *
     * @throws IllegalStateException    section() 을 한 번도 부르지 않았을 때
     * @throws IllegalArgumentException key 가 비었거나 = 가 들어 있을 때, value 에 줄바꿈이 있을 때
     */
    public void put(String key, String value) {
        // TODO(A)
        throw new UnsupportedOperationException("SaveFileWriter.put 은 아직 구현되지 않았습니다.");
    }

    /** 정수 값을 넣는다. put(key, String.valueOf(value)) 와 같다. */
    public void put(String key, int value) {
        put(key, String.valueOf(value));
    }

    /** 지금까지 넣은 줄들. 그대로 Files.write() 에 넘길 수 있다. */
    public List<String> toLines() {
        return List.copyOf(lines);
    }
}
