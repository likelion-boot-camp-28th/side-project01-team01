package com.textrpg.monster;

import com.textrpg.core.Position;
import com.textrpg.util.RandomUtil;

/**
 * 몬스터는 모두 여기서 만든다. 세이브 파일에서 복원할 때도 같은 메서드를 쓴다.
 * DungeonGenerator가 층을 만들 때 createRandom()과 createBoss()를 호출한다.
 */
public final class MonsterFactory {

    private static final int MIN_FLOOR_LEVEL = 1;
    private static final int MAX_FLOOR_LEVEL = 5;

    private MonsterFactory() {
    }

    /**
     * 몬스터 타입에 맞는 몬스터를 해당 위치에 생성한다.
     * 몬스터 타입으로 ABYSS_WARDEN을 주면 Boss를 생성한다.
     * 몬스터 타입으로 PILE_OF_BONES를 주면 예외를 발생한다.
     *
     * @param type 몬스터 타입(열거형)
     * @param pos 몬스터를 놓을 위치
     * @return 생성된 몬스터
     * @throws IllegalArgumentException 뼈 무더기를 생성하려고 할 때
     */
    public static Monster create(MonsterType type, Position pos) {
        if (type == MonsterType.ABYSS_WARDEN) {
            return createBoss(pos);
        } else if (type == MonsterType.PILE_OF_BONES) {
            throw new IllegalArgumentException("뼈 무더기를 생성하려면 birth값이 필요합니다.");
        } else {
            return new Monster(type, pos);
        }
    }

    /**
     * 뼈 무더기(PILE_OF_BONES)를 생성한다.
     * 몬스터 타입으로 PILE_OF_BONES가 아닌 타입을 주면 예외를 발생한다.
     *
     * @param type 몬스터 타입(열거형)
     * @param pos 몬스터를 놓을 위치
     * @param birth 탄생일(턴)
     * @return 생성된 뼈 무더기
     * @throws IllegalArgumentException 뼈 무더기가 아닌 몬스터에 탄생일을 부여하려 할 때
     */
    public static Monster create(MonsterType type, Position pos, int birth) {
        if (type == MonsterType.PILE_OF_BONES) {
            return createBones(pos, birth);
        } else {
            throw new IllegalArgumentException("뼈 무더기가 아닌 몬스터를 생성할 때는 birth값이 필요없습니다.");
        }
    }

    /**
     * 해당 층에서 생성될 수 있는 몬스터들 중 하나를 무작위로 해당 위치에 생성한다.
     * 보스(ABYSS_WARDEN)는 생성되지 않는다.
     *
     * @param floorLevel 현재 층 번호 (1 ~ 5)
     * @param pos 몬스터를 놓을 위치
     * @return 생성된 일반 몬스터
     * @throws IllegalArgumentException floorLevel이 1 ~ 5를 벗어날 때
     */
    public static Monster createRandom(int floorLevel, Position pos) {
        if (floorLevel < MIN_FLOOR_LEVEL || floorLevel > MAX_FLOOR_LEVEL) {
            throw new IllegalArgumentException("Impossible floorLevel");
        }
        int maxOrd = MonsterType.ABYSS_WARDEN.ordinal() - 1;
        int randOrd = RandomUtil.nextInt(0, maxOrd);
        MonsterType randType = MonsterType.values()[randOrd];

        while (!(floorLevel >= randType.getMinFloor() && floorLevel <= randType.getMaxFloor())) {
            randOrd = RandomUtil.nextInt(0, maxOrd);
            randType = MonsterType.values()[randOrd];
        }

        return new Monster(randType, pos);
    }

    /**
     * 해당 위치에 보스(ABYSS_WARDEN)를 생성한다.
     *
     * @param pos 보스를 놓을 위치
     * @return 생성된 보스
     */
    public static Boss createBoss(Position pos) {
        return new Boss(pos);
    }

    /**
     * 해당 위치에 해당 탄생일을 가진 뼈 무더기(PILE_OF_BONES)를 생성한다.
     *
     * @param pos 뼈 무더기를 놓을 위치
     * @param birth 뼈 무더기의 탄생일(턴)
     * @return 생성된 뼈 무더기
     */
    public static PileOfBones createBones(Position pos, int birth) {
        return new PileOfBones(pos, birth);
    }
}
