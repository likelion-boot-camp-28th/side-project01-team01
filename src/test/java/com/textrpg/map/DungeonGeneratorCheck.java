package com.textrpg.map;

import com.textrpg.core.Position;
import com.textrpg.monster.Boss;
import com.textrpg.monster.Monster;
import com.textrpg.util.RandomUtil;

import java.util.List;

/**
 * DungeonGenerator / Floor 확인용. JUnit 대신 main 으로 실행한다 (Team_Rules 2.11).
 * 실패가 하나라도 있으면 종료 코드 1.
 * 생성 로직을 구현한 뒤 build.gradle 의 checkClasses 에 추가한다 (지금 넣으면 build 가 실패한다).
 */
public final class DungeonGeneratorCheck {

    /**
     * 층마다 생성해 볼 횟수. 시드는 0 ~ SEEDS_PER_FLOOR - 1 을 쓴다.
     */
    private static final int SEEDS_PER_FLOOR = 1000;

    /**
     * 실패 메시지를 이 개수까지만 출력한다. 같은 버그로 수천 줄이 찍히는 것을 막는다.
     */
    private static final int MAX_PRINTED_FAILURES = 20;

    private static int passed;
    private static int failed;

    private DungeonGeneratorCheck() {
    }

    public static void main(String[] args) {
        for (int level = 1; level <= DungeonGenerator.MAX_FLOOR; level++) {
            for (long seed = 0; seed < SEEDS_PER_FLOOR; seed++) {
                RandomUtil.setSeed(seed);
                Floor f = DungeonGenerator.generate(level);
                String where = level + "층 seed=" + seed;

                checkRooms(f, where);
                checkGoal(f, level, where);
                checkEntities(f, where);
            }
        }
        // TODO(D): 세이브 구현 후 exportTiles / exportSeen / exportRooms → fromSave 왕복 결과가 원본과 같은지 확인

        System.out.println("통과 " + passed + " / 실패 " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    /**
     * 방 규칙.
     */
    private static void checkRooms(Floor f, String where) {
        int roomNum = f.getRooms().size();
        check(roomNum >= 6 && roomNum <= 9, where, "방 개수가 6~9 가 아님: " + roomNum);
        List<Room> rooms = f.getRooms();

        for (int i = 0; i < roomNum; i++) {
            for (int j = i + 1; j < roomNum; j++) {
                check(!rooms.get(i).intersects(rooms.get(j)), where, "방이 겹침: " + rooms.get(i) + ", " + rooms.get(j));
            }
        }

        for (int i = 0; i < roomNum; i++) {
            check(rooms.get(i).x() >= 1 &&
                            rooms.get(i).y() >= 1 &&
                            rooms.get(i).x() <= Floor.WIDTH - rooms.get(i).width() - 1 &&
                            rooms.get(i).y() <= Floor.HEIGHT - rooms.get(i).height() - 1,
                    where, "방이 맵 밖에 있음: " + rooms.get(i));
        }

    }

    /**
     * 시작 위치와 목표(계단·보스) 규칙.
     */
    private static void checkGoal(Floor f, int level, String where) {
        List<Room> rooms = f.getRooms();
        int stairsCount = 0;
        int bossCount = 0;
        Position bossPos = null;
        for (int y = 0; y < Floor.HEIGHT; y++) {
            for (int x = 0; x < Floor.WIDTH; x++) {
                if (f.getTile(x, y) == Tile.STAIRS_DOWN) {
                    stairsCount++;
                }
            }
        }
        List<Monster> monsters = f.getMonsters();
        for (Monster m : monsters) {
            if (m instanceof Boss) {
                bossCount++;
                bossPos = m.getPos();
            }
        }
        if (level < DungeonGenerator.MAX_FLOOR) {
            check(stairsCount == 1 &&
                            bossCount == 0 &&
                            f.getStairs().isPresent() &&
                            f.getTile(f.getStairs().get()) == Tile.STAIRS_DOWN,
                    where, "계단 1개·보스 0마리가 아님: 계단 " + stairsCount + "개, 보스 " + bossCount
                            + "마리, getStairs=" + f.getStairs());
        } else {
            check(stairsCount == 0 &&
                            bossCount == 1 &&
                            f.getStairs().isEmpty(),
                    where, "계단 0개·보스 1마리가 아님: 계단 " + stairsCount + "개, 보스 " + bossCount
                            + "마리, getStairs=" + f.getStairs());
        }

        Position goal = f.getStairs().isPresent() ? f.getStairs().get() : bossPos;
        if (goal != null) {
            check(DungeonGenerator.isReachable(f, f.getStartPos(), goal), where,
                    "목표에 도달하지 못함: 시작 " + f.getStartPos() + " → 목표 " + goal);
        }

        for (Room r : rooms) {
            check(DungeonGenerator.isReachable(f, f.getStartPos(), r.center()), where,
                    "방 중심에 도달하지 못함: " + r);
        }
    }

    /**
     * 몬스터·바닥 아이템(골드) 규칙.
     */
    private static void checkEntities(Floor f, String where) {
        // TODO(D): 일반 몬스터(Boss 제외) 4 ~ 8마리
        // TODO(D): 골드 더미 2 ~ 4개 (placeItems 구현 후 아이템 개수도)
        // TODO(D): 몬스터·아이템이 서로 같은 칸에 없음 (HashSet<Position> 의 add 가 false 면 중복)
        // TODO(D): 시작 위치에 몬스터·아이템 없음, 시작 방에 일반 몬스터 없음
    }

    /**
     * 조건이 참이면 통과, 거짓이면 실패로 센다.
     *
     * @param ok      확인할 조건
     * @param where   어느 층·시드인지 (예: "3층 seed=417")
     * @param message 실패했을 때 무엇이 틀렸는지 (예: "방이 겹침")
     */
    private static void check(boolean ok, String where, String message) {
        if (ok) {
            passed++;
            return;
        }
        failed++;
        if (failed <= MAX_PRINTED_FAILURES) {
            System.out.println("[실패] " + where + ": " + message);
        }
    }
}
