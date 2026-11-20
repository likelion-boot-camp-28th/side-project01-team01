package com.textrpg.map;

import com.textrpg.core.Position;

import java.util.Optional;

/**
 * 층을 무작위로 생성한다. 생성 절차와 상수 값은 docs/Class-Diagram.md 5.3 참고.
 * 모든 난수는 RandomUtil 을 쓰므로 RandomUtil.setSeed() 로 같은 맵을 다시 만들 수 있다.
 */
public final class DungeonGenerator {

    /** 마지막 층. 이 층에는 계단 대신 보스가 있다. */
    public static final int MAX_FLOOR = 5;

    private static final int MIN_ROOMS = 6;
    private static final int MAX_ROOMS = 9;
    private static final int ROOM_PLACE_TRIES = 50;
    private static final int ROOM_MIN_W = 4;
    private static final int ROOM_MAX_W = 12;
    private static final int ROOM_MIN_H = 3;
    private static final int ROOM_MAX_H = 6;
    private static final int MIN_MONSTERS = 4;
    private static final int MAX_MONSTERS = 8;
    private static final int MIN_ITEMS = 3;
    private static final int MAX_ITEMS = 6;
    private static final int MIN_GOLD_PILES = 2;
    private static final int MAX_GOLD_PILES = 4;
    private static final int GOLD_PER_FLOOR_MIN = 5;
    private static final int GOLD_PER_FLOOR_MAX = 15;
    private static final int MAX_ATTEMPTS = 100;

    private DungeonGenerator() {
    }

    /**
     * level 층을 무작위로 생성한다. 1 ~ 4층은 계단, 5층(MAX_FLOOR)은 계단 대신 보스가 있다.
     *
     * @param level 층 번호 (1 ~ MAX_FLOOR)
     * @return 시작 위치에서 계단(5층은 보스)까지 걸어갈 수 있는 층
     * @throws IllegalArgumentException level 이 1 ~ MAX_FLOOR 를 벗어날 때
     * @throws IllegalStateException    MAX_ATTEMPTS 번 연속 생성에 실패했을 때
     */
    public static Floor generate(int level) {
        // TODO(D): level 검사 → tryGenerate 를 MAX_ATTEMPTS 번까지 반복
        throw new UnsupportedOperationException("DungeonGenerator.generate 는 아직 구현되지 않았습니다.");
    }

    /**
     * 손으로 그린 작은 1층. 무작위 생성이 완성되기 전 다른 파트의 개발·테스트용.
     *
     * @return 테스트용 층 (부를 때마다 새 객체)
     */
    public static Floor createTestFloor() {
        // TODO(D): 방 2개 + 복도 1개를 고정 좌표로 그리고 시작 위치·계단·몬스터 1마리 배치
        throw new UnsupportedOperationException("DungeonGenerator.createTestFloor 는 아직 구현되지 않았습니다.");
    }

    /**
     * a 에서 b 까지 걸어서 갈 수 있는지 BFS 로 확인한다. 타일만 검사하고 몬스터는 무시한다.
     * DungeonGeneratorCheck 에서도 쓰므로 package-private.
     *
     * @param f 층
     * @param a 출발
     * @param b 도착
     * @return 갈 수 있으면 true
     */
    static boolean isReachable(Floor f, Position a, Position b) {
        // TODO(D): BFS, 이웃은 Direction.values() 로 상하좌우, f.isWalkable 로 통행 판정
        throw new UnsupportedOperationException("DungeonGenerator.isReachable 은 아직 구현되지 않았습니다.");
    }

    /**
     * 한 번 생성을 시도한다.
     *
     * @return 성공하면 층, 방이 MIN_ROOMS 개 미만이거나 목표에 도달할 수 없으면 empty
     */
    private static Optional<Floor> tryGenerate(int level) {
        // TODO(D): placeRooms → 방 개수 확인 → connectRooms → placeStartAndGoal → 배치 → isReachable
        throw new UnsupportedOperationException("DungeonGenerator.tryGenerate 는 아직 구현되지 않았습니다.");
    }

