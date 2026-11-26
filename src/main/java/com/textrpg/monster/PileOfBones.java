package com.textrpg.monster;

import com.textrpg.core.Position;

/**
 * 일반 몬스터인 해골 병사(SKELETON)이 사망하면 생성되는 뼈 무더기(PILE_OF_BONES).
 * 탄생일(birth)로부터 REVIVE_DURATION만큼의 턴이 지나면 부활한다.
 * MonsterFactory.create(PILE_OF_BONES, ...) 또는 createBones()로 생성한다.
 */
public class PileOfBones extends Monster {
    private static final int REVIVE_DURATION = 4;
    private final int birth;

    PileOfBones(Position pos, int birth) {
        super(MonsterType.PILE_OF_BONES, pos);
        this.birth = birth;
        this.wakeUp();
    }

    /**
     * 뼈 무더기가 해골 병사로 부활하는 턴인지 확인한다.
     *
     * @param turn 지금 턴
     * @return 지금 턴이 탄생일(birth)로부터 REVIVE_DURATION만큼의 턴 이상이 지났으면 true
     */
    public boolean isTimeToRevive(int turn) {
        return turn >= birth + REVIVE_DURATION;
    }

    public int getBirth() {
        return birth;
    }
}
