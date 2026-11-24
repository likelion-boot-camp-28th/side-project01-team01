package com.textrpg.map;

import com.textrpg.core.Position;
import com.textrpg.item.FloorItem;
import com.textrpg.monster.Boss;
import com.textrpg.monster.Monster;
import com.textrpg.util.RandomUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DungeonGenerator / Floor 확인용. JUnit 대신 main 으로 실행한다 (Team_Rules 2.11).
 * 실패가 하나라도 있으면 종료 코드 1.
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
     * 몬스터·바닥 아이템(일반 아이템·골드) 규칙.
     */
    private static void checkEntities(Floor f, String where) {
        int monsterCount = 0;
        List<Monster> monsters = f.getMonsters();
        for (Monster m : monsters) {
            if (!(m instanceof Boss)) {
                monsterCount++;
            }
        }
        check(monsterCount >= 4 && monsterCount <= 8, where, "일반 몬스터가 4~8마리가 아님: " + monsterCount);

        List<FloorItem> items = f.getItems();
        int itemCount = 0;
        for (FloorItem item : items) {
            if (!item.isGold()) {
                itemCount++;
            }
        }
        check(itemCount >= 3 && itemCount <= 6, where, "일반 아이템이 3~6개가 아님: " + itemCount);

        int goldCount = 0;
        for (FloorItem item : items) {
            if (item.isGold()) {
                goldCount++;
            }
        }
        check(goldCount >= 2 && goldCount <= 4, where, "골드 더미가 2~4개가 아님: " + goldCount);

        Set<Position> occupied = new HashSet<>();
        for (Monster m : monsters) {
            check(occupied.add(m.getPos()), where, "같은 칸에 겹침: " + m.getPos());
        }
        for (FloorItem item : items) {
            check(occupied.add(item.pos()), where, "같은 칸에 겹침: " + item.pos());
        }

        Position start = f.getStartPos();
        check(!occupied.contains(start), where, "시작 위치에 몬스터·아이템 있음: " + start);

        // 시작 위치가 방 밖이면 아래 검사는 건너뛴다. 실패는 여기서 기록된다.
        Room startRoom = f.roomAt(start);
        check(startRoom != null, where, "시작 위치가 방 안에 없음: " + start);
        if (startRoom != null) {
            for (Monster m : monsters) {
                check(!startRoom.contains(m.getPos()), where, "시작 방에 몬스터 있음: " + m.getPos());
            }
            for (FloorItem item : items) {
                check(!startRoom.contains(item.pos()), where, "시작 방에 아이템 있음: " + item.pos());
            }
        }
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
