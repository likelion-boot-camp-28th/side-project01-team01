package com.textrpg.context;

import java.util.Objects;

import com.textrpg.core.MessageLog;
import com.textrpg.entity.Player;
import com.textrpg.map.Floor;
import com.textrpg.meta.RunResult;

/**
 * 탐험 한 번 동안 한 턴을 처리하는 데 필요한 것들을 묶어 둔 상자.
 * 파트 사이의 메서드는 Player, Floor, MessageLog 를 하나씩 받는 대신 이 객체 하나를 받는다.
 * 그러면 P1 기능이 늘어도 필드만 추가하고 메서드 시그니처는 그대로 둘 수 있다.
 *
 * <p>규칙: 한 턴을 처리하는 데 필요한 것만 넣는다. 메뉴·세이브 슬롯·Profile 처럼 탐험 밖의 것은 넣지 않는다.
 */
public class GameContext {

    private final Player player;
    private final RunResult runResult;
    private final MessageLog log = new MessageLog();
    /** 계단을 내려가면 바뀐다. */
    private Floor floor;
    /** 지금까지 지난 턴 수. 행동해서 턴을 쓴 경우에만 늘어난다. */
    private int turn;

    /**
     * 새 탐험의 컨텍스트를 만든다. 턴은 0, 메시지 로그는 비어 있다.
     *
     * @param player    플레이어
     * @param floor     시작 층
     * @param runResult 이번 탐험의 기록 (처치 수, 도달 층)
     */
    public GameContext(Player player, Floor floor, RunResult runResult) {
        this.player = Objects.requireNonNull(player, "player");
        this.floor = Objects.requireNonNull(floor, "floor");
        this.runResult = Objects.requireNonNull(runResult, "runResult");
    }

    public Player getPlayer() {
        return player;
    }

    public Floor getFloor() {
        return floor;
    }

    /**
     * 현재 층을 바꾼다. 계단을 내려갈 때와 세이브를 불러올 때 쓴다.
     *
     * @param floor 새 층
     */
    public void setFloor(Floor floor) {
        this.floor = Objects.requireNonNull(floor, "floor");
    }

    public RunResult getRunResult() {
        return runResult;
    }

    public MessageLog getLog() {
        return log;
    }

    public int getTurn() {
        return turn;
    }

    /**
     * 턴을 하나 늘린다. TurnSystem 이 플레이어가 턴을 쓴 뒤에 호출한다.
     *
     * @return 늘어난 뒤의 턴 수
     */
    public int nextTurn() {
        return ++turn;
    }

    /**
     * 세이브에서 불러온 턴 수를 넣는다.
     *
     * @param turn 0 이상
     * @throws IllegalArgumentException 음수일 때
     */
    public void restoreTurn(int turn) {
        if (turn < 0) {
            throw new IllegalArgumentException("턴 수는 0 이상입니다: " + turn);
        }
        this.turn = turn;
    }
}
