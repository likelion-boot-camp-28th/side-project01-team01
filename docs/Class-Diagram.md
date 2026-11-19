# 클래스 다이어그램 (v2.1, 로그라이크 + 로그라이트 영구 성장)

> 기획서: [Project-Plan.md](./Project-Plan.md) · Mermaid `classDiagram` 형식 (GitHub, IntelliJ Mermaid 플러그인에서 렌더링)
> 표기: `+` public, `-` private, `#` protected, `*` abstract 메서드, `$` static, `<<P1>>` 추가 기능

## 1. 전체 구조

```mermaid
classDiagram
    direction LR
    Main --> GameManager
    GameManager --> TurnSystem
    GameManager --> ConsoleView
    GameManager --> SaveManager
    TurnSystem --> Player
    TurnSystem --> Floor
    TurnSystem --> CombatSystem
    TurnSystem --> MonsterAI
    Floor --> Tile
    Floor --> Room
    Floor --> Monster
    Floor --> FloorItem
    Player --> Inventory
    Inventory --> Item
    Entity <|-- Player
    Entity <|-- Monster
    DungeonGenerator ..> Floor : creates
    GameManager --> Profile
    GameManager --> RunResult
    Profile ..> Player : applyTo
    SaveManager ..> SlotData
    SlotData --> Profile
    SlotData --> RunData
```

| 파트 | 담당 | 클래스 |
|---|---|---|
| A | 게임 루프·UI·세이브 | Main, GameManager, GameState, TurnSystem, Command, CommandParser, MessageLog, ConsoleView, MapRenderer, InputHandler, RandomUtil, SaveManager, SlotData, RunData, SaveFileParser, SaveLoadException, RankingManager(P1) |
| B | 캐릭터·아이템·영구 성장 | Entity, Position, Player, Job, Stats, Item, Weapon, Armor, Potion, Scroll(P1), Food(P1), ItemType, Inventory, FloorItem, ItemFactory, ItemIdentifier(P1), Profile, UpgradeType, RunResult, ShardCalculator |
| C | 몬스터·전투 | Monster, Boss, MonsterType, MonsterFactory, MonsterAI, CombatSystem, DamageCalculator, AttackResult |
| D | 맵·생성 | Floor, Tile, Room, Direction, DungeonGenerator, FieldOfView(P1) |

---

## 2. 게임 루프 · UI (A 파트)

```mermaid
classDiagram
    class Main {
        +main(String[] args)$
    }
    class GameManager {
        -GameState state
        -int saveSlot
        -Profile profile
        -Player player
        -Floor floor
        -int turn
        -RunResult runResult
        -TurnSystem turnSystem
        -MessageLog log
        -ConsoleView view
        -InputHandler input
        -SaveManager saveManager
        +start()
        -showMainMenu()
        -selectSlot()
        -showSlotMenu()
        -showAltar()
        -newRun()
        -continueRun()
        -gameLoop()
        -descend()
        -endRun(boolean victory, String cause)
        -saveAndQuit()
    }
    class GameState {
        <<enumeration>>
        MAIN_MENU
        SLOT_MENU
        ALTAR
        PLAYING
        DEAD
        VICTORY
        QUIT
    }
    class TurnSystem {
        -CombatSystem combat
        -MonsterAI ai
        -MessageLog log
        +executePlayer(Command cmd, Player p, Floor f) boolean
        +runMonsters(Player p, Floor f)
        -tryMove(Player p, Floor f, Direction d) boolean
        -pickUp(Player p, Floor f) boolean
        -endOfTurn(Player p)
    }
    class Command {
        <<record>>
        CommandType type
        Direction direction
        int itemIndex
    }
    class CommandType {
        <<enumeration>>
        MOVE
        PICK_UP
        INVENTORY
        USE
        EQUIP
        DROP
        DESCEND
        SAVE_QUIT
        HELP
        WAIT
    }
    class CommandParser {
        +parse(String line)$ List~Command~
    }
    class MessageLog {
        -List~String~ messages
        +add(String msg)
        +recent(int n) List~String~
        +clearTurn()
    }
    class ConsoleView {
        -MapRenderer mapRenderer
        -boolean ansiClear
        +clearScreen()
        +render(Floor f, Player p, MessageLog log, int turn)
        +printStatusBar(Player p)
        +printInventory(Inventory inv)
        +printAltar(Profile profile)
        +printRunResult(RunResult r, int shards)
        +printMenu(String title, List~String~ options)
        +printLine(String msg)
    }
    class MapRenderer {
        +render(Floor f, Player p) List~String~
        -symbolAt(Floor f, Player p, int x, int y) char
    }
    class InputHandler {
        -Scanner scanner
        +readLine(String prompt) String
        +readInt(int min, int max) int
        +confirm(String prompt) boolean
    }
    class RandomUtil {
        -Random random$
        +setSeed(long seed)$
        +nextInt(int min, int max)$ int
        +chance(double percent)$ boolean
    }

    Main --> GameManager
    GameManager --> GameState
    GameManager --> TurnSystem
    GameManager --> MessageLog
    GameManager --> ConsoleView
    GameManager --> InputHandler
    GameManager ..> CommandParser
    CommandParser ..> Command : creates
    Command --> CommandType
    ConsoleView --> MapRenderer
    TurnSystem --> MessageLog
```

