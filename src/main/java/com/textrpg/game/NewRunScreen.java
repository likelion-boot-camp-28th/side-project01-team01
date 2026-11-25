package com.textrpg.game;

import java.util.Arrays;
import java.util.List;

import com.textrpg.entity.Job;
import com.textrpg.meta.Profile;
import com.textrpg.ui.ConsoleView;
import com.textrpg.ui.InputHandler;

/**
 * 새 탐험 준비 화면: 캐릭터 이름과 직업을 묻는다. 캐릭터와 층을 만드는 일은 GameManager 가 한다.
 */
final class NewRunScreen {

    private static final int MAX_NAME_LENGTH = 12;
    /** 이름을 비워 두면 쓰는 기본 이름. */
    private static final String DEFAULT_NAME = "모험가";

    private final ConsoleView view;
    private final InputHandler input;

    NewRunScreen(ConsoleView view, InputHandler input) {
        this.view = view;
        this.input = input;
    }

    /** 캐릭터 이름을 입력받는다. 비워 두면 기본 이름, 너무 길면 다시 묻는다. */
    String readName() {
        while (true) {
            String name = input.readLine("캐릭터 이름 (1~" + MAX_NAME_LENGTH + "자, 비우면 " + DEFAULT_NAME + "): ");
            if (name.isEmpty()) {
                return DEFAULT_NAME;
            }
            if (name.length() <= MAX_NAME_LENGTH) {
                return name;
            }
            view.printLine("이름은 " + MAX_NAME_LENGTH + "자까지 입력할 수 있습니다.");
        }
    }

    /** 해금된 직업 중 하나를 고른다. */
    Job chooseJob(Profile profile) {
        List<Job> jobs = Arrays.stream(Job.values()).filter(profile::isUnlocked).toList();
        view.printMenu("직업 선택", jobs.stream().map(Job::getDisplayName).toList());
        int choice = input.readInt("번호: ", 1, jobs.size());
        return jobs.get(choice - 1);
    }
}
