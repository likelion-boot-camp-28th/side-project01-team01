package com.textrpg.save;

import java.util.List;

/**
 * SlotData 와 세이브 파일 줄을 서로 바꾼다. 파일을 읽고 쓰는 일은 SaveManager 가, 줄을 섹션으로 나누는 일은
 * SaveFileParser 가 맡는다.
 */
final class SlotCodec {

    private SlotCodec() {
    }

    /** [META], [PROFILE], [UPGRADES] 와 (탐험이 있으면) 탐험 섹션을 줄로 만든다. */
    static List<String> encode(SlotData data) {
        // TODO(A): SaveFileWriter 로 [META] version·savedAt, [PROFILE], [UPGRADES] 쓰기. 탐험 섹션은 2단계
        throw new UnsupportedOperationException("SlotCodec.encode 는 아직 구현되지 않았습니다.");
    }

    /**
     * 파싱된 세이브 파일을 SlotData 로 바꾼다.
     *
     * @throws SaveLoadException 버전이 다르거나 필수 값이 없을 때
     */
    static SlotData decode(SaveFileParser parser) {
        // TODO(A): version 확인 → Profile 복원(B 와 합의한 메서드) → [RUN] 없으면 SlotData.profileOnly
        throw new UnsupportedOperationException("SlotCodec.decode 는 아직 구현되지 않았습니다.");
    }
}
