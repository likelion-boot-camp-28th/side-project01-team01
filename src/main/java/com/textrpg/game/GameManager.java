package com.textrpg.game;

import java.util.Arrays;
import java.util.List;

import com.textrpg.context.GameContext;
import com.textrpg.core.Position;
import com.textrpg.entity.Job;
import com.textrpg.entity.Player;
import com.textrpg.map.DungeonGenerator;
import com.textrpg.map.Floor;
import com.textrpg.meta.Profile;
import com.textrpg.meta.RunResult;
import com.textrpg.save.SaveLoadException;
import com.textrpg.save.SaveManager;
import com.textrpg.ui.ConsoleView;
import com.textrpg.ui.InputHandler;

/**
 * 게임 전체 흐름을 맡는다. GameState 에 따라 화면을 하나씩 보여 주고, 각 화면이 다음 GameState 를 돌려준다.
 *
 * <pre>
 * MAIN_MENU → SLOT_MENU ─┬→ PLAYING ─┬→ DEAD    ─┐
 *                        │           ├→ VICTORY ─┼→ SLOT_MENU
 *                        │           └→ (저장 후 종료) ┘
 *                        ├→ ALTAR   → SLOT_MENU
 *                        ├→ RECORDS → SLOT_MENU
 *                        └→ MAIN_MENU
 * MAIN_MENU → QUIT
 * </pre>
 *
 * <p>세이브 파일 오류(SaveLoadException)는 여기서 잡아 안내한 뒤 메인 메뉴로 돌아간다. 게임을 종료하지 않는다 (Team_Rules 2.9).
 */
public class GameManager {

    private static final String COMMAND_PROMPT = "명령 (w/a/s/d 이동, g 줍기, i 가방, > 내려가기, ? 도움말): ";
    private static final String PRESS_ENTER = "Enter 를 누르면 돌아갑니다.";
    private static final int MAX_NAME_LENGTH = 12;
    /** 이름을 비워 두면 쓰는 기본 이름. 입력 스트림이 끝났을 때(빈 문자열)도 여기서 멈춘다. */
    private static final String DEFAULT_NAME = "모험가";

    private final ConsoleView view;
    private final InputHandler input;
    private final SaveManager saveManager;
    private final TurnSystem turnSystem;

    private GameState state = GameState.MAIN_MENU;
    /** 선택한 슬롯 번호 (1 ~ SaveManager.SLOT_COUNT). 슬롯을 고르기 전에는 0. */
    private int slot;
    /** 선택한 슬롯의 영구 프로필. 슬롯을 고른 뒤에만 쓴다. */
    private Profile profile;
    /** 진행 중인 탐험. PLAYING, DEAD, VICTORY 에서만 쓴다. */
    private GameContext run;

    public GameManager(ConsoleView view, InputHandler input, SaveManager saveManager, TurnSystem turnSystem) {
        this.view = view;
        this.input = input;
        this.saveManager = saveManager;
        this.turnSystem = turnSystem;
    }

    /** QUIT 이 될 때까지 화면을 차례로 보여 준다. Main 이 호출한다. */
    public void start() {
        while (state != GameState.QUIT) {
            try {
                state = switch (state) {
                    case MAIN_MENU -> showMainMenu();
                    case SLOT_MENU -> showSlotMenu();
                    case ALTAR -> showAltar();
                    case RECORDS -> showRecords();
                    case PLAYING -> playRun();
                    case DEAD -> endRun(false);
                    case VICTORY -> endRun(true);
                    case QUIT -> GameState.QUIT;
                };
            } catch (SaveLoadException e) {
                view.printLine("세이브 파일 오류: " + e.getMessage());
                state = GameState.MAIN_MENU;
            }
        }
    }

    /**
     * 타이틀과 슬롯 1 ~ 3 요약을 보여 주고 슬롯을 고른다. 빈 슬롯이면 새 프로필을 만들어 바로 저장한다.
     *
     * @return 슬롯을 고르면 SLOT_MENU, 종료를 고르면 QUIT
     */
    private GameState showMainMenu() {
        // TODO(A): saveManager.getSlotSummary(i) 목록 → slot 선택 → load 또는 Profile.newProfile() + save
        throw new UnsupportedOperationException("GameManager.showMainMenu 는 아직 구현되지 않았습니다.");
    }

