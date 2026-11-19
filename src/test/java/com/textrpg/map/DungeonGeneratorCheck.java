package com.textrpg.map;

/**
 * DungeonGenerator / Floor 확인용. JUnit 대신 main 으로 실행한다 (Team_Rules 2.11).
 * 실패가 하나라도 있으면 종료 코드 1.
 * 생성 로직을 구현한 뒤 build.gradle 의 checkClasses 에 추가한다.
 */
public final class DungeonGeneratorCheck {

    private DungeonGeneratorCheck() {
    }

    public static void main(String[] args) {
        // TODO(D): RandomUtil.setSeed() 고정 후 1~5층 각 1000회 생성 검사
        //  - 방 개수 6~9, 겹침 없음, 맵 안
        //  - 시작 → 계단(5층은 보스) 도달, 모든 방 중심 도달
        //  - 계단 개수(1~4층 1개, 5층 0개 + 보스 1마리)
        //  - 몬스터·아이템 개수 범위, 같은 칸 중복 없음, 시작 위치 비어 있음
        //  - exportTiles/Seen/Rooms → fromSave 왕복
        throw new UnsupportedOperationException("TODO(D): DungeonGeneratorCheck");
    }
}