    /** ROOM_PLACE_TRIES 번 시도해 겹치지 않는 방을 MAX_ROOMS 개까지 추가하고 carveRoom 으로 그린다. */
    private static void placeRooms(Floor f) {
        // TODO(D): 크기·위치 무작위 → 벽까지 맵 안인지, 기존 방과 intersects 인지 확인 → addRoom, carveRoom
        throw new UnsupportedOperationException("DungeonGenerator.placeRooms 는 아직 구현되지 않았습니다.");
    }

    /** 방 둘레는 WALL, 내부는 FLOOR 로 채운다. */
    private static void carveRoom(Floor f, Room r) {
        // TODO(D): 벽 포함 영역을 돌며 둘레면 WALL, 아니면 FLOOR
        throw new UnsupportedOperationException("DungeonGenerator.carveRoom 은 아직 구현되지 않았습니다.");
    }

    /** 방을 x 좌표순으로 정렬한 뒤 i 번 방과 i+1 번 방의 중심을 L자 복도로 잇는다. */
    private static void connectRooms(Floor f) {
        // TODO(D): sortRoomsByX → 이웃한 방끼리 carveCorridor(center, center)
        throw new UnsupportedOperationException("DungeonGenerator.connectRooms 는 아직 구현되지 않았습니다.");
    }

    /** a 에서 b 까지 L자 복도를 판다. 가로·세로 중 무엇을 먼저 팔지는 무작위. */
    private static void carveCorridor(Floor f, Position a, Position b) {
        // TODO(D): RandomUtil.chance(50) 로 순서 결정 → 가로 구간, 세로 구간을 carveTile
        throw new UnsupportedOperationException("DungeonGenerator.carveCorridor 는 아직 구현되지 않았습니다.");
    }

    /** 복도 한 칸. ROCK 은 CORRIDOR, WALL 은 DOOR 로 바꾸고 FLOOR·DOOR·CORRIDOR 는 그대로 둔다. */
    private static void carveTile(Floor f, int x, int y) {
        // TODO(D): switch 표현식으로 타일별 처리
        throw new UnsupportedOperationException("DungeonGenerator.carveTile 은 아직 구현되지 않았습니다.");
    }

    /** 첫 방에 시작 위치, 마지막 방에 계단. 5층은 계단 대신 MonsterFactory.createBoss 로 보스. */
    private static void placeStartAndGoal(Floor f) {
        // TODO(D): setStartPos(첫 방 randomInside) → level < MAX_FLOOR 면 STAIRS_DOWN + setStairs, 아니면 보스
        throw new UnsupportedOperationException("DungeonGenerator.placeStartAndGoal 은 아직 구현되지 않았습니다.");
    }

    /** 시작 방을 뺀 방들에 몬스터 MIN_MONSTERS ~ MAX_MONSTERS 마리. */
    private static void placeMonsters(Floor f) {
        // TODO(D): randomFreePosition(시작 방 제외) → MonsterFactory 의 층별 무작위 생성 → addMonster
        throw new UnsupportedOperationException("DungeonGenerator.placeMonsters 는 아직 구현되지 않았습니다.");
    }

    /** 시작 방을 뺀 방들에 아이템 MIN_ITEMS ~ MAX_ITEMS 개. */
    private static void placeItems(Floor f) {
        // TODO(D): ItemFactory.createRandom(level) → addItem (B 의 ItemFactory 필요)
        throw new UnsupportedOperationException("DungeonGenerator.placeItems 는 아직 구현되지 않았습니다.");
    }

    /** 시작 방을 뺀 방들에 골드 더미 MIN_GOLD_PILES ~ MAX_GOLD_PILES 개. 더미당 층 x GOLD_PER_FLOOR_MIN ~ MAX. */
    private static void placeGold(Floor f) {
        // TODO(D): 골드 FloorItem 생성 → addItem (B 와 FloorItem 골드 표현 합의 필요)
        throw new UnsupportedOperationException("DungeonGenerator.placeGold 는 아직 구현되지 않았습니다.");
    }
}
