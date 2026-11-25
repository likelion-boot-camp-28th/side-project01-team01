package com.textrpg.save;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.textrpg.context.GameContext;
import com.textrpg.core.Position;
import com.textrpg.entity.Job;
import com.textrpg.entity.Player;
import com.textrpg.item.FloorItem;
import com.textrpg.item.Item;
import com.textrpg.item.ItemFactory;
import com.textrpg.map.Floor;
import com.textrpg.meta.Profile;
import com.textrpg.meta.RunResult;
import com.textrpg.meta.UpgradeType;
import com.textrpg.monster.Monster;
import com.textrpg.monster.MonsterFactory;
import com.textrpg.monster.MonsterType;
import com.textrpg.monster.PileOfBones;
import com.textrpg.shop.Shop;

/**
 * SlotData 와 세이브 파일 줄을 서로 바꾼다. 파일을 읽고 쓰는 일은 SaveManager 가, 줄을 섹션으로 나누는 일은
 * SaveFileParser 가 맡는다. 형식은 Project-Plan 2.7 세이브 파일 예시를 따른다.
 *
 * <p>프로필 섹션([META], [PROFILE], [UPGRADES])은 항상 쓰고, 진행 중인 탐험이 있으면 [RUN] 이하 섹션을 더 쓴다.
 * 기획서 예시에 더해 다음 값을 쓴다.
 * <ul>
 *   <li>[RUN] deepestFloor: 파편 계산에 쓰는 가장 깊은 층</li>
 *   <li>[FLOOR] room=x,y,width,height: 방 목록 (Room.toSaveString)</li>
 *   <li>[MONSTERS] monster=종류,x,y,현재HP,깨어남(true/false)[,탄생 턴] — 탄생 턴은 뼈 무더기만</li>
 *   <li>[SHOP] 상점이 있는 층에서만: 상인 위치 x, y, 치료를 받았는지 healUsed, 남은 진열 상품 item=id.
 *       이 섹션이 없는 예전 세이브는 상점이 없는 층으로 읽는다.</li>
 * </ul>
 */
final class SlotCodec {

    /** 세이브 형식 버전. 형식이 바뀌어 예전 파일을 못 읽게 되면 올린다. */
    static final int VERSION = 1;

    private static final String META = "META";
    private static final String PROFILE = "PROFILE";
    private static final String UPGRADES = "UPGRADES";
    /** 진행 중인 탐험 섹션. SaveManager.hasRun 도 이 이름으로 탐험이 있는지 본다. */
    static final String RUN = "RUN";
    private static final String PLAYER = "PLAYER";
    private static final String INVENTORY = "INVENTORY";
    private static final String FLOOR = "FLOOR";
    private static final String MONSTERS = "MONSTERS";
    private static final String ITEMS = "ITEMS";
    private static final String SHOP = "SHOP";

    private static final String LIST_SEPARATOR = ",";
    private static final String GOLD_PREFIX = "GOLD:";
    private static final String MAP_KEY = "map";
    private static final String SEEN_KEY = "seen";
    private static final String ROOM_KEY = "room";
    private static final String MONSTER_KEY = "monster";
    private static final String ITEM_KEY = "item";
    /** 몬스터 한 줄의 칸 수. 뼈 무더기는 탄생 턴이 하나 더 붙는다. */
    private static final int MONSTER_FIELDS = 5;
    private static final int ITEM_FIELDS = 3;

    private SlotCodec() {
    }

    /** 슬롯 내용을 파일 줄로 만든다. 강화는 1단계 이상인 것만 쓴다 (없는 강화는 읽을 때 0단계). */
    static List<String> encode(SlotData data) {
        SaveFileWriter w = new SaveFileWriter();
        w.comment("Into the Dark Save File");
        w.section(META);
        w.put("version", VERSION);
        w.put("savedAt", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());
        writeProfile(w, data.profile());
        data.run().ifPresent(run -> writeRun(w, run));
        return w.toLines();
    }

