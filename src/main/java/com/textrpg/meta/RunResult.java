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