    /**
     * 슬롯 메뉴: 새 탐험 / 이어하기(진행 중 탐험이 있을 때) / 어둠의 제단 / 기록 / 뒤로.
     * 진행 중인 탐험이 있는데 새 탐험을 고르면 기존 탐험이 사라진다고 확인을 받는다.
     *
     * @return 고른 메뉴에 해당하는 상태
     */
    private GameState showSlotMenu() {
        // TODO(A): 새 탐험 → newRun(), 이어하기 → continueRun() 후 PLAYING
        throw new UnsupportedOperationException("GameManager.showSlotMenu 는 아직 구현되지 않았습니다.");
    }

    /**
     * 어둠의 제단: 강화를 사면 profile.buy() 후 즉시 저장한다 (기획서 2.7 제단 구매 저장).
     *
     * @return SLOT_MENU
     */
    private GameState showAltar() {
        // TODO(A): view.printAltar(profile) → 번호 입력 → profile.buy(type) → saveManager.save(slot, profileOnly)
        throw new UnsupportedOperationException("GameManager.showAltar 는 아직 구현되지 않았습니다.");
    }

    /**
     * 프로필 누적 기록 화면.
     *
     * @return SLOT_MENU
     */
    private GameState showRecords() {
        // TODO(A): view.printRecords(profile) → 아무 키나 입력
        throw new UnsupportedOperationException("GameManager.showRecords 는 아직 구현되지 않았습니다.");
    }

    /**
     * 새 탐험을 준비한다: 이름·직업 선택 → new Player → profile.applyTo → 1층 생성 → 시작 위치 → 저장.
     * 직업은 profile.isUnlocked() 인 것만 고를 수 있다.
     */
    private void newRun() {
        String name = readPlayerName();
        Job job = chooseJob();
        Player player = new Player(name, job);
        // TODO(A, B): Profile.applyTo 구현 후 profile.applyTo(player) 호출 (영구 강화 적용)

        Floor first = DungeonGenerator.generate(1);
        player.setPos(first.getStartPos());
        run = new GameContext(player, first, new RunResult());
        run.getLog().add("어둠 속으로 들어왔습니다. ? 를 누르면 도움말을 볼 수 있습니다.");
        // TODO(A): SaveManager.save 구현 후 saveManager.save(slot, SlotData.withRun(profile, run))
    }