    /**
     * 파싱된 세이브 파일을 SlotData 로 바꾼다. [UPGRADES] 의 모르는 강화 id 는 무시한다 (Project-Plan 2.7).
     * [RUN] 섹션이 없으면 진행 중인 탐험이 없는 슬롯이다.
     *
     * @throws CorruptedRunException 프로필은 읽었지만 탐험 섹션이 잘못되었을 때
     * @throws SaveLoadException     버전이 다르거나, 프로필의 필수 값이 없거나, 값이 잘못되었을 때
     */
    static SlotData decode(SaveFileParser parser) {
        int version = parser.getInt(META, "version");
        if (version != VERSION) {
            throw new SaveLoadException("호환되지 않는 세이브 버전입니다: " + version + " (지원: " + VERSION + ")");
        }
        Profile profile = readProfile(parser);
        if (!parser.hasSection(RUN)) {
            return SlotData.profileOnly(profile);
        }
        try {
            return SlotData.withRun(profile, readRun(parser, profile));
        } catch (SaveLoadException e) {
            throw new CorruptedRunException(profile, "진행 중인 탐험 기록이 손상되었습니다: " + e.getMessage(), e);
        }
    }

    private static void writeProfile(SaveFileWriter w, Profile p) {
        w.section(PROFILE);
        w.put("shards", p.getShards());
        w.put("totalRuns", p.getTotalRuns());
        w.put("bestFloor", p.getBestFloor());
        w.put("clears", p.getClears());
        w.put("totalKills", p.getTotalKills());
        w.put("unlockedJobs", Arrays.stream(Job.values())
                .filter(p::isUnlocked)
                .map(Job::name)
                .collect(Collectors.joining(LIST_SEPARATOR)));

        w.section(UPGRADES);
        for (UpgradeType t : UpgradeType.values()) {
            if (p.getLevel(t) > 0) {
                w.put(t.name(), p.getLevel(t));
            }
        }
    }

    private static void writeRun(SaveFileWriter w, GameContext run) {
        w.comment("===== 진행 중인 탐험 (사망·클리어 시 아래 전부 삭제) =====");
        w.section(RUN);
        w.put("turn", run.getTurn());
        w.put("kills", run.getRunResult().getKills());
        w.put("deepestFloor", run.getRunResult().getDeepestFloor());
        writePlayer(w, run.getPlayer());
        writeFloor(w, run.getFloor());
    }

    private static void writePlayer(SaveFileWriter w, Player p) {
        w.section(PLAYER);
        w.put("name", p.getName());
        w.put("job", p.getJob().name());
        w.put("level", p.getLevel());
        w.put("exp", p.getExp());
        w.put("gold", p.getGold());
        w.put("hp", p.getStats().getHp());
        w.put("maxHp", p.getStats().getMaxHp());
        w.put("mp", p.getStats().getMp());
        w.put("maxMp", p.getStats().getMaxMp());
        // 장비 보너스를 뺀 기본값. 장비는 weapon/armor 로 따로 복원한다
        w.put("attack", p.getBaseAttack());
        w.put("defense", p.getBaseDefense());
        w.put("x", p.getPos().x());
        w.put("y", p.getPos().y());
        // 장비가 없으면 빈 값
        w.put("weapon", p.getWeapon().map(Item::getId).orElse(""));
        w.put("armor", p.getArmor().map(Item::getId).orElse(""));

        w.section(INVENTORY);
        for (Item item : p.getInventory().getItems()) {
            w.put(ITEM_KEY, item.getId());
        }
    }

    private static void writeFloor(SaveFileWriter w, Floor f) {
        w.section(FLOOR);
        w.put("level", f.getLevel());
        w.put("width", Floor.WIDTH);
        w.put("height", Floor.HEIGHT);
        w.comment("타일: R 바위, W 벽, . 바닥, # 복도, + 문, > 계단");
        putNumbered(w, MAP_KEY, f.exportTiles());
        w.comment("탐험 기록: 1 본 칸, 0 안 본 칸");
        putNumbered(w, SEEN_KEY, f.exportSeen());
        for (String room : f.exportRooms()) {
            w.put(ROOM_KEY, room);
        }

        w.section(MONSTERS);
        w.comment("종류,x,y,현재HP,깨어남[,탄생 턴(뼈 무더기)]");
        for (Monster m : f.getMonsters()) {
            w.put(MONSTER_KEY, encodeMonster(m));
        }

        w.section(ITEMS);
        w.comment("id,x,y (골드는 GOLD:수량)");
        for (FloorItem fi : f.getItems()) {
            String what = fi.isGold() ? GOLD_PREFIX + fi.goldAmount() : fi.item().getId();
            w.put(ITEM_KEY, String.join(LIST_SEPARATOR, what, String.valueOf(fi.pos().x()), String.valueOf(fi.pos().y())));
        }

        f.getShop().ifPresent(shop -> writeShop(w, shop));
    }

