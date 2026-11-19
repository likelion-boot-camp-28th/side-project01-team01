# Into the Dark

순수 Java 17(외부 라이브러리 없음)로 만드는 콘솔 로그라이크 던전 크롤러.
기획서와 클래스 다이어그램은 `docs/` 폴더(또는 팀 공유 문서)를 참고한다.

## 실행 방법

필요한 것: JDK 17 이상. Gradle은 래퍼(`gradlew`)가 자동으로 받으므로 따로 설치하지 않아도 된다.

```bash
./gradlew build                 # 컴파일 + jar 생성
./gradlew run                   # 바로 실행 (IntelliJ 콘솔이면 --args="--no-clear")
java -jar build/libs/into-the-dark-0.1.0.jar
```

- Windows는 `gradlew.bat build` / `gradlew.bat run`.
- 한글이 `???`로 깨지면: `java -Dstdout.encoding=UTF-8 -jar ...` 로 실행하거나, Windows 터미널에서 먼저 `chcp 65001`.
- 화면 지우기(ANSI)가 안 되는 콘솔(IntelliJ 실행 창 등)은 `--no-clear` 옵션을 준다.

## 패키지와 담당

| 패키지 | 담당 | 내용 |
|---|---|---|
| `game`, `ui`, `save`, `Main` | @nullsector12 | 게임 루프, 명령 입력, 화면 렌더링, 세이브/로드, 제단 화면 |
| `entity`, `item`, `meta` | @efkoskf | 플레이어·직업·스탯, 아이템·가방, 영구 성장 |
| `monster`, `combat` | @oeuns2219 | 몬스터·AI, 충돌 전투 |
| `map` | @gunheekk | 타일 맵, 방·복도 생성 |
| `util` | @nullsector12 | `RandomUtil` |

## 공통 클래스 (이미 구현됨, 변경 시 팀 공유 필수)

| 클래스 | 용도 |
|---|---|
| `entity.Position` | 좌표 record. x=열(0~59), y=행(0~19). `move()`, `distanceTo()`, `isAdjacent()` |
| `map.Direction` | `UP/DOWN/LEFT/RIGHT`, `fromKey('w')` |
| `map.Tile` | 타일 종류, 화면 문자/세이브 문자/통행 가능 여부 |
| `entity.Stats` | HP·MP·공격·방어·명중·치명타·회피 (확률은 퍼센트 0~100) |
| `entity.Entity` | 플레이어·몬스터 공통 부모. `takeDamage()`, `heal()`, `getAttack()`/`getDefense()` 추상 |
| `item.ItemType` | 아이템 분류와 맵 기호 |
| `game.MessageLog` | 게임 메시지 기록. `add()`, `recent(n)` |
| `ui.ConsoleView` | 모든 출력의 창구. `printLine()`, `printMenu()`, `bar()` |
| `ui.InputHandler` | 모든 입력의 창구. `readLine()`, `readInt()`, `confirm()` |
| `util.RandomUtil` | 모든 난수의 창구. `nextInt(min, max)`, `chance(%)`, `setSeed()` |

## 규칙 문서

- [Git_Convention.md](docs/Git_Convention.md): 브랜치 전략, 커밋 메시지, PR 규칙
- [Team_Rules.md](docs/Team_Rules.md): 협업 규칙, 프로젝트 고유 규칙, 코드 컨벤션