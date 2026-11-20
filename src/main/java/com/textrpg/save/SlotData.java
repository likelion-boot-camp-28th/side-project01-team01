package com.textrpg.save;

import java.util.Objects;
import java.util.Optional;

import com.textrpg.context.GameContext;
import com.textrpg.meta.Profile;

/**
 * 세이브 슬롯 하나에 담기는 내용: 영구 프로필 + (있다면) 진행 중인 탐험.
 * SaveManager.save() 에 넘기고, SaveManager.load() 가 돌려준다.
 *
 * <p>진행 중인 탐험은 GameContext 그대로 담는다. 저장되는 값(player, floor, runResult, turn)이 GameContext 와 같아서
 * 다이어그램의 RunData 를 따로 두지 않는다. 불러올 때 MessageLog 는 비어 있는 새 로그로 시작한다.
 *
 * @param profile 영구 프로필. 항상 있다
 * @param run     진행 중인 탐험. 없으면 Optional.empty() (세이브 파일에 [RUN] 섹션이 없음)
 */
public record SlotData(Profile profile, Optional<GameContext> run) {

    public SlotData {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(run, "run");
    }

    /** 탐험 없이 프로필만 있는 슬롯. 새 슬롯, 제단 구매 후, 사망·클리어 후에 쓴다. */
    public static SlotData profileOnly(Profile profile) {
        return new SlotData(profile, Optional.empty());
    }

    /** 진행 중인 탐험이 있는 슬롯. 탐험 시작 직후와 저장 후 종료에 쓴다. */
    public static SlotData withRun(Profile profile, GameContext run) {
        return new SlotData(profile, Optional.of(run));
    }

    /** 진행 중인 탐험이 있는지. 슬롯 메뉴의 "이어하기" 표시 여부. */
    public boolean hasRun() {
        return run.isPresent();
    }
}