    private static void writeShop(SaveFileWriter w, Shop shop) {
        w.section(SHOP);
        w.put("x", shop.getMerchantPos().x());
        w.put("y", shop.getMerchantPos().y());
        w.put("healUsed", String.valueOf(shop.isHealUsed()));
        w.comment("남은 진열 상품 (산 상품은 빠짐)");
        for (Item item : shop.getStock()) {
            w.put(ITEM_KEY, item.getId());
        }
    }

    /** map0=..., map1=... 처럼 줄 번호를 붙여 쓴다. */
    private static void putNumbered(SaveFileWriter w, String key, List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            w.put(key + i, lines.get(i));
        }
    }

    private static String encodeMonster(Monster m) {
        List<String> fields = new ArrayList<>(List.of(
                m.getType().name(),
                String.valueOf(m.getPos().x()),
                String.valueOf(m.getPos().y()),
                String.valueOf(m.getStats().getHp()),
                String.valueOf(m.isAwake())));
        if (m instanceof PileOfBones bones) {
            fields.add(String.valueOf(bones.getBirth()));
        }
        return String.join(LIST_SEPARATOR, fields);
    }

    private static Profile readProfile(SaveFileParser parser) {
        Set<Job> jobs = EnumSet.noneOf(Job.class);
        for (String name : parser.getString(PROFILE, "unlockedJobs").split(LIST_SEPARATOR)) {
            if (name.isBlank()) {
                continue;
            }
            try {
                jobs.add(Job.valueOf(name.strip()));
            } catch (IllegalArgumentException e) {
                throw new SaveLoadException("[PROFILE] unlockedJobs 에 모르는 직업이 있습니다: " + name, e);
            }
        }
        Map<UpgradeType, Integer> levels = new EnumMap<>(UpgradeType.class);
        for (UpgradeType t : UpgradeType.values()) {
            levels.put(t, parser.getInt(UPGRADES, t.name(), 0));
        }
        try {
            return Profile.restore(
                    parser.getInt(PROFILE, "shards"),
                    parser.getInt(PROFILE, "totalRuns"),
                    parser.getInt(PROFILE, "bestFloor"),
                    parser.getInt(PROFILE, "clears"),
                    parser.getInt(PROFILE, "totalKills"),
                    jobs, levels);
        } catch (IllegalArgumentException e) {
            throw new SaveLoadException("프로필 값이 잘못되었습니다: " + e.getMessage(), e);
        }
    }

    /** 진행 중인 탐험을 되살린다. 세이브에 없는 강화 효과(가방 칸·경험치 배율)는 프로필로 다시 맞춘다. */
    private static GameContext readRun(SaveFileParser parser, Profile profile) {
        Floor floor = readFloor(parser);
        // 상인 칸은 지나갈 수 없으므로 플레이어 위치를 확인하기 전에 놓는다
        readShop(parser, floor);
        readMonsters(parser, floor);
        readItems(parser, floor);
        Player player = readPlayer(parser, floor);
        // applyTo 를 다시 부르면 HP·공격·방어 강화가 두 번 붙으므로 저장되지 않는 효과만 맞춘다
        profile.applyRunModifiers(player);

        RunResult result;
        try {
            result = RunResult.restore(parser.getInt(RUN, "deepestFloor"), parser.getInt(RUN, "kills"));
        } catch (IllegalArgumentException e) {
            throw new SaveLoadException("[RUN] 값이 잘못되었습니다: " + e.getMessage(), e);
        }
        GameContext run = new GameContext(player, floor, result);
        try {
            run.restoreTurn(parser.getInt(RUN, "turn"));
        } catch (IllegalArgumentException e) {
            throw new SaveLoadException("[RUN] 값이 잘못되었습니다: " + e.getMessage(), e);
        }
        return run;
    }

    private static Player readPlayer(SaveFileParser parser, Floor floor) {
        String name = parser.getString(PLAYER, "name").strip();
        if (name.isEmpty()) {
            throw new SaveLoadException("[PLAYER] 이름이 비어 있습니다.");
        }
        String jobName = parser.getString(PLAYER, "job");
        Job job;
        try {
            job = Job.valueOf(jobName.strip());
        } catch (IllegalArgumentException e) {
            throw new SaveLoadException("[PLAYER] 모르는 직업입니다: " + jobName, e);
        }

        int x = parser.getInt(PLAYER, "x");
        int y = parser.getInt(PLAYER, "y");
        Position pos = checkInside(floor, PLAYER, "x=" + x + ", y=" + y, x, y);
        if (!floor.isWalkable(pos)) {
            throw new SaveLoadException("[PLAYER] 지나갈 수 없는 칸에 있습니다: x=" + x + ", y=" + y);
        }

        List<Item> items = new ArrayList<>();
        for (String id : parser.getList(INVENTORY, ITEM_KEY)) {
            items.add(readItem(INVENTORY, id));
        }
        try {
            return Player.restore(name, job,
                    parser.getInt(PLAYER, "level"),
                    parser.getInt(PLAYER, "exp"),
                    parser.getInt(PLAYER, "gold"),
                    parser.getInt(PLAYER, "hp"),
                    parser.getInt(PLAYER, "maxHp"),
                    parser.getInt(PLAYER, "mp"),
                    parser.getInt(PLAYER, "maxMp"),
                    parser.getInt(PLAYER, "attack"),
                    parser.getInt(PLAYER, "defense"),
                    pos, readEquipment(parser, "weapon"), readEquipment(parser, "armor"), items);
        } catch (IllegalArgumentException e) {
            throw new SaveLoadException("[PLAYER] 값이 잘못되었습니다: " + e.getMessage(), e);
        }
    }

    /** 장비 칸(weapon/armor)의 아이템. 빈 값이면 장비 없음(null). 칸에 맞는 종류인지는 Player.restore 가 확인한다. */
    private static Item readEquipment(SaveFileParser parser, String key) {
        String id = parser.getString(PLAYER, key).strip();
        return id.isEmpty() ? null : readItem(PLAYER, id);
    }

    private static Floor readFloor(SaveFileParser parser) {
        int width = parser.getInt(FLOOR, "width");
        int height = parser.getInt(FLOOR, "height");
        if (width != Floor.WIDTH || height != Floor.HEIGHT) {
            throw new SaveLoadException("[FLOOR] 맵 크기가 다릅니다: " + width + "x" + height
                    + " (지원: " + Floor.WIDTH + "x" + Floor.HEIGHT + ")");
        }
        List<String> tiles = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (int y = 0; y < Floor.HEIGHT; y++) {
            tiles.add(parser.getString(FLOOR, MAP_KEY + y));
            seen.add(parser.getString(FLOOR, SEEN_KEY + y));
        }
        try {
            return Floor.fromSave(parser.getInt(FLOOR, "level"), tiles, seen, parser.getList(FLOOR, ROOM_KEY));
        } catch (IllegalArgumentException e) {
            throw new SaveLoadException("[FLOOR] 맵이 잘못되었습니다: " + e.getMessage(), e);
        }
    }

    private static void readMonsters(SaveFileParser parser, Floor floor) {
        for (String line : parser.getList(MONSTERS, MONSTER_KEY)) {
            String[] f = split(line);
            MonsterType type = toMonsterType(line, f[0]);
            int expected = type == MonsterType.PILE_OF_BONES ? MONSTER_FIELDS + 1 : MONSTER_FIELDS;
            if (f.length != expected) {
                throw new SaveLoadException("[MONSTERS] 칸 수가 " + expected + "개가 아닙니다: " + line);
            }
            Position pos = checkInside(floor, MONSTERS, line, toInt(MONSTERS, line, f[1]), toInt(MONSTERS, line, f[2]));
            Monster m = type == MonsterType.PILE_OF_BONES
                    ? MonsterFactory.create(type, pos, toInt(MONSTERS, line, f[5]))
                    : MonsterFactory.create(type, pos);

            int hp = toInt(MONSTERS, line, f[3]);
            if (hp < 1 || hp > m.getStats().getMaxHp()) {
                throw new SaveLoadException("[MONSTERS] HP 는 1 ~ " + m.getStats().getMaxHp() + " 입니다: " + line);
            }
            m.getStats().setHp(hp);
            if (toBoolean(MONSTERS, line, f[4])) {
                m.wakeUp();
            }
            floor.addMonster(m);
        }
    }

    private static void readItems(SaveFileParser parser, Floor floor) {
        for (String line : parser.getList(ITEMS, ITEM_KEY)) {
            String[] f = split(line);
            if (f.length != ITEM_FIELDS) {
                throw new SaveLoadException("[ITEMS] 칸 수가 " + ITEM_FIELDS + "개가 아닙니다: " + line);
            }
            Position pos = checkInside(floor, ITEMS, line, toInt(ITEMS, line, f[1]), toInt(ITEMS, line, f[2]));
            if (f[0].startsWith(GOLD_PREFIX)) {
                int amount = toInt(ITEMS, line, f[0].substring(GOLD_PREFIX.length()));
                if (amount < 1) {
                    throw new SaveLoadException("[ITEMS] 골드는 1 이상입니다: " + line);
                }
                floor.addItem(FloorItem.gold(amount, pos));
            } else {
                floor.addItem(FloorItem.of(readItem(ITEMS, f[0]), pos));
            }
        }
    }

    /** [SHOP] 이 있으면 상점을 되살린다. 상인은 방 안 바닥 칸에 서 있어야 한다. */
    private static void readShop(SaveFileParser parser, Floor floor) {
        if (!parser.hasSection(SHOP)) {
            return;
        }
        int x = parser.getInt(SHOP, "x");
        int y = parser.getInt(SHOP, "y");
        String where = "x=" + x + ", y=" + y;
        Position pos = checkInside(floor, SHOP, where, x, y);
        if (floor.roomAt(pos) == null || !floor.isWalkable(pos)) {
            throw new SaveLoadException("[SHOP] 상인이 방 안 바닥이 아닌 칸에 있습니다: " + where);
        }
        String healUsedValue = parser.getString(SHOP, "healUsed").strip();
        boolean healUsed = toBoolean(SHOP, "healUsed=" + healUsedValue, healUsedValue);
        List<Item> stock = new ArrayList<>();
        for (String id : parser.getList(SHOP, ITEM_KEY)) {
            stock.add(readItem(SHOP, id));
        }
        try {
            floor.setShop(Shop.restore(floor.getLevel(), pos, stock, healUsed));
        } catch (IllegalArgumentException e) {
            throw new SaveLoadException("[SHOP] 값이 잘못되었습니다: " + e.getMessage(), e);
        }
    }

    /** 아이템 id 로 아이템을 만든다. 바닥 아이템, 가방, 장비가 같이 쓴다. */
    private static Item readItem(String section, String id) {
        try {
            return ItemFactory.createById(id.strip());
        } catch (IllegalArgumentException e) {
            throw new SaveLoadException("[" + section + "] 모르는 아이템입니다: " + id, e);
        }
    }

    /** 좌표가 맵 안인지 확인한다. 몬스터·아이템·플레이어 위치에 쓴다. */
    private static Position checkInside(Floor floor, String section, String line, int x, int y) {
        Position pos = new Position(x, y);
        if (!floor.isInBounds(pos)) {
            throw new SaveLoadException("[" + section + "] 맵 밖 좌표입니다: " + line);
        }
        return pos;
    }

    private static String[] split(String line) {
        String[] fields = line.split(LIST_SEPARATOR, -1);
        for (int i = 0; i < fields.length; i++) {
            fields[i] = fields[i].strip();
        }
        return fields;
    }

    private static MonsterType toMonsterType(String line, String name) {
        try {
            return MonsterType.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new SaveLoadException("[MONSTERS] 모르는 몬스터입니다: " + line, e);
        }
    }

    private static int toInt(String section, String line, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new SaveLoadException("[" + section + "] 숫자가 아닌 값이 있습니다: " + line, e);
        }
    }

    private static boolean toBoolean(String section, String line, String value) {
        return switch (value) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new SaveLoadException("[" + section + "] true/false 가 아닌 값이 있습니다: " + line);
        };
    }
}
