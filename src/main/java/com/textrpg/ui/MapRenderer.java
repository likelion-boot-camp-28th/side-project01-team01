package com.textrpg.ui;

import java.util.List;

import com.textrpg.entity.Player;
import com.textrpg.map.Floor;

/**
 * 층을 화면에 찍을 문자 줄들로 바꾼다. 출력은 하지 않고 줄 목록만 돌려준다 (출력은 ConsoleView).
 *
 * <p>한 칸에 여러 개가 겹치면 우선순위: 플레이어 {@code @} → 몬스터 기호 → 바닥 아이템 기호 → 타일 기호.
 * P1 시야를 적용하면 안 본 칸은 공백, 본 적 있지만 지금 안 보이는 칸은 타일만 보인다(몬스터·아이템 숨김).
 */
public class MapRenderer {

    /**
     * 층 전체를 문자 줄로 만든다.
     *
     * @param floor  그릴 층
     * @param player 플레이어 (위치에 @ 를 찍는다)
     * @return Floor.HEIGHT 줄, 각 줄 Floor.WIDTH 자
     */
    public List<String> render(Floor floor, Player player) {
        // TODO(A): y 0 ~ HEIGHT-1, x 0 ~ WIDTH-1 마다 symbolAt 으로 한 줄씩 만들기
        throw new UnsupportedOperationException("MapRenderer.render 는 아직 구현되지 않았습니다.");
    }

    /** (x, y) 칸에 찍을 문자. 우선순위는 클래스 설명 참고. */
    private char symbolAt(Floor floor, Player player, int x, int y) {
        // TODO(A): 플레이어 → floor.getMonsterAt → floor.getItemAt → floor.getTile(x, y).getDisplayChar()
        throw new UnsupportedOperationException("MapRenderer.symbolAt 은 아직 구현되지 않았습니다.");
    }
}
