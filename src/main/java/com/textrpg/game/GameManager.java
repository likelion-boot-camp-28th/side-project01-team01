package com.textrpg.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.textrpg.context.GameContext;
import com.textrpg.core.Position;
import com.textrpg.entity.Job;
import com.textrpg.entity.Player;
import com.textrpg.map.DungeonGenerator;
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
 * <p>세이브 파일 오류(SaveLoadException)는 여기서 잡아 안내한 뒤 메인 메뉴로 돌아간다. 게임을 종료하지 않는다 (Team_Rules 2.9).
 * 입력 스트림이 끝나면(InputClosedException) 저장하지 않고 종료한다. 강제 종료와 같아서 마지막 저장 시점이 남는다.
 */
public class GameManager {

    private static final String COMMAND_PROMPT = "명령 (w/a/s/d 이동, i 가방, l 로그, > 내려가기, ? 도움말): ";
    private static final String PRESS_ENTER = "Enter 를 누르면 돌아갑니다.";
    private static final int MAX_NAME_LENGTH = 12;
    /** 이름을 비워 두면 쓰는 기본 이름. */
    private static final String DEFAULT_NAME = "모험가";

    private final ConsoleView view;
    private final InputHandler input;
    private final SaveManager saveManager;
    private final TurnSystem turnSystem;
    private final DebugOptions debug;

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
        return saveManager.exists(slot) && saveManager.load(slot).hasRun();
    }

    /**
     * 어둠의 제단: 강화를 사면 profile.buy() 후 즉시 저장한다 (기획서 2.7 제단 구매 저장).
     *
     * @return SLOT_MENU
     */
    private GameState showAltar() {
        view.clearScreen();
        UpgradeType[] types = UpgradeType.values();
        while (true) {
            view.printAltar(profile);
            int choice = input.readInt("강화 번호 (0: 돌아가기): ", 0, types.length);
            if (choice == 0) {
                return GameState.SLOT_MENU;
            }
            UpgradeType type = types[choice - 1];
            if (profile.buy(type)) {
                saveManager.save(slot, SlotData.profileOnly(profile));
                view.printLine(type.getDisplayName() + " " + profile.getLevel(type) + "단계를 얻었습니다.");
            } else if (profile.getLevel(type) >= type.getMaxLevel()) {
                view.printLine(type.getDisplayName() + " 은(는) 이미 최대 단계입니다.");
            } else {
                view.printLine("파편이 부족합니다.");
            }
        }
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
        String name = readPlayerName();
        Job job = chooseJob();
        Player player = new Player(name, job);
        profile.applyTo(player);

        // --start-floor=N 이면 그 층에서 시작한다. 보통은 1층
        Floor first = DungeonGenerator.generate(debug.startFloor());
        player.setPos(first.getStartPos());
        RunResult result = new RunResult();
        result.reachFloor(first.getLevel());
        run = new GameContext(player, first, result);
        run.getLog().add("어둠 속으로 들어왔습니다. ? 를 누르면 도움말을 볼 수 있습니다.");
        saveManager.save(slot, SlotData.withRun(profile, run));
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

    /** 세이브의 진행 중 탐험을 불러온다. 슬롯 메뉴는 탐험이 있을 때만 이어하기를 보여 준다. */
    private void continueRun() {
        run = saveManager.load(slot).run()
                .orElseThrow(() -> new IllegalStateException("이어할 탐험이 없습니다: 슬롯 " + slot));
        run.getLog().add("지난 탐험을 이어 갑니다. 지하 " + run.getFloor().getLevel() + "층");
    }

    /**
     * 탐험 턴 루프: 화면 그리기 → 입력 → CommandParser.parse → 명령마다 처리.
     * 화면 명령(INVENTORY, HELP, LOG, SAVE_QUIT)은 여기서 처리하고, 나머지는 turnSystem.playTurn() 에 넘긴다.
     * 탐험 화면에는 이번 행동으로 생긴 메시지만 보이도록, 턴을 쓰는 명령을 실행하기 전에 지난 메시지를 읽음 처리한다.
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
            if (!isScreenCommand(commands.get(0).type())) {
                run.getLog().markRead();
            }
            next = runCommands(commands);
        }
        return next;
    }

    /** 턴을 쓰지 않고 다른 화면을 여는 명령인지. 이 명령들은 CommandParser 가 한 줄에 하나만 만든다. */
    private static boolean isScreenCommand(CommandType type) {
        return type == CommandType.INVENTORY || type == CommandType.HELP
                || type == CommandType.LOG || type == CommandType.SAVE_QUIT;
    }

    /**
     * 한 줄에서 나온 명령들을 차례로 실행한다. 화면 명령(INVENTORY, HELP, LOG, SAVE_QUIT)은 CommandParser 가
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
                case LOG -> {
                    view.clearScreen();
                    view.printLogHistory(run.getLog());
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
        result.finish(run.getPlayer(), victory, victory ? "" : turnSystem.getDeathCause());
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
