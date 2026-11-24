package com.textrpg.map;

import com.textrpg.core.Position;
import com.textrpg.item.FloorItem;
import com.textrpg.monster.Monster;
import com.textrpg.util.RandomUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 던전의 한 층 (60 x 20 타일 맵). 타일, 방, 몬스터, 바닥 아이템, 시작 위치, 계단을 가진다.
 * map 패키지 밖에서는 DungeonGenerator.generate() / createTestFloor() / fromSave() 로만 만든다.
 */
public class Floor {

    public static final int WIDTH = 60;
    public static final int HEIGHT = 20;
    private static final int FREE_POSITION_TRIES = 100;

    private final int level;
    private final Tile[][] tiles;
    private final boolean[][] seen;
    private final boolean[][] visible;
    private final List<Room> rooms = new ArrayList<>();
    private final List<Monster> monsters = new ArrayList<>();
    private final List<FloorItem> items = new ArrayList<>();
    private Position stairs;
    private Position startPos;

    /**
     * 전부 ROCK 이고 seen/visible 이 전부 false 인 빈 층. 생성기와 fromSave 전용이라 package-private.
     *
     * @param level 층 번호 (1 ~ DungeonGenerator.MAX_FLOOR)
     */
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
     * 계단은 타일에서 STAIRS_DOWN 을 찾아 복원하고(없으면 5층), 시작 위치는 첫 방의 중심으로 둔다.
     * 몬스터와 바닥 아이템은 SaveManager 가 addMonster() / addItem() 으로 따로 넣는다.
     *
     * @param level 층 번호
     * @param tiles HEIGHT 줄, 각 줄 WIDTH 자. 문자는 Tile 의 saveChar
     * @param seen  HEIGHT 줄, 각 줄 WIDTH 자. '1' 본 칸, '0' 안 본 칸
     * @param rooms 방마다 Room.toSaveString() 형식 한 줄
     * @return 복원한 층
     * @throws IllegalArgumentException 줄 수·길이가 맞지 않거나, 모르는 문자, 잘못된 방 형식일 때.
     *                                  메시지에 몇 번째 줄이 왜 틀렸는지 넣는다 (SaveManager 가 SaveLoadException 으로 감싼다)
     */
    public static Floor fromSave(int level, List<String> tiles, List<String> seen, List<String> rooms) {
        // TODO(D): 줄 수·길이 검사 → Tile.fromSaveChar 로 타일 복원 → seen 복원 → Room.fromSaveString → 계단 찾기
        throw new UnsupportedOperationException("Floor.fromSave 는 아직 구현되지 않았습니다.");
    }

    public int getLevel() {
        return level;
    }

    /**
     * (x, y) 의 타일. 호출하는 쪽마다 범위 검사를 반복하지 않도록 맵 밖이면 예외 대신 ROCK 을 돌려준다.
     *
     * @param x 열 (0 ~ WIDTH - 1)
     * @param y 행 (0 ~ HEIGHT - 1)
     * @return 타일, 맵 밖이면 ROCK
     */
    public Tile getTile(int x, int y) {
        if (!isInRange(x, y)) {
            return Tile.ROCK;
        }
        return tiles[y][x];
    }

    /**
     * p 의 타일.
     *
     * @param p 좌표
     * @return 타일, 맵 밖이면 ROCK
     */
    public Tile getTile(Position p) {
        return getTile(p.x(), p.y());
    }

    /**
     * p 가 맵 안인지.
     *
     * @param p 좌표
     * @return {@code 0 <= x < WIDTH} 이고 {@code 0 <= y < HEIGHT} 이면 true
     */
    public boolean isInBounds(Position p) {
        return isInRange(p.x(), p.y());
    }

    /**
     * p 로 걸어갈 수 있는 타일인지. 몬스터·플레이어가 있는지는 보지 않는다 (getMonsterAt 으로 따로 확인).
     *
     * @param p 좌표
     * @return 맵 안이고 타일이 통행 가능하면 true
     */
    public boolean isWalkable(Position p) {
        return isInBounds(p) && getTile(p).isWalkable();
    }

