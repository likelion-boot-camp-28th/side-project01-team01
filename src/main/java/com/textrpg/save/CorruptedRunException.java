package com.textrpg.save;

import java.util.Objects;

import com.textrpg.meta.Profile;

/**
 * 프로필 섹션은 멀쩡한데 진행 중인 탐험 섹션([RUN] 이하)만 손상되었을 때 던진다.
 * 읽어 둔 프로필을 함께 넘기므로, GameManager 는 탐험만 버리고 프로필을 살릴지 물을 수 있다 (Project-Plan 2.7).
 */
public class CorruptedRunException extends SaveLoadException {

    private final transient Profile profile;

    /**
     * @param profile 정상적으로 읽은 프로필
     * @param message 사용자에게 보여 줄 수 있는 설명
     * @param cause   탐험 섹션을 읽다가 난 예외
     */
    public CorruptedRunException(Profile profile, String message, Throwable cause) {
        super(message, cause);
        this.profile = Objects.requireNonNull(profile, "profile");
    }

    /** 손상되지 않은 프로필. 탐험만 지우고 계속할 때 쓴다. */
    public Profile getProfile() {
        return profile;
    }
}
