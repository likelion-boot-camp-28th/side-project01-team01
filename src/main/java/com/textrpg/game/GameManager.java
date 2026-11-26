package com.textrpg.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.textrpg.context.GameContext;
import com.textrpg.entity.Job;
import com.textrpg.entity.Player;
import com.textrpg.map.DungeonGenerator;
import com.textrpg.map.FieldOfView;
import com.textrpg.map.Floor;
import com.textrpg.meta.Profile;
import com.textrpg.meta.RunResult;
import com.textrpg.meta.ShardCalculator;
import com.textrpg.meta.UpgradeType;
import com.textrpg.save.CorruptedRunException;
import com.textrpg.save.SaveLoadException;
import com.textrpg.save.SaveManager;
import com.textrpg.save.SlotData;
import com.textrpg.ui.ConsoleView;
import com.textrpg.ui.InputClosedException;
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
 * <p>이 클래스는 상태 전환과 저장 시점(새 탐험, 저장 후 종료, 사망·클리어, 제단 구매)을 정한다.
 * 입력을 반복해서 받는 화면은 따로 두었다: 탐험(ExplorationScreen, 가방·상점 화면 포함), 제단(AltarScreen),
 * 이름·직업 선택(NewRunScreen).
 *
 * <p>세이브 파일 오류(SaveLoadException)는 여기서 잡아 안내한 뒤 메인 메뉴로 돌아간다. 게임을 종료하지 않는다 (Team_Rules 2.9).
 * 입력 스트림이 끝나면(InputClosedException) 저장하지 않고 종료한다. 강제 종료와 같아서 마지막 저장 시점이 남는다.
 */
public class GameManager {

    /** Enter 를 기다릴 때의 안내. 탐험 화면(도움말·로그)도 같이 쓴다. */
    static final String PRESS_ENTER = "Enter 를 누르면 돌아갑니다.";

    private final ConsoleView view;
    private final InputHandler input;
    private final SaveManager saveManager;
    private final TurnSystem turnSystem;
    private final DebugOptions debug;
    private final ExplorationScreen explorationScreen;
    private final AltarScreen altarScreen;
    private final NewRunScreen newRunScreen;

    private GameState state = GameState.MAIN_MENU;
    /** 선택한 슬롯 번호 (1 ~ SaveManager.SLOT_COUNT). 슬롯을 고르기 전에는 0. */
    private int slot;
    /** 선택한 슬롯의 영구 프로필. 슬롯을 고른 뒤에만 쓴다. */
    private Profile profile;
    /** 진행 중인 탐험. PLAYING, DEAD, VICTORY 에서만 쓴다. */
    private GameContext run;

    public GameManager(ConsoleView view, InputHandler input, SaveManager saveManager, TurnSystem turnSystem) {
        this(view, input, saveManager, turnSystem, DebugOptions.NONE);
    }

    /** @param debug 개발·시연용 옵션 (--debug-shards, --start-floor=N). 보통 실행은 DebugOptions.NONE */
    public GameManager(ConsoleView view, InputHandler input, SaveManager saveManager, TurnSystem turnSystem,
                       DebugOptions debug) {
        this.view = view;
        this.input = input;
        this.saveManager = saveManager;
        this.turnSystem = turnSystem;
        this.debug = debug;
        this.explorationScreen = new ExplorationScreen(view, input, turnSystem);
        this.altarScreen = new AltarScreen(view, input);
        this.newRunScreen = new NewRunScreen(view, input);
    }

    /** QUIT 이 될 때까지 화면을 차례로 보여 준다. Main 이 호출한다. */
    public void start() {
        try {
            while (state != GameState.QUIT) {
                state = nextState();
            }
        } catch (InputClosedException e) {
            view.printLine("");
            view.printLine("입력이 끝나 게임을 종료합니다.");
        }
    }

