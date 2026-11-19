package com.textrpg.save;

import java.util.List;
import java.util.Map;

/**
 * 세이브 파일의 텍스트를 섹션별 key/value 로 나눈다. 객체로 바꾸는 일은 하지 않는다 (codec 이 맡는다).
 *
 * <p>파싱 규칙 (Project-Plan 2.7):
 * <ul>
 *   <li>{@code #} 으로 시작하는 줄과 빈 줄은 무시한다.</li>
 *   <li>{@code [이름]} 은 섹션 시작이다.</li>
 *   <li>그 외 줄은 첫 {@code =} 를 기준으로 key 와 value 로 나눈다. value 에 {@code =} 가 있어도 된다.</li>
 *   <li>같은 key 가 반복되면({@code item=}, {@code monster=}) 순서대로 모두 보관한다.</li>
 * </ul>
 *
 * @see SaveFileWriter 같은 형식으로 쓰는 쪽
 */
public final class SaveFileParser {

    /** 섹션 이름 → (key → 값 목록). 파일에 나온 순서를 유지한다. */
    private final Map<String, Map<String, List<String>>> sections;

    private SaveFileParser(Map<String, Map<String, List<String>>> sections) {
        this.sections = sections;
    }

    /**
     * 세이브 파일의 줄들을 읽어 섹션별로 나눈다.
     *
     * @param lines Files.readAllLines() 로 읽은 줄들
     * @return 파싱 결과
     * @throws SaveLoadException 섹션 밖에 key=value 가 있거나, = 가 없는 줄이 있을 때
     */
    public static SaveFileParser parse(List<String> lines) {
        // TODO(A): 파싱 규칙대로 구현
        throw new UnsupportedOperationException("SaveFileParser.parse 는 아직 구현되지 않았습니다.");
    }

    /** 섹션이 있는지. 예: [RUN] 이 없으면 진행 중인 탐험이 없다. */
    public boolean hasSection(String section) {
        // TODO(A)
        throw new UnsupportedOperationException("SaveFileParser.hasSection 은 아직 구현되지 않았습니다.");
    }

    /**
     * 필수 key 의 값. 같은 key 가 여러 번 있으면 첫 번째 값.
     *
     * @return key 의 값
     * @throws SaveLoadException 섹션이나 key 가 없을 때
     */
    public String getString(String section, String key) {
        // TODO(A)
        throw new UnsupportedOperationException("SaveFileParser.getString 은 아직 구현되지 않았습니다.");
    }

    /**
     * 필수 key 의 정수 값.
     *
     * @return key 의 값
     * @throws SaveLoadException 섹션이나 key 가 없거나 숫자가 아닐 때
     */
    public int getInt(String section, String key) {
        // TODO(A)
        throw new UnsupportedOperationException("SaveFileParser.getInt 는 아직 구현되지 않았습니다.");
    }

    /**
     * 없어도 되는 key 의 정수 값. 예: [UPGRADES] 에 없는 강화는 0단계.
     *
     * @return key 의 값, 섹션이나 key 가 없으면 defaultValue
     * @throws SaveLoadException key 는 있는데 숫자가 아닐 때
     */
    public int getInt(String section, String key, int defaultValue) {
        // TODO(A)
        throw new UnsupportedOperationException("SaveFileParser.getInt 는 아직 구현되지 않았습니다.");
    }

    /**
     * 반복되는 key 의 값 목록. 예: [INVENTORY] 의 item=.
     *
     * @return 파일에 나온 순서대로의 값들. 섹션이나 key 가 없으면 빈 리스트
     */
    public List<String> getList(String section, String key) {
        // TODO(A)
        throw new UnsupportedOperationException("SaveFileParser.getList 는 아직 구현되지 않았습니다.");
    }
}
