package com.textrpg.map;

import com.textrpg.entity.Position;
import com.textrpg.item.FloorItem;
import com.textrpg.monster.Monster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 던전의 한 층. 타일, 방, 몬스터, 바닥 아이템, 시작 위치, 계단을 가진다.
 * map 패키지 밖에서는 DungeonGenerator.generate() / createTestFloor() / fromSave() 로만 만든다.
 */
public class Floor {

    public static final int WIDTH = 60;
    public static final int HEIGHT = 20;

    private final int level;
    private final Tile[][] tiles;
    private final boolean[][] seen;
    private final boolean[][] visible;
    private final List<Room> rooms = new ArrayList<>();
    private final List<Monster> monsters = new ArrayList<>();
    private final List<FloorItem> items = new ArrayList<>();
    private Position stairs;
    private Position startPos;

    /** 전부 ROCK, seen/visible 은 전부 false 인 빈 층. 생성기 전용. */
    Floor(int level) {
        this.level = level;
        this.tiles = new Tile[HEIGHT][WIDTH];
        this.seen = new boolean[HEIGHT][WIDTH];
        this.visible = new boolean[HEIGHT][WIDTH];
        for (Tile[] row : tiles) {
            Arrays.fill(row, Tile.ROCK);
        }
    }

    /**
     * 세이브 파일 내용으로 층을 복원한다.
     * 계단은 타일에서 STAIRS_DOWN 을 찾아 복원하고, 시작 위치는 첫 방의 중심으로 둔다.
     *
     * @param tiles HEIGHT 줄 x WIDTH 자, Tile 의 saveChar
     * @param seen  HEIGHT 줄 x WIDTH 자, '1' 본 칸 / '0' 안 본 칸
     * @param rooms 방마다 Room.toSaveString() 형식 한 줄
     * @throws IllegalArgumentException 줄 수·길이 불일치, 모르는 문자, 잘못된 방 형식
     */
    public static Floor fromSave(int level, List<String> tiles, List<String> seen, List<String> rooms) {
        // TODO(D): 타일·탐험 기록·방 복원, 계단 위치 찾기
        throw new UnsupportedOperationException("TODO(D): Floor.fromSave");
    }

    public int getLevel() {
        return level;
    }

    /** (x, y) 의 타일. 맵 밖이면 예외 대신 ROCK 을 돌려준다. */
    public Tile getTile(int x, int y) {
        if (!isInRange(x, y)) {
            return Tile.ROCK;
        }
        return tiles[y][x];
    }

    /** p 의 타일. 맵 밖이면 ROCK. */
    public Tile getTile(Position p) {
        return getTile(p.x(), p.y());
    }

    /** 0 <= x < WIDTH, 0 <= y < HEIGHT 이면 true. */
    public boolean isInBounds(Position p) {
        return isInRange(p.x(), p.y());
    }

    /** 맵 안이고 타일이 통행 가능하면 true. 몬스터·플레이어가 있는지는 보지 않는다. */
    public boolean isWalkable(Position p) {
        return isInBounds(p) && getTile(p).isWalkable();
    }

    /**
     * 타일이 FLOOR 이고 몬스터·아이템이 없고 시작 위치가 아니면 true. 배치·순간이동용.
     */
    public boolean isFree(Position p) {
        // TODO(D): 타일·몬스터·아이템·시작 위치 검사
        throw new UnsupportedOperationException("TODO(D): Floor.isFree");
    }

    /**
     * 무작위 방 안에서 isFree 인 칸을 고른다.
     *
     * @throws IllegalStateException 정해진 횟수 안에 빈 칸을 찾지 못한 경우
     */
    public Position randomFreePosition() {
        // TODO(D): 무작위 방에서 빈 칸 찾기
        throw new UnsupportedOperationException("TODO(D): Floor.randomFreePosition");
    }

    public Position getStartPos() {
        return startPos;
    }

    /** 계단 위치. 5층은 계단 대신 보스가 있으므로 비어 있다. */
    public Optional<Position> getStairs() {
        return Optional.ofNullable(stairs);
    }

    /** 방 목록 복사본. */
    public List<Room> getRooms() {
        return List.copyOf(rooms);
    }

    /**
     * p 를 내부에 포함하는 방.
     *
     * @return 방, 복도·문·벽이면 null (맵 조회라 null 허용, Team_Rules 2.9)
     */
    public Room roomAt(Position p) {
        // TODO(D): 방 내부 포함 검사
        throw new UnsupportedOperationException("TODO(D): Floor.roomAt");
    }

    /** 몬스터 목록 복사본. 순회 중 removeMonster() 가 불려도 안전하다. 죽은 몬스터는 isAlive() 로 건너뛴다. */
    public List<Monster> getMonsters() {
        return List.copyOf(monsters);
    }

