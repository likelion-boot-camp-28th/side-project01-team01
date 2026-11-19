package com.textrpg.map;

/**
 * DungeonGenerator / Floor 확인용. JUnit 대신 main 으로 실행한다 (Team_Rules 2.11).
 * 실패가 하나라도 있으면 종료 코드 1.
 * 생성 로직을 구현한 뒤 build.gradle 의 checkClasses 에 추가한다 (지금 넣으면 build 가 실패한다).
 */
public final class DungeonGeneratorCheck {

    private DungeonGeneratorCheck() {
    }

    public static void main(String[] args) {
        // TODO(D): RandomUtil.setSeed() 로 고정한 뒤 1 ~ 5층을 각 1000회 생성해서 확인
        //  - 방 개수 6 ~ 9, 방끼리 겹침 없음, 모든 방이 맵 안
        //  - 시작 → 계단(5층은 보스) 도달, 모든 방 중심이 시작에서 도달 가능
        //  - 1 ~ 4층은 계단 1개, 5층은 계단 0개 + 보스 1마리
        //  - 몬스터·아이템 개수 범위, 같은 칸에 두 개 이상 없음, 시작 위치 비어 있음
        //  - exportTiles / exportSeen / exportRooms → fromSave 왕복 결과가 원본과 같음
        throw new UnsupportedOperationException("DungeonGeneratorCheck.main 은 아직 구현되지 않았습니다.");
    }
}
