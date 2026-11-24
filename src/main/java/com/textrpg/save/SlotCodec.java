package com.textrpg.save;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.textrpg.entity.Job;
import com.textrpg.meta.Profile;
import com.textrpg.meta.UpgradeType;

/**
 * SlotData 와 세이브 파일 줄을 서로 바꾼다. 파일을 읽고 쓰는 일은 SaveManager 가, 줄을 섹션으로 나누는 일은
 * SaveFileParser 가 맡는다. 형식은 Project-Plan 2.7 세이브 파일 예시를 따른다.
 *
 * <p>지금은 프로필 섹션([META], [PROFILE], [UPGRADES])만 다룬다. 진행 중인 탐험 섹션([RUN] 이하)은 2단계.
 */
final class SlotCodec {

    /** 세이브 형식 버전. 형식이 바뀌어 예전 파일을 못 읽게 되면 올린다. */
    static final int VERSION = 1;

    private static final String META = "META";
    private static final String PROFILE = "PROFILE";
    private static final String UPGRADES = "UPGRADES";
    private static final String LIST_SEPARATOR = ",";

    private SlotCodec() {
    }

    /**
     * 슬롯 내용을 파일 줄로 만든다. 강화는 1단계 이상인 것만 쓴다 (없는 강화는 읽을 때 0단계).
     *
     * @throws UnsupportedOperationException 진행 중인 탐험이 있을 때 (2단계 구현 전)
     */
    static List<String> encode(SlotData data) {
        if (data.hasRun()) {
            // TODO(A, 2단계): [RUN]·[PLAYER]·[INVENTORY]·[FLOOR]·[MONSTERS]·[ITEMS] — Player 복원, Floor.fromSave 이후
            throw new UnsupportedOperationException("진행 중인 탐험 저장은 아직 구현되지 않았습니다.");
        }
        SaveFileWriter w = new SaveFileWriter();
        w.comment("Into the Dark Save File");
        w.section(META);
        w.put("version", VERSION);
        w.put("savedAt", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());
        writeProfile(w, data.profile());
        return w.toLines();
    }

    /**
     * 파싱된 세이브 파일을 SlotData 로 바꾼다. [UPGRADES] 의 모르는 강화 id 는 무시한다 (Project-Plan 2.7).
     *
     * @throws SaveLoadException 버전이 다르거나, 필수 값이 없거나, 값이 잘못되었을 때
     */
    static SlotData decode(SaveFileParser parser) {
        int version = parser.getInt(META, "version");
        if (version != VERSION) {
            throw new SaveLoadException("호환되지 않는 세이브 버전입니다: " + version + " (지원: " + VERSION + ")");
        }
        // TODO(A, 2단계): [RUN] 이 있으면 탐험 복원. 지금은 [RUN] 을 쓰지 않으므로 프로필만 돌려준다
        return SlotData.profileOnly(readProfile(parser));
    }

    private static void writeProfile(SaveFileWriter w, Profile p) {
        w.section(PROFILE);
        w.put("shards", p.getShards());
        w.put("totalRuns", p.getTotalRuns());
        w.put("bestFloor", p.getBestFloor());
        w.put("clears", p.getClears());
        w.put("totalKills", p.getTotalKills());
        w.put("unlockedJobs", Arrays.stream(Job.values())
                .filter(p::isUnlocked)
                .map(Job::name)
                .collect(Collectors.joining(LIST_SEPARATOR)));

        w.section(UPGRADES);
        for (UpgradeType t : UpgradeType.values()) {
            if (p.getLevel(t) > 0) {
                w.put(t.name(), p.getLevel(t));
            }
        }
    }

    private static Profile readProfile(SaveFileParser parser) {
        Set<Job> jobs = EnumSet.noneOf(Job.class);
        for (String name : parser.getString(PROFILE, "unlockedJobs").split(LIST_SEPARATOR)) {
            if (name.isBlank()) {
                continue;
            }
            try {
                jobs.add(Job.valueOf(name.strip()));
            } catch (IllegalArgumentException e) {
                throw new SaveLoadException("[PROFILE] unlockedJobs 에 모르는 직업이 있습니다: " + name, e);
            }
        }
        Map<UpgradeType, Integer> levels = new EnumMap<>(UpgradeType.class);
        for (UpgradeType t : UpgradeType.values()) {
            levels.put(t, parser.getInt(UPGRADES, t.name(), 0));
        }
        try {
            return Profile.restore(
                    parser.getInt(PROFILE, "shards"),
                    parser.getInt(PROFILE, "totalRuns"),
                    parser.getInt(PROFILE, "bestFloor"),
                    parser.getInt(PROFILE, "clears"),
                    parser.getInt(PROFILE, "totalKills"),
                    jobs, levels);
        } catch (IllegalArgumentException e) {
            throw new SaveLoadException("프로필 값이 잘못되었습니다: " + e.getMessage(), e);
        }
    }
}