    /**
     * p 에 있는 몬스터.
     *
     * @return 몬스터, 없으면 null (맵 조회라 null 허용, Team_Rules 2.9)
     */
    public Monster getMonsterAt(Position p) {
        // TODO(D): 위치로 몬스터 찾기
        throw new UnsupportedOperationException("TODO(D): Floor.getMonsterAt");
    }

    /** 몬스터를 층에 추가한다. 생성 배치와 세이브 복원에서 사용. */
    public void addMonster(Monster m) {
        monsters.add(m);
    }

    public void removeMonster(Monster m) {
        monsters.remove(m);
    }

    /** 바닥 아이템 목록 복사본. */
    public List<FloorItem> getItems() {
        return List.copyOf(items);
    }

    /**
     * p 에 있는 바닥 아이템. 여러 개면 가장 나중에 놓인 것.
     *
     * @return 아이템, 없으면 null (맵 조회라 null 허용, Team_Rules 2.9)
     */
    public FloorItem getItemAt(Position p) {
        // TODO(D): 위치로 아이템 찾기 (나중에 놓인 것 우선)
        throw new UnsupportedOperationException("TODO(D): Floor.getItemAt");
    }

    /** 바닥에 아이템을 놓는다. 한 칸에 여러 개를 허용한다. */
    public void addItem(FloorItem fi) {
        items.add(fi);
    }

    public void removeItem(FloorItem fi) {
        items.remove(fi);
    }

    /** 한 번이라도 본 칸이면 true. 세이브 대상. (P1) */
    public boolean isSeen(int x, int y) {
        // TODO(D): P1 시야
        throw new UnsupportedOperationException("TODO(D): Floor.isSeen");
    }

    /** (P1) */
    public void markSeen(int x, int y) {
        // TODO(D): P1 시야
        throw new UnsupportedOperationException("TODO(D): Floor.markSeen");
    }

    /** 이번 턴에 보이는 칸이면 true. 세이브하지 않는다. (P1) */
    public boolean isVisible(int x, int y) {
        // TODO(D): P1 시야
        throw new UnsupportedOperationException("TODO(D): Floor.isVisible");
    }

    /** 보이는 칸으로 표시하고 seen 도 함께 표시한다. (P1) */
    public void setVisible(int x, int y) {
        // TODO(D): P1 시야
        throw new UnsupportedOperationException("TODO(D): Floor.setVisible");
    }

    /** 모든 칸을 안 보이는 상태로 되돌린다. 매 턴 시야 계산 전에 호출. (P1) */
    public void clearVisible() {
        // TODO(D): P1 시야
        throw new UnsupportedOperationException("TODO(D): Floor.clearVisible");
    }

    /** 모든 칸을 seen 으로 표시한다. 지도 두루마리용. (P1) */
    public void revealAll() {
        // TODO(D): P1 지도 두루마리
        throw new UnsupportedOperationException("TODO(D): Floor.revealAll");
    }

    /** 세이브용 타일 문자열. HEIGHT 줄, 각 줄 WIDTH 자 (Tile 의 saveChar). */
    public List<String> exportTiles() {
        // TODO(D): 세이브용 타일 문자열
        throw new UnsupportedOperationException("TODO(D): Floor.exportTiles");
    }

    /** 세이브용 탐험 기록. HEIGHT 줄, 각 줄 WIDTH 자 ('1' / '0'). */
    public List<String> exportSeen() {
        // TODO(D): 세이브용 탐험 기록 문자열
        throw new UnsupportedOperationException("TODO(D): Floor.exportSeen");
    }

    /** 세이브용 방 목록. 방마다 Room.toSaveString() 한 줄. */
    public List<String> exportRooms() {
        // TODO(D): 세이브용 방 문자열
        throw new UnsupportedOperationException("TODO(D): Floor.exportRooms");
    }

    /** @throws IllegalArgumentException 맵 밖 좌표 */
    void setTile(int x, int y, Tile t) {
        if (!isInRange(x, y)) {
            throw new IllegalArgumentException("맵 밖 좌표: (" + x + ", " + y + ")");
        }
        tiles[y][x] = t;
    }

    void addRoom(Room r) {
        rooms.add(r);
    }

    /** 복도 연결 전에 방을 x 좌표순으로 정렬한다. */
    void sortRoomsByX() {
        // TODO(D): x 좌표순 정렬
        throw new UnsupportedOperationException("TODO(D): Floor.sortRoomsByX");
    }

    void setStartPos(Position p) {
        this.startPos = p;
    }

    void setStairs(Position p) {
        this.stairs = p;
    }

    private static boolean isInRange(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }
}
