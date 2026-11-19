package com.textrpg.save;

import java.nio.file.Path;
import java.util.List;

/**
 * 세이브 슬롯 파일(saves/slot1.sav ~ slot3.sav)을 읽고 쓴다.
 * 1차 스켈레톤은 파일 입출력 부분만 담는다. SlotData 를 다루는 save / load / clearRun / getSlotSummary 는
 * B·C·D 스켈레톤이 develop 에 들어간 뒤 2차 PR 에서 추가한다.
 */
public class SaveManager {

    public static final int SLOT_COUNT = 3;

    private static final String SLOT_FILE_FORMAT = "slot%d.sav";
    private static final String TEMP_SUFFIX = ".tmp";

    private final Path saveDir;

    /**
     * @param saveDir 세이브 폴더. 게임은 Path.of("saves"), 확인용 테스트는 Files.createTempDirectory() 를 넘긴다.
     *                폴더가 없으면 처음 저장할 때 만든다.
     */
    public SaveManager(Path saveDir) {
        this.saveDir = saveDir;
    }

    /**
     * 슬롯에 세이브 파일이 있는지.
     *
     * @param slot 1 ~ SLOT_COUNT
     * @throws IllegalArgumentException 슬롯 번호가 범위를 벗어날 때
     */
    public boolean exists(int slot) {
        // TODO(A)
        throw new UnsupportedOperationException("SaveManager.exists 는 아직 구현되지 않았습니다.");
    }

    // TODO(A, 2차): save(int slot, SlotData data), load(int slot), clearRun(int slot, Profile updated),
    //  getSlotSummary(int slot) — SlotData 가 Profile(B), Player(B), Floor(D) 를 쓰므로 B·C·D 스켈레톤 이후에 추가

    /**
     * 슬롯 번호를 파일 경로로 바꾼다. 예: 1 → saves/slot1.sav
     *
     * @throws IllegalArgumentException 슬롯 번호가 1 ~ SLOT_COUNT 를 벗어날 때
     */
    private Path slotFile(int slot) {
        if (slot < 1 || slot > SLOT_COUNT) {
            throw new IllegalArgumentException("슬롯 번호는 1 ~ " + SLOT_COUNT + " 입니다: " + slot);
        }
        return saveDir.resolve(String.format(SLOT_FILE_FORMAT, slot));
    }

    /**
     * 파일을 UTF-8 로 읽는다.
     *
     * @throws SaveLoadException 파일이 없거나 읽지 못할 때
     */
    private List<String> readLines(Path file) {
        // TODO(A): Files.readAllLines(file, UTF_8), IOException 은 SaveLoadException 으로 감싼다
        throw new UnsupportedOperationException("SaveManager.readLines 는 아직 구현되지 않았습니다.");
    }

    /**
     * 임시 파일(slotN.sav.tmp)에 다 쓴 뒤 원래 파일과 한 번에 바꾼다. 쓰는 도중 꺼져도 기존 세이브는 남는다.
     * ATOMIC_MOVE 를 지원하지 않는 파일 시스템이면 REPLACE_EXISTING 으로만 다시 시도한다.
     *
     * @throws SaveLoadException 쓰기나 교체에 실패했을 때
     */
    private void writeAtomically(Path file, List<String> lines) {
        // TODO(A): saveDir 생성 → file + TEMP_SUFFIX 에 쓰기 → Files.move(REPLACE_EXISTING, ATOMIC_MOVE)
        throw new UnsupportedOperationException("SaveManager.writeAtomically 는 아직 구현되지 않았습니다.");
    }
}