> `MapRenderer.symbolAt()` 우선순위: 플레이어 `@` → 몬스터 기호 → 바닥 아이템 기호 → 타일 기호. P1 시야 적용 시 안 본 칸은 공백, 본 적 있지만 지금 안 보이는 칸은 타일만(몬스터 숨김).

---

## 3. 캐릭터 · 아이템 (B 파트)

```mermaid
classDiagram
    class Position {
        <<record>>
        int x
        int y
        +move(Direction d) Position
        +distanceTo(Position o) int
        +isAdjacent(Position o) boolean
    }
    class Entity {
        <<abstract>>
        #String name
        #char symbol
        #Position pos
        #Stats stats
        +getName() String
        +getSymbol() char
        +getPos() Position
        +setPos(Position p)
        +getStats() Stats
        +isAlive() boolean
        +takeDamage(int amount) int
        +heal(int amount)
        +getAttack()* int
        +getDefense()* int
    }
    class Stats {
        -int hp
        -int maxHp
        -int mp
        -int maxMp
        -int attack
        -int defense
        -double hitRate
        -double critRate
        -double evadeRate
        +getters / setters
    }
    class Player {
        -Job job
        -int level
        -int exp
        -int gold
        -int hunger
        -Weapon weapon
        -Armor armor
        -Inventory inventory
        +Player(String name, Job job)
        +getAttack() int
        +getDefense() int
        +gainExp(int exp) boolean
        -levelUp()
        +equip(Item item) String
        +useItem(int index, Floor f) String
        +dropItem(int index, Floor f) String
        +addGold(int amount)
        +regenerate(int turn)
    }
    class Job {
        <<enumeration>>
        WARRIOR
        MAGE
        ROGUE
        -String displayName
        +createBaseStats() Stats
    }
    class Inventory {
        -List~Item~ items
        +CAPACITY$ int
        +add(Item item) boolean
        +remove(int index) Item
        +get(int index) Item
        +isFull() boolean
        +getItems() List~Item~
    }
    class Item {
        <<abstract>>
        #String id
        #String name
        #char symbol
        #ItemType type
        +getId() String
        +getDisplayName() String
        +getSymbol() char
        +use(Player p, Floor f)* String
    }
    class ItemType {
        <<enumeration>>
        WEAPON
        ARMOR
        POTION
        SCROLL
        FOOD
        GOLD
    }
    class Weapon {
        -int attackBonus
        +use(Player p, Floor f) String
    }
    class Armor {
        -int defenseBonus
        +use(Player p, Floor f) String
    }
    class Potion {
        -int hpRestore
        -int mpRestore
        +use(Player p, Floor f) String
    }
    class Scroll {
        <<P1>>
        -ScrollEffect effect
        +use(Player p, Floor f) String
    }
    class Food {
        <<P1>>
        -int nutrition
        +use(Player p, Floor f) String
    }
    class FloorItem {
        <<record>>
        Item item
        Position pos
        int goldAmount
    }
    class ItemFactory {
        +createById(String id)$ Item
        +createRandom(int floorLevel)$ Item
    }
    class ItemIdentifier {
        <<P1>>
        -Map unknownNames
        -Set identified
        +init()
        +displayName(Item item) String
        +identify(String id)
        +isIdentified(String id) boolean
    }

    Entity <|-- Player
    Entity *-- Stats
    Entity --> Position
    Player --> Job
    Player *-- Inventory
    Player o-- Weapon
    Player o-- Armor
    Inventory o-- Item
    Item <|-- Weapon
    Item <|-- Armor
    Item <|-- Potion
    Item <|-- Scroll
    Item <|-- Food
    Item --> ItemType
    FloorItem --> Item
    FloorItem --> Position
    ItemFactory ..> Item : creates
    Item ..> ItemIdentifier : displayName
```

