# 팀 규칙 · 코드 컨벤션

Git 사용 규칙은 [Git_Convention.md](Git_Convention.md)를 따른다.
이 문서의 코드 컨벤션은 Oracle Java Code Conventions와 Google Java Style Guide에서 순수 Java 프로젝트에 가장 널리 쓰이는 규칙을 골라, IntelliJ 기본 포맷과 충돌하지 않게 정리한 것이다.

---

## 1. 협업 규칙

### 1.1 담당과 소통
- 패키지별 담당자가 있다 (README의 담당표). **다른 사람 담당 패키지를 고칠 때는 먼저 담당자에게 알린다.**
- 공통 클래스(`Position`, `Direction`, `Tile`, `Stats`, `Entity`, `ItemType`, `MessageLog`, `ConsoleView`, `InputHandler`, `RandomUtil`)와 public 메서드 시그니처를 바꿀 때는 **변경 전에 팀 전체에 공유**한다.
- 매일 10분 진행 공유: 어제 한 일 / 오늘 할 일 / 막힌 점.
- 30분 넘게 막히면 혼자 붙잡지 말고 바로 공유한다.

### 1.2 프로젝트 고유 규칙
- 외부 라이브러리를 추가하지 않는다. `../build.gradle`의 `dependencies`는 비워 둔다.
- `System.out`, `System.in`을 직접 쓰지 않는다. 출력은 `ConsoleView`, 입력은 `InputHandler`만 사용한다. 단, 입력 프롬프트와 입력 오류 안내("1 ~ 3 사이의 숫자를 입력하세요.")는 입력과 붙어 있으므로 `InputHandler`가 출력한다.
- 게임 중 발생하는 문장(전투, 획득, 이벤트)은 `MessageLog.add()`로 남긴다.
- `new Random()`을 직접 만들지 않는다. 모든 난수는 `RandomUtil`을 사용한다.
- 몬스터·아이템은 각 Factory의 id/type 기반 메서드로만 생성한다 (세이브 복원과 같은 경로를 쓰기 위해).
- 밸런스 수치(HP, 공격력, 비용, 확률)는 enum이나 상수로 한곳에 모은다.
- 코드를 올리기 전에 `./gradlew build`가 경고 없이 통과하는지 확인한다.

---

## 2. 코드 컨벤션

### 2.1 파일과 인코딩
- 소스 파일 인코딩은 **UTF-8**, 줄바꿈은 **LF** (`.gitattributes`로 자동 처리).
- 파일 하나에 최상위 public 클래스 하나. 파일명은 클래스명과 같게.
- 파일 끝에는 빈 줄 하나를 둔다.

### 2.2 이름 규칙

| 대상 | 규칙 | 예시 |
|---|---|---|
| 패키지 | 모두 소문자, 단어 붙여 쓰기 | `com.textrpg.map` |
| 클래스·인터페이스·enum·record | PascalCase, 명사 | `DungeonGenerator`, `MonsterType` |
| 메서드 | camelCase, 동사로 시작 | `takeDamage()`, `isWalkable()` |
| 변수·필드·매개변수 | camelCase, 의미 있는 명사 | `currentFloor`, `expReward` |
| 상수 (`static final`) | UPPER_SNAKE_CASE | `MAX_FLOOR`, `INVENTORY_CAPACITY` |
| enum 값 | UPPER_SNAKE_CASE | `STAIRS_DOWN`, `SHADOW_KNIGHT` |
| boolean | `is`, `has`, `can`으로 시작 | `isAlive()`, `hasRun()`, `canBuy()` |
| 컬렉션 | 복수형 | `monsters`, `items` |

- 축약어도 단어처럼 쓴다: `HpBar` (O), `HPBar` (X).
- 한 글자 변수는 짧은 반복문(`i`, `j`)과 좌표(`x`, `y`)에만 쓴다.
- 클래스 이름에 `Manager`, `Util`을 남발하지 않는다. 역할이 드러나는 이름을 우선한다.

### 2.3 포맷
- 들여쓰기는 **스페이스 4칸** (탭 사용 금지). IntelliJ 기본값.
- 한 줄은 **최대 120자**. 넘으면 연산자·쉼표 뒤에서 줄을 바꾸고 8칸 들여쓴다.
- 중괄호는 **K&R 스타일**: 여는 중괄호는 같은 줄 끝에.
- `if`, `for`, `while`은 **한 줄이어도 중괄호를 생략하지 않는다.**
- 한 줄에 문장 하나, 변수 선언 하나.
- 메서드 사이에는 빈 줄 하나.
- 키워드와 괄호 사이, 이항 연산자 앞뒤, 쉼표 뒤에는 공백 한 칸: `if (hp <= 0) {`, `a + b`, `foo(x, y)`.
- 커밋 전 IntelliJ `Ctrl+Alt+L`(Mac `Cmd+Opt+L`)로 포맷을 맞춘다.

