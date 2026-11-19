package com.textrpg.map;

import com.textrpg.entity.Position;
import com.textrpg.monster.MonsterFactory;
import com.textrpg.monster.MonsterType;

import java.util.List;
import java.util.Optional;

/**
 * 층을 무작위로 생성한다. 생성 절차와 상수는 docs/Class-Diagram.md 5.3 참고.
 * 모든 난수는 RandomUtil 을 사용하므로 setSeed() 로 생성 결과를 재현할 수 있다.
 */
public final class DungeonGenerator {

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

    /*
     * 테스트용 고정 맵. 문자는 Tile 의 saveChar 이고, 공백은 ROCK 이다.
     * 방 A 내부 (2,2) 8x4, 방 B 내부 (24,3) 10x5, 두 방은 문 '+' 와 L자 복도로 이어진다.
     */
    private static final char TEST_MAP_ROCK = ' ';
    private static final String[] TEST_MAP = {
            "",
            " WWWWWWWWWW",
            " W........W            WWWWWWWWWWWW",
            " W........+#######     W..........W",
            " W........W      #     W..........W",
            " W........W      ######+..........W",
            " WWWWWWWWWW            W.......>..W",
            "                       W..........W",
            "                       WWWWWWWWWWWW",
    };
    private static final List<Room> TEST_ROOMS = List.of(
            new Room(2, 2, 8, 4),
            new Room(24, 3, 10, 5));
    private static final Position TEST_START = new Position(4, 3);
    private static final Position TEST_STAIRS = new Position(31, 6);
    private static final Position TEST_GOBLIN = new Position(28, 5);

    private DungeonGenerator() {
    }

    /**
     * level 층을 무작위로 생성한다. 1~4층은 계단, 5층(MAX_FLOOR)은 계단 대신 보스가 있다.
     *
     * @throws IllegalStateException MAX_ATTEMPTS 번 연속 생성에 실패한 경우
     */
    public static Floor generate(int level) {
        // TODO(D): tryGenerate 를 MAX_ATTEMPTS 번까지 반복
        throw new UnsupportedOperationException("TODO(D): DungeonGenerator.generate");
    }

    /**
     * 손으로 그린 작은 1층. 무작위 생성이 완성되기 전 다른 파트의 개발·테스트용.
     * 방 2개, 복도 1개, 계단 (31, 6), 시작 위치 (4, 3), 고블린 1마리 (28, 5). 아이템은 없다.
     */
    public static Floor createTestFloor() {
        Floor floor = new Floor(1);
        for (int y = 0; y < TEST_MAP.length; y++) {
            String line = TEST_MAP[y];
            for (int x = 0; x < line.length(); x++) {
                char c = line.charAt(x);
                if (c != TEST_MAP_ROCK) {
                    floor.setTile(x, y, Tile.fromSaveChar(c));
                }
            }
        }
        for (Room room : TEST_ROOMS) {
            floor.addRoom(room);
        }
        floor.setStartPos(TEST_START);
        floor.setStairs(TEST_STAIRS);
        floor.addMonster(MonsterFactory.create(MonsterType.GOBLIN, TEST_GOBLIN));
        return floor;
    }

    /**
     * BFS 로 a 에서 b 까지 걸어서 갈 수 있는지 확인한다. 타일만 검사하고 몬스터는 무시한다.
     * DungeonGeneratorCheck 에서도 쓰므로 package-private.
     */
    static boolean isReachable(Floor f, Position a, Position b) {
        // TODO(D): BFS
        throw new UnsupportedOperationException("TODO(D): DungeonGenerator.isReachable");
    }

    /** 한 번 생성을 시도한다. 방이 MIN_ROOMS 개 미만이거나 목표에 도달할 수 없으면 empty. */
    private static Optional<Floor> tryGenerate(int level) {
        // TODO(D): 방 배치 → 복도 → 시작·목표 → 배치 → 도달성 검사
        throw new UnsupportedOperationException("TODO(D): DungeonGenerator.tryGenerate");
    }

    /** ROOM_PLACE_TRIES 번 시도해 겹치지 않는 방을 MAX_ROOMS 개까지 추가한다. */
    private static void placeRooms(Floor f) {
        // TODO(D): 방 무작위 배치
        throw new UnsupportedOperationException("TODO(D): DungeonGenerator.placeRooms");
    }

    /** 방 둘레는 WALL, 내부는 FLOOR 로 채운다. */
    private static void carveRoom(Floor f, Room r) {
        // TODO(D): 방 그리기
        throw new UnsupportedOperationException("TODO(D): DungeonGenerator.carveRoom");
    }

    /** 방을 x 좌표순으로 정렬한 뒤 i 번 ↔ i+1 번 중심을 L자 복도로 잇는다. */
    private static void connectRooms(Floor f) {
        // TODO(D): 방 연결
        throw new UnsupportedOperationException("TODO(D): DungeonGenerator.connectRooms");
    }

    /** a 에서 b 까지 L자 복도를 판다. 가로·세로 중 무엇을 먼저 팔지는 무작위. */
    private static void carveCorridor(Floor f, Position a, Position b) {
        // TODO(D): L자 복도
        throw new UnsupportedOperationException("TODO(D): DungeonGenerator.carveCorridor");
    }

    /** ROCK → CORRIDOR, WALL → DOOR, FLOOR·DOOR 는 그대로. */
    private static void carveTile(Floor f, int x, int y) {
        // TODO(D): 복도 한 칸 파기
        throw new UnsupportedOperationException("TODO(D): DungeonGenerator.carveTile");
    }

    /** 첫 방에 시작 위치, 마지막 방에 계단(5층은 보스). */
    private static void placeStartAndGoal(Floor f) {
        // TODO(D): 시작 위치·계단·보스 배치
        throw new UnsupportedOperationException("TODO(D): DungeonGenerator.placeStartAndGoal");
    }

    /** 시작 방을 뺀 방들에 몬스터 MIN_MONSTERS ~ MAX_MONSTERS 마리. */
    private static void placeMonsters(Floor f) {
        // TODO(D): MonsterFactory 로 몬스터 배치
        throw new UnsupportedOperationException("TODO(D): DungeonGenerator.placeMonsters");
    }

    /** 시작 방을 뺀 방들에 아이템 MIN_ITEMS ~ MAX_ITEMS 개. */
    private static void placeItems(Floor f) {
        // TODO(D): ItemFactory 로 아이템 배치 (B 의 ItemFactory 필요)
        throw new UnsupportedOperationException("TODO(D): DungeonGenerator.placeItems");
    }

    /** 시작 방을 뺀 방들에 골드 더미 MIN_GOLD_PILES ~ MAX_GOLD_PILES 개, 더미당 층 x 5 ~ 층 x 15. */
    private static void placeGold(Floor f) {
        // TODO(D): 골드 배치 (B 의 FloorItem 골드 표현 합의 필요)
        throw new UnsupportedOperationException("TODO(D): DungeonGenerator.placeGold");
    }
}
