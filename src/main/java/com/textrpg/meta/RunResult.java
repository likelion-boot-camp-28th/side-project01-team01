package com.textrpg.meta;

import com.textrpg.entity.Player;

/**
 * 탐험 한 번의 결과. 탐험 중에 처치 수·도달 층을 모으고, 끝날 때 finish() 로 마무리한다.
 * ShardCalculator 가 이 값으로 파편을 계산한다.
 */
public class RunResult {

    private int deepestFloor = 1;
    private int kills;
    private int finalLevel;
    private int gold;
    private boolean victory;
    private String deathCause = "";
    private int turns;

    /**
     * 세이브 파일의 [RUN] 값으로 진행 중인 탐험 기록을 되살린다. SaveManager 만 쓴다.
     *
     * @param deepestFloor 지금까지 도달한 가장 깊은 층 (1 이상)
     * @param kills        지금까지 처치한 몬스터 수 (0 이상)
     * @throws IllegalArgumentException 값이 범위를 벗어날 때
     */
    public static RunResult restore(int deepestFloor, int kills) {
        if (deepestFloor < 1) {
            throw new IllegalArgumentException("deepestFloor 는 1 이상입니다: " + deepestFloor);
        }
        if (kills < 0) {
            throw new IllegalArgumentException("kills 는 0 이상입니다: " + kills);
        }
        RunResult result = new RunResult();
        result.deepestFloor = deepestFloor;
        result.kills = kills;
        return result;
    }

    /** 몬스터를 처치할 때마다 C 의 CombatSystem.rewardKill() 이 ctx.getRunResult().addKill() 로 호출한다. */
    public void addKill() {
        kills++;
    }

    /** 새 층에 내려갈 때 호출한다. 더 깊은 층일 때만 기록이 바뀐다. */
    public void reachFloor(int level) {
        deepestFloor = Math.max(deepestFloor, level);
    }

    /** 사망하거나 클리어했을 때 호출한다. */
    public void finish(Player p, boolean victory, String cause) {
        // TODO(B, A와 합의): turns 는 GameContext.getTurn() 값을 받는 방법으로 A와 합의 필요
        finalLevel = p.getLevel();
        gold = p.getGold();
        this.victory = victory;
        deathCause = cause;
    }

    public int getDeepestFloor() {
        return deepestFloor;
    }

    public int getKills() {
        return kills;
    }

    public int getFinalLevel() {
        return finalLevel;
    }

    public int getGold() {
        return gold;
    }

    public boolean isVictory() {
        return victory;
    }

    public String getDeathCause() {
        return deathCause;
    }

    public int getTurns() {
        return turns;
    }
}