> `use()`는 결과 문장을 돌려주고, `TurnSystem`이 그 문장을 `MessageLog`에 넣는다. Weapon/Armor의 `use()`는 장착, Potion/Scroll/Food는 효과 적용 후 가방에서 제거.

---

## 4. 몬스터 · 전투 (C 파트)

```mermaid
classDiagram
    class Entity {
        <<abstract>>
    }
    class Monster {
        -MonsterType type
        -boolean awake
        +Monster(MonsterType type, Position pos)
        +getType() MonsterType
        +getAttack() int
        +getDefense() int
        +getExpReward() int
        +isAwake() boolean
        +wakeUp()
    }
    class Boss {
        +attacksPerTurn() int
    }
    class MonsterType {
        <<enumeration>>
        SLIME
        BAT
        GOBLIN
        WOLF
        SKELETON
        ORC
        SPIDER
        SHADOW_KNIGHT
        ABYSS_WARDEN
        -String displayName
        -char symbol
        -int minFloor
        -int maxFloor
        -int hp
        -int attack
        -int defense
        -int exp
        -int gold
        -int speed
    }
    class MonsterFactory {
        +create(MonsterType t, Position pos)$ Monster
        +createRandom(int floorLevel, Position pos)$ Monster
        +createBoss(Position pos)$ Boss
    }
    class MonsterAI {
        -CombatSystem combat
        +act(Monster m, Player p, Floor f, MessageLog log)
        -canSee(Monster m, Player p, Floor f) boolean
        -stepToward(Monster m, Position target, Floor f) Direction
        -wander(Monster m, Floor f)
    }
    class CombatSystem {
        +attack(Entity atk, Entity def, MessageLog log) AttackResult
        +rewardKill(Player p, Monster m, Floor f, RunResult r, MessageLog log)
    }
    class DamageCalculator {
        +rollHit(Entity atk, Entity def)$ boolean
        +rollCritical(Entity atk)$ boolean
        +calculate(Entity atk, Entity def, boolean crit)$ int
    }
    class AttackResult {
        <<record>>
        boolean hit
        boolean critical
        int damage
        boolean killed
    }

    Entity <|-- Monster
    Monster <|-- Boss
    Monster --> MonsterType
    MonsterFactory ..> Monster : creates
    MonsterAI --> CombatSystem
    CombatSystem ..> DamageCalculator
    CombatSystem ..> AttackResult : returns
```

> 데미지: 명중 실패 시 0 → `max(1, 공격 - 방어) × 0.9~1.1` → 치명타 시 ×1.5.
> AI 규칙: 깨어 있지 않으면 대기, 플레이어가 같은 방 또는 6칸 이내면 깨어남. 깨어 있으면 인접 시 공격, 아니면 x·y 중 거리가 큰 축으로 한 칸 이동(막히면 다른 축). `speed`가 2면 2번 행동, 슬라임처럼 느린 몬스터는 짝수 턴에만 행동.

---

## 5. 맵 · 던전 생성 (D 파트)

```mermaid
classDiagram
    class Floor {
        -int level
        -Tile[][] tiles
        -boolean[][] seen
        -List~Room~ rooms
        -List~Monster~ monsters
        -List~FloorItem~ items
        -Position stairs
        -Position startPos
        +WIDTH$ int
        +HEIGHT$ int
        +getTile(int x, int y) Tile
        +isWalkable(Position p) boolean
        +getMonsterAt(Position p) Monster
        +getItemAt(Position p) FloorItem
        +removeMonster(Monster m)
        +addItem(FloorItem fi)
        +removeItem(FloorItem fi)
        +roomAt(Position p) Room
        +isSeen(int x, int y) boolean
        +markSeen(int x, int y)
        +exportTiles() List~String~
        +exportSeen() List~String~
        +fromSave(int level, List~String~ tiles, List~String~ seen)$ Floor
    }
    class Tile {
        <<enumeration>>
        WALL
        FLOOR
        CORRIDOR
        DOOR
        STAIRS_DOWN
        -char displayChar
        -char saveChar
        -boolean walkable
        +fromSaveChar(char c)$ Tile
    }
    class Room {
        <<record>>
        int x
        int y
        int width
        int height
        +center() Position
        +contains(Position p) boolean
        +intersects(Room o) boolean
        +randomInside() Position
    }
    class Direction {
        <<enumeration>>
        UP
        DOWN
        LEFT
        RIGHT
        -int dx
        -int dy
        +fromKey(char c)$ Direction
    }
    class DungeonGenerator {
        +generate(int level)$ Floor
        +createTestFloor()$ Floor
        -placeRooms(Floor f)$
        -connectRooms(Floor f)$
        -carveCorridor(Floor f, Position a, Position b)$
        -placeStairs(Floor f)$
        -placeMonsters(Floor f)$
        -placeItems(Floor f)$
        -isReachable(Floor f, Position a, Position b)$ boolean
    }
    class FieldOfView {
        <<P1>>
        +update(Floor f, Position playerPos)$
    }

    Floor *-- Tile
    Floor *-- Room
    Floor o-- Monster
    Floor o-- FloorItem
    DungeonGenerator ..> Floor : creates
    DungeonGenerator ..> MonsterFactory
    DungeonGenerator ..> ItemFactory
    FieldOfView ..> Floor
```

