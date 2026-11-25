package com.textrpg.game;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.textrpg.context.GameContext;
import com.textrpg.core.Position;
import com.textrpg.entity.Player;
import com.textrpg.map.DungeonGenerator;
import com.textrpg.map.FieldOfView;
import com.textrpg.map.Floor;
import com.textrpg.monster.Monster;
import com.textrpg.shop.Shop;
import com.textrpg.ui.ConsoleView;
import com.textrpg.ui.InputHandler;

/**
 * 탐험 화면: 지도 그리기 → 명령 입력 → 실행을 탐험이 끝나거나 저장 후 종료를 고를 때까지 반복한다.
 * 화면 명령(INVENTORY, HELP, LOG, TALK, SAVE_QUIT)은 여기서 처리하고, 나머지는 turnSystem.playTurn() 에 넘긴다.
 * 계단을 내려가면 다음 층을 만든다.
 *
 * <p>저장은 하지 않는다. 화면을 나가는 이유(Exit)를 돌려주면 GameManager 가 저장하거나 결과 화면으로 넘어간다.
 */
final class ExplorationScreen {

    /** 탐험 화면을 나가는 이유. */
    enum Exit {
        /** 플레이어가 쓰러짐. 원인은 TurnSystem.getDeathCause(). */
        DIED,
        /** 보스를 쓰러뜨림. */
        VICTORY,
        /** 저장 후 종료(save)를 입력함. */
        SAVE_QUIT
    }

    private static final String PROMPT = "명령 (w/a/s/d 이동, i 가방, l 로그, > 내려가기, ? 도움말): ";

    private final ConsoleView view;
    private final InputHandler input;
    private final TurnSystem turnSystem;
    private final InventoryScreen inventoryScreen;
    private final ShopScreen shopScreen;

    ExplorationScreen(ConsoleView view, InputHandler input, TurnSystem turnSystem) {
        this.view = view;
        this.input = input;
        this.turnSystem = turnSystem;
        this.inventoryScreen = new InventoryScreen(view, input);
        this.shopScreen = new ShopScreen(view, input);
    }

    /**
     * 탐험 턴 루프: 화면 그리기 → 입력 → CommandParser.parse → 명령마다 처리.
     * 탐험 화면에는 이번 행동으로 생긴 메시지만 보이도록, 턴을 쓰는 명령을 실행하기 전에 지난 메시지를 읽음 처리한다.
     * 연속 이동(dddd)은 명령마다 한 턴씩 진행하고, 중간에 몬스터가 나타나거나 턴을 못 쓰면 남은 명령을 버린다.
     *
     * @param run 진행 중인 탐험. 계단을 내려가면 run 의 층이 바뀐다
     * @return 탐험 화면을 나가는 이유
     */
    Exit play(GameContext run) {
        while (true) {
            view.render(run);
            String line = input.readLine(PROMPT);
            List<Command> commands = CommandParser.parse(line);
            if (commands.isEmpty()) {
                if (!line.isEmpty()) {
                    run.getLog().add("알 수 없는 명령입니다: " + line + " (? 를 누르면 도움말)");
                }
                continue;
            }
            if (!commands.get(0).type().isScreenCommand()) {
                run.getLog().markRead();
            }
            Optional<Exit> exit = runCommands(commands, run);
            if (exit.isPresent()) {
                return exit.get();
            }
        }
    }