    /** 지금 상태의 화면을 하나 보여 주고 다음 상태를 돌려준다. 세이브 오류는 안내 후 메인 메뉴로. */
    private GameState nextState() {
        try {
            return switch (state) {
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
            // 메인 메뉴가 화면을 지우므로 안내를 읽을 수 있게 Enter 를 기다린다.
            view.printLine("세이브 파일 오류: " + e.getMessage());
            waitForEnter();
            return GameState.MAIN_MENU;
        }
    }

    /**
     * 타이틀과 슬롯 1 ~ 3 요약을 보여 주고 슬롯을 고른다. 세이브가 있으면 프로필을 불러오고,
     * 빈 슬롯이면 새 프로필을 만들어 바로 저장한다. 손상된 슬롯은 새로 시작할지 묻는다.
     *
     * @return 슬롯을 고르면 SLOT_MENU, 종료를 고르면 QUIT, 손상된 슬롯을 그대로 두면 MAIN_MENU
     */
    private GameState showMainMenu() {
        view.clearScreen();
        List<String> options = new ArrayList<>();
        for (int i = 1; i <= SaveManager.SLOT_COUNT; i++) {
            options.add("슬롯 " + i + "  " + saveManager.getSlotSummary(i));
        }
        options.add("종료");
        view.printMenu(debug.isEnabled() ? ConsoleView.GAME_TITLE + " [디버그]" : ConsoleView.GAME_TITLE, options);

        int choice = input.readInt("번호: ", 1, options.size());
        if (choice == options.size()) {
            return GameState.QUIT;
        }
        slot = choice;
        if (!saveManager.exists(slot)) {
            startNewProfile();
            applyDebugShards();
            return GameState.SLOT_MENU;
        }
        try {
            profile = saveManager.load(slot).profile();
        } catch (CorruptedRunException e) {
            view.printLine("세이브 파일 오류: " + e.getMessage());
            if (!input.confirm("진행 중인 탐험만 지우고 파편·강화·기록은 유지할까요?")) {
                return GameState.MAIN_MENU;
            }
            profile = e.getProfile();
            saveManager.clearRun(slot, profile);
        } catch (SaveLoadException e) {
            view.printLine("세이브 파일 오류: " + e.getMessage());
            if (!input.confirm("이 슬롯을 지우고 새로 시작할까요?")) {
                return GameState.MAIN_MENU;
            }
            startNewProfile();
        }
        applyDebugShards();
        return GameState.SLOT_MENU;
    }

    /**
     * --debug-shards: 고른 슬롯의 파편이 기준보다 적으면 기준까지 채운다. 나머지 기록·강화는 그대로 둔다.
     * 바로 저장하지 않고, 제단 구매나 탐험이 끝날 때 함께 저장된다.
     */
    private void applyDebugShards() {
        if (profile.getShards() >= debug.minShards()) {
            return;
        }
        Set<Job> unlocked = EnumSet.noneOf(Job.class);
        for (Job job : Job.values()) {
            if (profile.isUnlocked(job)) {
                unlocked.add(job);
            }
        }
        Map<UpgradeType, Integer> levels = new EnumMap<>(UpgradeType.class);
        for (UpgradeType t : UpgradeType.values()) {
            levels.put(t, profile.getLevel(t));
        }
        profile = Profile.restore(debug.minShards(), profile.getTotalRuns(), profile.getBestFloor(),
                profile.getClears(), profile.getTotalKills(), unlocked, levels);
    }

    /** 이 슬롯을 빈 프로필로 시작하고 바로 저장한다. */
    private void startNewProfile() {
        profile = Profile.newProfile();
        saveManager.save(slot, SlotData.profileOnly(profile));
    }

    /**
     * 슬롯 메뉴: 새 탐험 / 이어하기(진행 중 탐험이 있을 때) / 어둠의 제단 / 기록 / 뒤로.
     * 진행 중인 탐험이 있는데 새 탐험을 고르면 기존 탐험이 사라진다고 확인을 받는다.
     *
     * @return 고른 메뉴에 해당하는 상태
     */
    private GameState showSlotMenu() {
        view.clearScreen();
        List<SlotAction> actions = new ArrayList<>(Arrays.asList(SlotAction.values()));
        if (!hasSavedRun()) {
            actions.remove(SlotAction.CONTINUE);
        }
        view.printMenu("슬롯 " + slot + "  |  파편 " + profile.getShards(),
                actions.stream().map(SlotAction::label).toList());

        SlotAction action = actions.get(input.readInt("번호: ", 1, actions.size()) - 1);
        return switch (action) {
            case NEW_RUN -> {
                if (hasSavedRun() && !input.confirm("진행 중인 탐험이 사라집니다. 새로 시작할까요?")) {
                    yield GameState.SLOT_MENU;
                }
                newRun();
                yield GameState.PLAYING;
            }
            case CONTINUE -> {
                continueRun();
                yield GameState.PLAYING;
            }
            case ALTAR -> GameState.ALTAR;
            case RECORDS -> GameState.RECORDS;
            case BACK -> GameState.MAIN_MENU;
        };
    }

    /** 이 슬롯에 저장된 진행 중 탐험이 있는지. 슬롯 메뉴의 "이어하기" 표시와 새 탐험 확인에 쓴다. */
    private boolean hasSavedRun() {
        return saveManager.hasRun(slot);
    }

    /**
     * 어둠의 제단: 강화를 살 때마다 즉시 저장한다 (기획서 2.7 제단 구매 저장).
     *
     * @return SLOT_MENU
     */
    private GameState showAltar() {
        altarScreen.open(profile, () -> saveManager.save(slot, SlotData.profileOnly(profile)));
        return GameState.SLOT_MENU;
    }

    /**
     * 프로필 누적 기록 화면.
     *
     * @return SLOT_MENU
     */
    private GameState showRecords() {
        view.clearScreen();
        view.printRecords(profile);
        waitForEnter();
        return GameState.SLOT_MENU;
    }

    /**
     * 새 탐험을 준비한다: 이름·직업 선택 → new Player → profile.applyTo → 1층 생성 → 시작 위치 → 저장.
     * 직업은 profile.isUnlocked() 인 것만 고를 수 있다.
     */
    private void newRun() {
        String name = newRunScreen.readName();
        Job job = newRunScreen.chooseJob(profile);
        Player player = new Player(name, job);
        profile.applyTo(player);

        // --start-floor=N 이면 그 층에서 시작한다. 보통은 1층
        Floor first = DungeonGenerator.generate(debug.startFloor());
        player.setPos(first.getStartPos());
        FieldOfView.update(first, player.getPos());
        RunResult result = new RunResult();
        result.reachFloor(first.getLevel());
        run = new GameContext(player, first, result);
        run.getLog().add("어둠 속으로 들어왔습니다. ? 를 누르면 도움말을 볼 수 있습니다.");
        saveManager.save(slot, SlotData.withRun(profile, run));
    }

    /** 세이브의 진행 중 탐험을 불러온다. 슬롯 메뉴는 탐험이 있을 때만 이어하기를 보여 준다. */
    private void continueRun() {
        run = saveManager.load(slot).run()
                .orElseThrow(() -> new IllegalStateException("이어할 탐험이 없습니다: 슬롯 " + slot));
        // 세이브에는 본 칸(seen)만 있고 지금 보이는 칸은 없으므로 다시 계산한다.
        FieldOfView.update(run.getFloor(), run.getPlayer().getPos());
        run.getLog().add("지난 탐험을 이어 갑니다. 지하 " + run.getFloor().getLevel() + "층");
    }

    /**
     * 탐험 화면을 열고, 화면을 나가는 이유에 따라 다음 상태를 정한다.
     *
     * @return 사망 → DEAD, 클리어 → VICTORY, 저장 후 종료 → SLOT_MENU
     */
    private GameState playRun() {
        return switch (explorationScreen.play(run)) {
            case DIED -> GameState.DEAD;
            case VICTORY -> GameState.VICTORY;
            case SAVE_QUIT -> saveAndQuit();
        };
    }

    private void waitForEnter() {
        input.readLine(PRESS_ENTER);
    }

    /**
     * 저장 후 종료: 진행 중인 탐험을 그대로 저장하고 슬롯 메뉴로 돌아간다.
     */
    private GameState saveAndQuit() {
        saveManager.save(slot, SlotData.withRun(profile, run));
        run = null;
        return GameState.SLOT_MENU;
    }

    /**
     * 사망·클리어 처리. 탐험 섹션을 지우고 즉시 저장하므로 죽음을 되돌릴 수 없다 (기획서 2.7 영구 죽음).
     *
     * @param victory 클리어면 true, 사망이면 false
     * @return SLOT_MENU
     */
    private GameState endRun(boolean victory) {
        RunResult result = run.getRunResult();
        result.finish(run.getPlayer(), run.getTurn(), victory, victory ? "" : turnSystem.getDeathCause());
        int shards = ShardCalculator.calculate(result, profile);
        profile.recordRun(result, shards);
        // 결과 화면보다 먼저 저장한다. 결과 화면에서 강제 종료해도 죽음을 되돌릴 수 없다.
        saveManager.clearRun(slot, profile);
        run = null;

        view.clearScreen();
        view.printRunResult(result, ShardCalculator.breakdown(result, profile));
        waitForEnter();
        return GameState.SLOT_MENU;
    }

    /** 슬롯 메뉴 항목. 선언 순서가 화면 순서다. */
    private enum SlotAction {
        NEW_RUN("새 탐험"),
        CONTINUE("이어하기"),
        ALTAR("어둠의 제단"),
        RECORDS("기록"),
        BACK("메인 메뉴로");

        private final String label;

        SlotAction(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }
}
