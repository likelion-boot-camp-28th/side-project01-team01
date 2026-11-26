package com.textrpg.save;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import com.textrpg.meta.Profile;

/**
 * 세이브 슬롯 파일(saves/slot1.sav ~ slot3.sav)을 읽고 쓴다.
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
        return Files.isRegularFile(slotFile(slot));
    }

    /**
     * 슬롯에 저장한다. 임시 파일에 쓴 뒤 교체하므로 도중에 꺼져도 기존 세이브는 남는다.
     *
     * @throws SaveLoadException 쓰기에 실패했을 때
     */
    public void save(int slot, SlotData data) {
        writeLines(slot, SlotCodec.encode(data));
    }

    /**
     * 슬롯을 읽는다.
     *
     * @throws SaveLoadException 파일이 없거나 손상되었을 때
     */
    public SlotData load(int slot) {
        return SlotCodec.decode(SaveFileParser.parse(readLines(slot)));
    }

    /**
     * 진행 중인 탐험이 있는지. 객체를 복원하지 않고 [RUN] 섹션이 있는지만 본다 (슬롯 메뉴의 "이어하기" 표시용).
     * 탐험 섹션이 손상됐는지는 확인하지 않는다. 그건 불러올 때(load) 알 수 있다.
     *
     * @return 파일이 없으면 false
     * @throws SaveLoadException 파일을 읽을 수 없거나 형식이 깨졌을 때
     */
    public boolean hasRun(int slot) {
        return exists(slot) && SaveFileParser.parse(readLines(slot)).hasSection(SlotCodec.RUN);
    }

    /** 사망·클리어 후 탐험 섹션을 지우고 갱신된 프로필만 저장한다 (영구 죽음). */
    public void clearRun(int slot, Profile updated) {
        save(slot, SlotData.profileOnly(updated));
    }

    /**
     * 메인 메뉴에 보여 줄 슬롯 한 줄 요약. 메뉴는 항상 그려져야 하므로 예외를 던지지 않는다.
     * 예: "파편 85 | 최고 3층 | 탐험 6회 | 진행 중: 용사 Lv.3 전사 지하 2층"
     *
     * @return 파일이 없으면 "(새 슬롯)", 읽을 수 없으면 "(손상된 세이브)", 탐험만 손상되었으면 프로필 요약 뒤에 " | 탐험 기록 손상"
     * @throws IllegalArgumentException 슬롯 번호가 범위를 벗어날 때
     */
    public String getSlotSummary(int slot) {
        if (!exists(slot)) {
            return "(새 슬롯)";
        }
        SlotData data;
        try {
            data = load(slot);
        } catch (CorruptedRunException e) {
            return profileSummary(e.getProfile()) + " | 탐험 기록 손상";
        } catch (SaveLoadException e) {
            return "(손상된 세이브)";
        }
        String summary = profileSummary(data.profile());
        return data.run()
                .map(run -> summary + " | 진행 중: " + run.getPlayer().getName() + " Lv." + run.getPlayer().getLevel()
                        + " " + run.getPlayer().getJob().getDisplayName() + " 지하 " + run.getFloor().getLevel() + "층")
                .orElse(summary);
    }

    private static String profileSummary(Profile p) {
        String bestFloor = p.getBestFloor() == 0 ? "-" : p.getBestFloor() + "층";
        return "파편 " + p.getShards() + " | 최고 " + bestFloor + " | 탐험 " + p.getTotalRuns() + "회";
    }

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
     * 슬롯 파일을 UTF-8 로 읽는다.
     *
     * @throws IllegalArgumentException 슬롯 번호가 범위를 벗어날 때
     * @throws SaveLoadException 파일이 없거나 읽지 못할 때
     */
    List<String> readLines(int slot) {
        Path file = slotFile(slot);
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            throw new SaveLoadException("세이브 파일이 없습니다: " + file.getFileName(), e);
        } catch (CharacterCodingException e) {
            throw new SaveLoadException("세이브 파일이 손상되었습니다: " + file.getFileName(), e);
        } catch (IOException e) {
            throw new SaveLoadException("세이브 파일을 읽을 수 없습니다: " + file.getFileName(), e);
        }
    }

    /**
     * 임시 파일(slotN.sav.tmp)에 다 쓴 뒤 원래 파일과 한 번에 바꾼다. 쓰는 도중 꺼져도 기존 세이브는 남는다.
     * ATOMIC_MOVE 를 지원하지 않는 파일 시스템이면 REPLACE_EXISTING 으로만 다시 시도한다.
     *
     * @throws IllegalArgumentException 슬롯 번호가 범위를 벗어날 때
     * @throws SaveLoadException 쓰기나 교체에 실패했을 때
     */
    void writeLines(int slot, List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("lines 가 null 입니다.");
        }
        Path file = slotFile(slot);
        Path tmp = file.resolveSibling(file.getFileName() + TEMP_SUFFIX);
        try {
            Files.createDirectories(saveDir);
            Files.write(tmp, lines, StandardCharsets.UTF_8);
            moveReplacing(tmp, file);
        } catch (IOException e) {
            SaveLoadException failure = new SaveLoadException("세이브 파일을 저장하지 못했습니다: " + file.getFileName(), e);
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException cleanUp) {
                // 정리 실패는 원래 오류보다 덜 중요하므로 원래 예외에 붙여 둔다.
                failure.addSuppressed(cleanUp);
            }
            throw failure;
        }
    }

    private static void moveReplacing(Path tmp, Path file) throws IOException {
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