> 생성 절차: ① 전부 WALL로 채움 → ② 크기 4~12 x 3~6인 방을 겹치지 않게 최대 50회 시도해 6~9개 배치 → ③ 방을 x좌표순 정렬 후 i번 ↔ i+1번 중심을 L자 복도로 연결, 방 경계와 만나는 칸은 DOOR → ④ 첫 방에 시작 위치, 마지막 방에 계단(5층은 보스) → ⑤ 시작 방 외 방들에 몬스터·아이템 배치 → ⑥ BFS로 시작 → 계단 도달 확인, 실패 시 재생성.

---

## 6. 영구 성장 · 로그라이트 (B 파트, 필수)

```mermaid
classDiagram
    class Profile {
        -int shards
        -int totalRuns
        -int bestFloor
        -int clears
        -int totalKills
        -Map upgradeLevels
        -Set unlockedJobs
        +newProfile()$ Profile
        +getShards() int
        +getLevel(UpgradeType t) int
        +canBuy(UpgradeType t) boolean
        +buy(UpgradeType t) boolean
        +isUnlocked(Job job) boolean
        +applyTo(Player p)
        +recordRun(RunResult r, int shardsEarned)
    }
    class UpgradeType {
        <<enumeration>>
        MAX_HP
        ATTACK
        DEFENSE
        START_POTION
        BAG_SIZE
        EXP_BOOST
        SHARD_BOOST
        UNLOCK_MAGE
        UNLOCK_ROGUE
        -String displayName
        -String description
        -int maxLevel
        -int[] costs
        -int valuePerLevel
        +costFor(int nextLevel) int
    }
    class RunResult {
        -int deepestFloor
        -int kills
        -int finalLevel
        -int gold
        -boolean victory
        -String deathCause
        -int turns
        +addKill()
        +reachFloor(int level)
        +finish(Player p, boolean victory, String cause)
    }
    class ShardCalculator {
        +calculate(RunResult r, Profile p)$ int
        +breakdown(RunResult r, Profile p)$ List~String~
    }

    Profile --> UpgradeType
    Profile ..> Player : applyTo
    Profile ..> RunResult : recordRun
    ShardCalculator ..> RunResult
    ShardCalculator ..> Profile : SHARD_BOOST 단계
```

> 흐름: 새 탐험 → `new Player(name, job)` → `profile.applyTo(player)` (HP·공격·방어 증가, 시작 물약 지급, 가방 칸 증가) → 탐험 중 `RunResult`에 처치·도달 층 누적 → 사망/클리어 시 `ShardCalculator.calculate()` → `profile.recordRun()` → 탐험 섹션 삭제 후 저장.
> 경험치 강화(P1)는 `Player.gainExp()`가 받는 값에 배율을 곱한다. 배율은 `applyTo()`에서 `Player`에 넣어 둔다.

---

## 7. 세이브 / 로드 (A 파트, 필수)

```mermaid
classDiagram
    class SaveManager {
        -Path saveDir
        +SLOT_COUNT$ int
        +save(int slot, SlotData data)
        +load(int slot) SlotData
        +exists(int slot) boolean
        +getSlotSummary(int slot) String
        +clearRun(int slot, Profile updated)
        -toLines(SlotData data) List~String~
        -writeAtomically(Path file, List~String~ lines)
    }
    class SlotData {
        <<record>>
        int version
        LocalDateTime savedAt
        Profile profile
        RunData run
        +hasRun() boolean
    }
    class RunData {
        <<record>>
        int turn
        Player player
        Floor floor
        RunResult result
        +capture(Player p, Floor f, int turn, RunResult r)$ RunData
    }
    class SaveFileParser {
        -Map sections
        +parse(List~String~ lines)$ SaveFileParser
        +getString(String section, String key) String
        +getInt(String section, String key) int
        +getList(String section, String key) List~String~
    }
    class SaveLoadException {
        +SaveLoadException(String msg, Throwable cause)
    }
    class RankingManager {
        <<P1>>
        -Path file
        +record(String name, int score, String cause)
        +top(int n) List~String~
    }

    SaveManager ..> SlotData
    SaveManager ..> SaveFileParser
    SaveManager ..> SaveLoadException : throws
    SlotData --> Profile
    SlotData --> RunData
    RunData --> Player
    RunData --> Floor
    RunData --> RunResult
    SaveManager ..> ItemFactory : createById
    SaveManager ..> MonsterFactory : create
    SaveManager ..> Floor : fromSave
    SaveLoadException --|> RuntimeException
```