    /**
     * 한 줄에서 나온 명령들을 차례로 실행한다. 화면 명령(INVENTORY, HELP, LOG, TALK, SAVE_QUIT)은 CommandParser 가
     * 한 줄에 하나만 만들므로 여기서 바로 처리한다. 연속 이동은 턴을 못 썼거나 중단 조건에 걸리면 남은 명령을 버린다.
     *
     * @return 탐험 화면을 나가야 하면 그 이유, 탐험이 이어지면 빈 값
     */
    private Optional<Exit> runCommands(List<Command> commands, GameContext run) {
        for (Command cmd : commands) {
            switch (cmd.type()) {
                case INVENTORY -> {
                    return openInventory(run);
                }
                case HELP -> {
                    view.clearScreen();
                    view.printHelp();
                    waitForEnter();
                    return Optional.empty();
                }
                case LOG -> {
                    view.clearScreen();
                    view.printLogHistory(run.getLog());
                    waitForEnter();
                    return Optional.empty();
                }
                case TALK -> {
                    talk(run);
                    return Optional.empty();
                }
                case SAVE_QUIT -> {
                    return Optional.of(Exit.SAVE_QUIT);
                }
                default -> {
                    int hpBefore = run.getPlayer().getStats().getHp();
                    Set<Monster> seenBefore = visibleMonsters(run.getFloor());
                    TurnOutcome outcome = turnSystem.playTurn(cmd, run);
                    if (outcome == TurnOutcome.DIED) {
                        return Optional.of(Exit.DIED);
                    }
                    if (outcome == TurnOutcome.VICTORY) {
                        return Optional.of(Exit.VICTORY);
                    }
                    if (outcome == TurnOutcome.DESCENDED) {
                        descend(run);
                        return Optional.empty();
                    }
                    if (outcome == TurnOutcome.NO_TURN || isInterrupted(run, hpBefore, seenBefore)) {
                        return Optional.empty();
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 가방 화면 (i). 아이템 명령을 고르면 그 명령을 한 턴으로 실행하고 탐험 화면으로 돌아간다.
     * 몬스터도 그 턴에 움직이므로 결과는 탐험 화면에서 본다. 그냥 닫으면 턴을 쓰지 않는다.
     */
    private Optional<Exit> openInventory(GameContext run) {
        Optional<Command> chosen = inventoryScreen.choose(run.getPlayer().getInventory());
        if (chosen.isEmpty()) {
            return Optional.empty();
        }
        run.getLog().markRead();
        return runCommands(List.of(chosen.get()), run);
    }

    /** 상인과 대화 (t). 상인이 바로 옆(상하좌우)에 있으면 상점 화면을 연다. 상점에서 하는 일은 턴을 쓰지 않는다. */
    private void talk(GameContext run) {
        Position pos = run.getPlayer().getPos();
        Optional<Shop> shop = run.getFloor().getShop().filter(s -> s.getMerchantPos().isAdjacent(pos));
        if (shop.isEmpty()) {
            run.getLog().add("말을 걸 상대가 없습니다. 상인 바로 옆에서 t 를 누르세요.");
            return;
        }
        run.getLog().markRead();
        shopScreen.open(shop.get(), run.getPlayer(), run.getLog());
    }

    /**
     * 연속 이동을 멈춰야 하는지 (기획서: 몬스터 발견 시 중단).
     * 이번 턴에 피해를 입었거나, 전에 안 보이던 몬스터가 보이게 됐거나, 몬스터가 바로 옆에 있으면 멈춘다.
     *
     * @param hpBefore   이번 턴 전 HP
     * @param seenBefore 이번 턴 전에 보이던 몬스터
     */
    private static boolean isInterrupted(GameContext run, int hpBefore, Set<Monster> seenBefore) {
        Player player = run.getPlayer();
        if (player.getStats().getHp() < hpBefore) {
            return true;
        }
        if (!seenBefore.containsAll(visibleMonsters(run.getFloor()))) {
            return true;
        }
        Position pos = player.getPos();
        return run.getFloor().getMonsters().stream().anyMatch(m -> m.getPos().isAdjacent(pos));
    }

    /** 지금 시야 안에 있는 몬스터. 같은 몬스터인지는 객체로 비교한다. */
    private static Set<Monster> visibleMonsters(Floor floor) {
        Set<Monster> visible = new HashSet<>();
        for (Monster m : floor.getMonsters()) {
            if (floor.isVisible(m.getPos().x(), m.getPos().y())) {
                visible.add(m);
            }
        }
        return visible;
    }

    /**
     * 다음 층을 만들어 들어간다. 지나온 층은 버린다 (돌아갈 수 없음).
     * 계단은 보스층(5층) 전까지만 있으므로 TurnSystem 이 DESCENDED 를 돌려준 경우에만 부른다.
     */
    private static void descend(GameContext run) {
        int nextLevel = run.getFloor().getLevel() + 1;
        Floor next = DungeonGenerator.generate(nextLevel);
        run.setFloor(next);
        run.getPlayer().setPos(next.getStartPos());
        FieldOfView.update(next, next.getStartPos());
        run.getRunResult().reachFloor(nextLevel);
        run.getLog().add("지하 " + nextLevel + "층으로 내려왔습니다.");
    }

    private void waitForEnter() {
        input.readLine(GameManager.PRESS_ENTER);
    }
}