    /**
     * p 에 무언가를 새로 놓을 수 있는지. 생성 배치와 순간이동 두루마리용.
     *
     * @param p 좌표
     * @return 타일이 FLOOR 이고 몬스터·아이템이 없고 시작 위치가 아니면 true
     */
    public boolean isFree(Position p) {
        return getTile(p) == Tile.FLOOR && getMonsterAt(p) == null && getItemAt(p) == null && !p.equals(startPos);
    }

    /**
     * 무작위 방 안에서 isFree 인 칸을 고른다.
     *
     * @return 빈 칸
     * @throws IllegalStateException 정해진 횟수 안에 빈 칸을 찾지 못했을 때
     */
    public Position randomFreePosition() {
        for (int i = 0; i < FREE_POSITION_TRIES; i++) {
            Position p = RandomUtil.pick(rooms).randomInside();
            if (isFree(p)) {
                return p;
            }
        }
        throw new IllegalStateException("빈 칸을 " + FREE_POSITION_TRIES + "번 안에 찾지 못했습니다.");
    }

    /**
     * 플레이어 시작 위치. 새 층에 들어오면 GameManager 가 player.setPos(floor.getStartPos()) 를 호출한다.
     *
     * @return 시작 위치
     */
    public Position getStartPos() {
        return startPos;
    }

    /**
     * 계단 위치.
     *
     * @return 계단 위치, 5층(계단 대신 보스)이면 비어 있음
     */
    public Optional<Position> getStairs() {
        return Optional.ofNullable(stairs);
    }

    /**
     * 방 목록.
     *
     * @return 복사본 (수정 불가)
     */
    public List<Room> getRooms() {
        return List.copyOf(rooms);
    }