**파일 구조**: 프로필 섹션 `[META]`, `[PROFILE]`, `[UPGRADES]`는 항상 있고, 탐험 섹션 `[RUN]`, `[PLAYER]`, `[INVENTORY]`, `[IDENTIFY]`(P1), `[FLOOR]`(타일·탐험 20줄씩), `[MONSTERS]`, `[ITEMS]`는 진행 중인 탐험이 있을 때만 쓴다.

**저장 시점**
- 슬롯 처음 생성, 제단에서 강화 구매, 탐험 시작 직후, `S` 저장 후 종료, 사망·클리어 직후
- 항상 `slotN.sav.tmp`에 쓰고 `slotN.sav`로 원자적 교체

**이어하기**
1. 슬롯 요약 출력 (예: `1) 파편 85 | 최고 3층 | 탐험 6회 | 진행 중: 용사 Lv.3 전사 지하 2층`, 빈 슬롯은 `(새 슬롯)`)
2. `Files.readAllLines()` → `SaveFileParser.parse()` → `SlotData`
3. `hasRun()`이면 `Floor.fromSave()`로 맵 복원 → `MonsterFactory.create(type, pos)` 후 HP 덮어쓰기 → `ItemFactory.createById()`로 바닥·가방·장비 복원
4. 실패 시 `SaveLoadException` → 안내 후 메인 메뉴 (탐험 섹션만 손상이면 프로필만 살릴지 확인)

**영구 죽음 + 영구 성장**
- `GameManager.endRun()`: `runResult.finish()` → 파편 계산 → `profile.recordRun()` → `saveManager.clearRun(slot, profile)` (탐험 섹션 없이 프로필만 다시 씀) → 결과 화면 → 슬롯 메뉴

```mermaid
sequenceDiagram
    participant GM as GameManager
    participant SC as ShardCalculator
    participant PR as Profile
    participant SM as SaveManager
    participant FS as saves/slot1.sav

    Note over GM,FS: 저장 후 종료
    GM->>SM: save(1, SlotData(profile, RunData.capture(...)))
    SM->>FS: slot1.sav.tmp 쓰기 후 교체

    Note over GM,FS: 사망 또는 클리어
    GM->>GM: runResult.finish(player, victory, cause)
    GM->>SC: calculate(runResult, profile)
    SC-->>GM: 61 파편
    GM->>PR: recordRun(runResult, 61)
    GM->>SM: clearRun(1, profile)
    SM->>FS: 프로필 섹션만 다시 쓰기 (탐험 삭제)
    GM->>GM: 결과 화면 → 슬롯 메뉴

    Note over GM,FS: 제단에서 강화 구매
    GM->>PR: buy(MAX_HP)
    GM->>SM: save(1, SlotData(profile, null))
```

---

## 8. 핵심 시퀀스: 한 턴의 흐름

```mermaid
sequenceDiagram
    participant GM as GameManager
    participant V as ConsoleView
    participant TS as TurnSystem
    participant F as Floor
    participant CS as CombatSystem
    participant AI as MonsterAI

    GM->>V: render(floor, player, log, turn)
    GM->>GM: input.readLine() → CommandParser.parse()
    GM->>TS: executePlayer(cmd, player, floor)
    alt 이동 방향에 몬스터 있음
        TS->>CS: attack(player, monster, log)
        CS-->>TS: AttackResult
        opt killed
            TS->>CS: rewardKill(player, monster, floor, log)
        end
    else 이동 가능
        TS->>F: isWalkable(next)
        TS->>TS: player.setPos(next)
    end
    TS-->>GM: 턴 소모 여부
    opt 턴 소모
        GM->>TS: runMonsters(player, floor)
        loop 층의 모든 몬스터
            TS->>AI: act(monster, player, floor, log)
        end
        GM->>GM: turn++, 사망 확인
    end
```