```java
// 좋은 예
if (monster.isAlive()) {
    log.add(monster.getName() + "이(가) 공격한다.");
}

// 나쁜 예
if(monster.isAlive()) log.add(monster.getName()+"이(가) 공격한다.");
```

### 2.4 import
- 와일드카드 import(`import java.util.*;`)를 쓰지 않는다.
- 쓰지 않는 import는 지운다 (IntelliJ `Ctrl+Alt+O`).
- 순서: `java.*` → 그 외 → 프로젝트(`com.textrpg.*`), 그룹 사이 빈 줄 하나. (IntelliJ 기본 정렬을 따르면 된다)

### 2.5 클래스 구성 순서
1. `static final` 상수
2. `static` 필드
3. 인스턴스 필드
4. 생성자
5. `static` 팩토리 메서드
6. public 메서드
7. private 메서드 (호출하는 public 메서드 근처에 두어도 된다)
8. `equals()`, `hashCode()`, `toString()`
9. 중첩 클래스

### 2.6 접근 제어와 불변성
- 필드는 기본 **`private`**. 상속에 꼭 필요할 때만 `protected`.
- 바뀌지 않는 필드는 `final`을 붙인다.
- 값만 담는 클래스는 Java 17 **`record`**를 우선 사용한다 (`Position`, `AttackResult` 등).
- getter는 필요할 때만 만들고, setter는 꼭 필요한 경우에만 만든다.
- 내부 컬렉션을 그대로 반환하지 않는다. `List.copyOf()`나 `Collections.unmodifiableList()`로 감싸 반환한다.
- 인스턴스를 만들 필요가 없는 유틸 클래스는 `final` + `private` 생성자.

### 2.7 메서드
- 메서드는 **한 가지 일**만 한다. 30줄을 넘으면 나눌 수 있는지 먼저 검토한다.
- 매개변수는 3개 이하를 권장, 5개를 넘기지 않는다. 많아지면 객체로 묶는다.
- 조건 중첩이 깊어지면 조기 반환(early return)을 쓴다.
- boolean 매개변수로 동작을 바꾸기보다 메서드를 나눈다.

### 2.8 타입과 표현식
- 변수 타입은 인터페이스로 선언한다: `List<Item> items = new ArrayList<>();`
- `var`는 오른쪽에서 타입이 명확할 때만 쓴다: `var monsters = new ArrayList<Monster>();` (O), `var result = calc();` (X).
- 문자열 비교는 `equals()`, enum 비교는 `==`.
- 반복되는 분기에는 Java 17 **switch 표현식**(`case A -> ...`)을 사용한다.
- 모든 `switch`는 enum 값을 빠짐없이 다루거나 `default`를 둔다.
- 매직 넘버를 쓰지 않는다. 의미 있는 상수나 enum 필드로 뺀다.

```java
// 나쁜 예
if (player.getLevel() * 20 <= exp) { ... }

// 좋은 예
private static final int EXP_PER_LEVEL = 20;
if (player.getLevel() * EXP_PER_LEVEL <= exp) { ... }
```

### 2.9 null과 예외
- 메서드는 가능하면 `null`을 반환하지 않는다. 컬렉션은 빈 컬렉션을, 값이 없을 수 있으면 `Optional`을 반환한다. (예외: 성능이 중요한 맵 조회는 `null` 반환을 허용하되 Javadoc에 명시)
- 잘못된 인자는 `IllegalArgumentException`, 잘못된 상태는 `IllegalStateException`을 던진다.
- **빈 catch 블록 금지.** 무시할 때는 이유를 주석으로 남긴다.
- `catch (Exception e)`로 뭉뚱그려 잡지 않는다. 구체적인 예외를 잡는다.
- 파일 입출력은 `try-with-resources`를 사용한다.
- 사용자 입력 오류로 게임이 종료되면 안 된다. 입력·세이브 파일 오류는 안내 후 다시 묻거나 메뉴로 돌아간다.

### 2.10 주석
- public 클래스와 다른 파트가 호출하는 public 메서드에는 **Javadoc**(`/** */`)을 단다. 무엇을 하는지, 반환값, 예외를 적는다.
- 코드가 "무엇"을 하는지는 코드로 드러내고, 주석은 **"왜"** 그렇게 했는지 적는다.
- 할 일은 `// TODO(담당자): 내용` 형식으로 남긴다. 예: `// TODO(C): 보스 2회 공격 패턴`
- 주석 처리한 옛 코드는 남기지 않는다. Git에 기록이 있다.
- 주석은 한국어로 써도 된다.

### 2.11 테스트와 확인
- 외부 라이브러리를 쓰지 않으므로 JUnit 대신, 핵심 로직은 `main` 메서드를 가진 확인용 클래스를 `src/test/java`에 두어 검증한다. (예: 맵 1000회 생성 후 계단 도달 확인, 세이브 저장 후 다시 읽어 값 비교)
- 난수가 들어간 로직은 `RandomUtil.setSeed()`로 고정해 재현한다.