    /**
     * p 를 내부에 포함하는 방. 몬스터 AI 의 "같은 방" 판정과 시야 계산에 쓴다.
     *
     * @param p 좌표
     * @return 방, 복도·문·벽이면 null (자주 부르는 맵 조회라 Optional 대신 null, Team_Rules 2.9)
     */
    public Room roomAt(Position p) {
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).contains(p)) {
                return rooms.get(i);
            }
        }
        return null;
    }

    /**
     * 몬스터 목록. 몬스터 턴 도중 removeMonster() 가 불려도 순회가 깨지지 않도록 복사본을 돌려준다.
     * 순회 중 이미 죽은 몬스터는 isAlive() 로 건너뛴다.
     *
     * @return 복사본 (수정 불가)
     */
    public List<Monster> getMonsters() {
        return List.copyOf(monsters);
    }

    /**
     * p 에 있는 몬스터.
     *
     * @param p 좌표
     * @return 몬스터, 없으면 null (자주 부르는 맵 조회라 Optional 대신 null, Team_Rules 2.9)
     */
    public Monster getMonsterAt(Position p) {
        for (Monster m : monsters) {
            if (m.getPos().equals(p)) {
                return m;
            }
        }
        return null;
    }

    /**
     * 몬스터를 층에 추가한다. 생성 배치와 세이브 복원에서 쓴다.
     *
     * @param m 추가할 몬스터 (위치는 m.getPos())
     */
    public void addMonster(Monster m) {
        monsters.add(m);
    }

    /**
     * 몬스터를 층에서 뺀다. 처치했을 때 쓴다.
     *
     * @param m 뺄 몬스터
     */
    public void removeMonster(Monster m) {
        monsters.remove(m);
    }

    /**
     * 바닥 아이템 목록.
     *
     * @return 복사본 (수정 불가)
     */
    public List<FloorItem> getItems() {
        return List.copyOf(items);
    }

    /**
     * p 에 있는 바닥 아이템. 한 칸에 여러 개면 가장 나중에 놓인 것.
     *
     * @param p 좌표
     * @return 아이템, 없으면 null (자주 부르는 맵 조회라 Optional 대신 null, Team_Rules 2.9)
     */
    public FloorItem getItemAt(Position p) {
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).pos().equals(p)) {
                return items.get(i);
            }
        }
        return null;
    }

    /**
     * 바닥에 아이템을 놓는다. 가방에서 버리기가 항상 성공하도록 한 칸에 여러 개를 허용한다.
     *
     * @param fi 놓을 아이템
     */
    public void addItem(FloorItem fi) {
        items.add(fi);
    }

    /**
     * 바닥 아이템을 치운다. 줍기에서 쓴다.
     *
     * @param fi 치울 아이템
     */
    public void removeItem(FloorItem fi) {
        items.remove(fi);
    }

    /**
     * (P1) 한 번이라도 본 칸인지. 세이브 대상이다.
     *
     * @param x 열
     * @param y 행
     * @return 본 적 있으면 true, 맵 밖이면 false
     */
    public boolean isSeen(int x, int y) {
        // TODO(D): P1 시야
        throw new UnsupportedOperationException("Floor.isSeen 은 아직 구현되지 않았습니다.");
    }

    /**
     * (P1) 본 칸으로 표시한다.
     *
     * @param x 열
     * @param y 행
     */
    public void markSeen(int x, int y) {
        // TODO(D): P1 시야
        throw new UnsupportedOperationException("Floor.markSeen 은 아직 구현되지 않았습니다.");
    }

    /**
     * (P1) 이번 턴에 보이는 칸인지. 세이브하지 않는다.
     *
     * @param x 열
     * @param y 행
     * @return 보이면 true, 맵 밖이면 false
     */
    public boolean isVisible(int x, int y) {
        // TODO(D): P1 시야
        throw new UnsupportedOperationException("Floor.isVisible 은 아직 구현되지 않았습니다.");
    }

    /**
     * (P1) 보이는 칸으로 표시하고 seen 도 함께 표시한다.
     *
     * @param x 열
     * @param y 행
     */
    public void setVisible(int x, int y) {
        // TODO(D): P1 시야
        throw new UnsupportedOperationException("Floor.setVisible 은 아직 구현되지 않았습니다.");
    }

    /**
     * (P1) 모든 칸을 안 보이는 상태로 되돌린다. 매 턴 시야 계산 전에 호출한다.
     */
    public void clearVisible() {
        // TODO(D): P1 시야
        throw new UnsupportedOperationException("Floor.clearVisible 은 아직 구현되지 않았습니다.");
    }

    /**
     * (P1) 모든 칸을 seen 으로 표시한다. 지도 두루마리용.
     */
    public void revealAll() {
        // TODO(D): P1 지도 두루마리
        throw new UnsupportedOperationException("Floor.revealAll 은 아직 구현되지 않았습니다.");
    }

    /**
     * 세이브용 타일 문자열.
     *
     * @return HEIGHT 줄, 각 줄 WIDTH 자 (Tile 의 saveChar)
     */
    public List<String> exportTiles() {
        List<String> lines = new ArrayList<>();
        for (int y = 0; y < HEIGHT; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < WIDTH; x++) {
                sb.append(tiles[y][x].getSaveChar());
            }
            lines.add(sb.toString());
        }
        return List.copyOf(lines);
    }

    /**
     * 세이브용 탐험 기록.
     *
     * @return HEIGHT 줄, 각 줄 WIDTH 자 ('1' 본 칸, '0' 안 본 칸)
     */
    public List<String> exportSeen() {
        List<String> lines = new ArrayList<>();
        for (int y = 0; y < HEIGHT; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < WIDTH; x++) {
                if (seen[y][x]) {
                    sb.append('1');
                } else {
                    sb.append('0');
                }
            }
            lines.add(sb.toString());
        }
        return List.copyOf(lines);
    }

    /**
     * 세이브용 방 목록.
     *
     * @return 방마다 Room.toSaveString() 한 줄
     */
    public List<String> exportRooms() {
        List<String> lines = new ArrayList<>();
        for (Room r : rooms) {
            lines.add(r.toSaveString());
        }
        return List.copyOf(lines);
    }

    /**
     * 타일을 바꾼다. 생성기 전용.
     *
     * @throws IllegalArgumentException 맵 밖 좌표일 때
     */
    void setTile(int x, int y, Tile t) {
        if (!isInRange(x, y)) {
            throw new IllegalArgumentException("맵 밖 좌표입니다: (" + x + ", " + y + ")");
        }
        tiles[y][x] = t;
    }

    void addRoom(Room r) {
        rooms.add(r);
    }

    /**
     * 복도 연결 전에 방을 x 좌표순으로 정렬한다. 생성기 전용.
     */
    void sortRoomsByX() {
        rooms.sort(Comparator.comparingInt(Room::x));
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