    /** 캐릭터 이름을 입력받는다. 비워 두면 기본 이름, 너무 길면 다시 묻는다. */
    private String readPlayerName() {
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
    private Job chooseJob() {
        List<Job> jobs = Arrays.stream(Job.values()).filter(profile::isUnlocked).toList();
        view.printMenu("직업 선택", jobs.stream().map(Job::getDisplayName).toList());
        int choice = input.readInt("번호: ", 1, jobs.size());
        return jobs.get(choice - 1);
    }

    /** 세이브의 진행 중 탐험을 불러온다. */
    private void continueRun() {
        // TODO(A): run = saveManager.load(slot).run().orElseThrow()
        throw new UnsupportedOperationException("GameManager.continueRun 은 아직 구현되지 않았습니다.");
    }

    /**
     * 탐험 턴 루프: 화면 그리기 → 입력 → CommandParser.parse → 명령마다 처리.
     * 화면 명령(INVENTORY, HELP, SAVE_QUIT)은 여기서 처리하고, 나머지는 turnSystem.playTurn() 에 넘긴다.
     * 연속 이동(dddd)은 명령마다 한 턴씩 진행하고, 중간에 몬스터가 나타나거나 턴을 못 쓰면 남은 명령을 버린다.
     *
     * @return DIED → DEAD, VICTORY → VICTORY, 저장 후 종료 → SLOT_MENU
     */
    private GameState playRun() {
        GameState next = GameState.PLAYING;
        while (next == GameState.PLAYING) {
            view.render(run);
            String line = input.readLine(COMMAND_PROMPT);
            List<Command> commands = CommandParser.parse(line);
            if (commands.isEmpty()) {
                if (!line.isEmpty()) {
                    run.getLog().add("알 수 없는 명령입니다: " + line + " (? 를 누르면 도움말)");
                }
                continue;
            }
            next = runCommands(commands);
        }
        return next;
    }

    /**
     * 한 줄에서 나온 명령들을 차례로 실행한다. 화면 명령(INVENTORY, HELP, SAVE_QUIT)은 CommandParser 가
     * 한 줄에 하나만 만들므로 여기서 바로 처리한다. 연속 이동은 턴을 못 썼거나 중단 조건에 걸리면 남은 명령을 버린다.
     *
     * @return 탐험이 이어지면 PLAYING, 아니면 다음 상태 (DEAD, VICTORY, SLOT_MENU)
     */
    private GameState runCommands(List<Command> commands) {
        for (Command cmd : commands) {
            switch (cmd.type()) {
                case INVENTORY -> {
                    view.clearScreen();
                    view.printInventory(run.getPlayer().getInventory());
                    waitForEnter();
                    return GameState.PLAYING;
                }
                case HELP -> {
                    view.clearScreen();
                    view.printHelp();
                    waitForEnter();
                    return GameState.PLAYING;
                }
                case SAVE_QUIT -> {
                    return saveAndQuit();
                }
                default -> {
                    int hpBefore = run.getPlayer().getStats().getHp();
                    TurnOutcome outcome = turnSystem.playTurn(cmd, run);
                    if (outcome == TurnOutcome.DIED) {
                        return GameState.DEAD;
                    }
                    if (outcome == TurnOutcome.VICTORY) {
                        return GameState.VICTORY;
                    }
                    if (outcome == TurnOutcome.DESCENDED) {
                        descend();
                        return GameState.PLAYING;
                    }
                    if (outcome == TurnOutcome.NO_TURN || isInterrupted(hpBefore)) {
                        return GameState.PLAYING;
                    }
                }
            }
        }
        return GameState.PLAYING;
    }

    /**
     * 연속 이동을 멈춰야 하는지 (기획서: 몬스터 발견 시 중단).
     * 시야(P1)가 없으므로 이번 턴에 피해를 입었거나 몬스터가 바로 옆에 있으면 멈춘다.
     */
    private boolean isInterrupted(int hpBefore) {
        Player player = run.getPlayer();
        if (player.getStats().getHp() < hpBefore) {
            return true;
        }
        Position pos = player.getPos();
        return run.getFloor().getMonsters().stream().anyMatch(m -> m.getPos().isAdjacent(pos));
    }

    private void waitForEnter() {
        input.readLine(PRESS_ENTER);
    }

    /**
     * 다음 층을 만들어 들어간다. 지나온 층은 버린다 (돌아갈 수 없음).
     * 계단은 보스층(5층) 전까지만 있으므로 TurnSystem 이 DESCENDED 를 돌려준 경우에만 부른다.
     */
    private void descend() {
        int nextLevel = run.getFloor().getLevel() + 1;
        Floor next = DungeonGenerator.generate(nextLevel);
        run.setFloor(next);
        run.getPlayer().setPos(next.getStartPos());
        run.getRunResult().reachFloor(nextLevel);
        run.getLog().add("지하 " + nextLevel + "층으로 내려왔습니다.");
    }

    /** 저장 후 종료: 진행 중인 탐험을 그대로 저장하고 슬롯 메뉴로 돌아간다. */
    private GameState saveAndQuit() {
        // TODO(A): saveManager.save(slot, SlotData.withRun(profile, run)) → run = null → SLOT_MENU
        throw new UnsupportedOperationException("GameManager.saveAndQuit 은 아직 구현되지 않았습니다.");
    }

    /**
     * 사망·클리어 처리. 탐험 섹션을 지우고 즉시 저장하므로 죽음을 되돌릴 수 없다 (기획서 2.7 영구 죽음).
     *
     * @param victory 클리어면 true, 사망이면 false
     * @return SLOT_MENU
     */
    private GameState endRun(boolean victory) {
        // TODO(A): runResult.finish(player, victory, turnSystem.getDeathCause())
        //  → shards = ShardCalculator.calculate → profile.recordRun → saveManager.clearRun(slot, profile)
        //  → view.printRunResult(runResult, ShardCalculator.breakdown(...)) → run = null
        throw new UnsupportedOperationException("GameManager.endRun 은 아직 구현되지 않았습니다.");
    }
}
