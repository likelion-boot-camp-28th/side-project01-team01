package com.textrpg.map;

import com.textrpg.entity.Position;

/**
 * 직사각형 방.
 * x, y, width, height 는 바닥(내부) 영역 기준이고, 벽은 그 바깥 한 칸 둘레(x-1 ~ x+width, y-1 ~ y+height)다.
 */
public record Room(int x, int y, int width, int height) {

    /**
     * 세이브 문자열 "x,y,width,height" 를 방으로 복원한다.
     *
     * @throws IllegalArgumentException 형식이 잘못된 경우
     */
    public static Room fromSaveString(String s) {
        // TODO(D): "x,y,width,height" 파싱
        throw new UnsupportedOperationException("TODO(D): Room.fromSaveString");
    }

    /** 방 내부의 중심 좌표. 복도 연결 기준점으로 쓴다. */
    public Position center() {
        // TODO(D): 중심 좌표 계산
        throw new UnsupportedOperationException("TODO(D): Room.center");
    }

    /** p 가 방 내부(바닥)에 있으면 true. 벽은 포함하지 않는다. */
    public boolean contains(Position p) {
        // TODO(D): 내부 영역 포함 판정
        throw new UnsupportedOperationException("TODO(D): Room.contains");
    }

    /** p 가 벽까지 포함한 방 영역에 있으면 true. */
    public boolean containsWithWalls(Position p) {
        // TODO(D): 벽 포함 영역 판정
        throw new UnsupportedOperationException("TODO(D): Room.containsWithWalls");
    }

    /** 벽까지 포함한 영역끼리 겹치면 true. 두 방이 벽을 공유하지 않게 하기 위해서다. */
    public boolean intersects(Room o) {
        // TODO(D): 벽 포함 영역 겹침 판정
        throw new UnsupportedOperationException("TODO(D): Room.intersects");
    }

    /** 방 내부의 무작위 좌표. 난수는 RandomUtil 을 사용한다. */
    public Position randomInside() {
        // TODO(D): RandomUtil 로 내부 좌표 선택
        throw new UnsupportedOperationException("TODO(D): Room.randomInside");
    }

    /** 세이브용 문자열 "x,y,width,height". */
    public String toSaveString() {
        // TODO(D): 세이브 문자열 생성
        throw new UnsupportedOperationException("TODO(D): Room.toSaveString");
    }
}
