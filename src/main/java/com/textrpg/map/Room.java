package com.textrpg.map;

import com.textrpg.core.Position;

/**
 * 직사각형 방.
 * x, y, width, height 는 바닥(내부) 영역 기준이고, 벽은 그 바깥 한 칸 둘레(x-1 ~ x+width, y-1 ~ y+height)다.
 *
 * @param x      내부 왼쪽 위 칸의 x
 * @param y      내부 왼쪽 위 칸의 y
 * @param width  내부 너비 (벽 제외)
 * @param height 내부 높이 (벽 제외)
 */
public record Room(int x, int y, int width, int height) {

    /**
     * 세이브 문자열을 방으로 복원한다.
     *
     * @param s toSaveString() 형식 "x,y,width,height"
     * @return 복원한 방
     * @throws IllegalArgumentException 숫자가 4개가 아니거나 숫자로 바꿀 수 없을 때
     */
    public static Room fromSaveString(String s) {
        // TODO(D): 쉼표로 나눠 정수 4개 파싱
        throw new UnsupportedOperationException("Room.fromSaveString 은 아직 구현되지 않았습니다.");
    }

    /**
     * 방 내부의 중심 좌표. 복도 연결 기준점으로 쓴다.
     *
     * @return 중심 좌표 (내부에 있는 칸)
     */
    public Position center() {
        // TODO(D): x + width / 2, y + height / 2
        throw new UnsupportedOperationException("Room.center 는 아직 구현되지 않았습니다.");
    }

    /**
     * p 가 방 내부(바닥)에 있는지. 벽은 포함하지 않는다.
     *
     * @param p 검사할 좌표
     * @return 내부에 있으면 true
     */
    public boolean contains(Position p) {
        // TODO(D): x <= p.x < x + width, y <= p.y < y + height
        throw new UnsupportedOperationException("Room.contains 는 아직 구현되지 않았습니다.");
    }

    /**
     * p 가 벽까지 포함한 방 영역에 있는지.
     *
     * @param p 검사할 좌표
     * @return 벽 포함 영역에 있으면 true
     */
    public boolean containsWithWalls(Position p) {
        // TODO(D): 범위를 한 칸씩 넓혀 contains 와 같은 방식으로 검사
        throw new UnsupportedOperationException("Room.containsWithWalls 는 아직 구현되지 않았습니다.");
    }

    /**
     * 벽까지 포함한 영역끼리 겹치는지. 두 방이 벽을 공유하지 않게 하기 위해 벽까지 본다.
     *
     * @param o 비교할 방
     * @return 겹치면 true
     */
    public boolean intersects(Room o) {
        // TODO(D): 벽 포함 사각형 두 개의 겹침 판정
        throw new UnsupportedOperationException("Room.intersects 는 아직 구현되지 않았습니다.");
    }

    /**
     * 방 내부의 무작위 좌표. 난수는 RandomUtil 을 사용한다 (Team_Rules 1.2).
     *
     * @return 내부의 무작위 칸
     */
    public Position randomInside() {
        // TODO(D): RandomUtil.nextInt 로 x, y 선택
        throw new UnsupportedOperationException("Room.randomInside 는 아직 구현되지 않았습니다.");
    }

    /**
     * 세이브용 문자열.
     *
     * @return "x,y,width,height"
     */
    public String toSaveString() {
        // TODO(D): 쉼표로 이어 붙이기
        throw new UnsupportedOperationException("Room.toSaveString 은 아직 구현되지 않았습니다.");
    }
}
