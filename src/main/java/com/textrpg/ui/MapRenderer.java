package com.textrpg.ui;

import java.util.ArrayList;
import java.util.List;

import com.textrpg.core.Position;
import com.textrpg.entity.Player;
import com.textrpg.item.FloorItem;
import com.textrpg.map.Floor;
import com.textrpg.monster.Monster;

/**
 * 층을 화면에 찍을 문자 줄들로 바꾼다. 출력은 하지 않고 줄 목록만 돌려준다 (출력은 ConsoleView).
 *
 * <p>한 칸에 여러 개가 겹치면 우선순위: 플레이어 {@code @} → 몬스터 기호 → 바닥 아이템 기호 → 타일 기호.
 * 시야(FieldOfView)에 따라 안 본 칸은 공백, 본 적 있지만 지금 안 보이는 칸은 타일만 보인다(몬스터·아이템 숨김).
 * 시야는 그리기 전에 FieldOfView.update() 로 갱신되어 있어야 한다.
 */
public class MapRenderer {

    /**
     * 층을 시야에 맞춰 문자 줄로 만든다.
     *
     * @param floor  그릴 층
     * @param player 플레이어 (위치에 @ 를 찍는다)
     * @return Floor.HEIGHT 줄, 각 줄 Floor.WIDTH 자 (수정 불가)
     */
    public List<String> render(Floor floor, Player player) {
        List<String> lines = new ArrayList<>(Floor.HEIGHT);
        for (int y = 0; y < Floor.HEIGHT; y++) {
            StringBuilder row = new StringBuilder(Floor.WIDTH);
            for (int x = 0; x < Floor.WIDTH; x++) {
                row.append(symbolAt(floor, player, x, y));
            }
            lines.add(row.toString());
        }
        return List.copyOf(lines);
    }

    /** (x, y) 칸에 찍을 문자. 우선순위는 클래스 설명 참고. */
    private char symbolAt(Floor floor, Player player, int x, int y) {
        Position pos = new Position(x, y);
        if (player.getPos().equals(pos)) {
            return player.getSymbol();
        }
        if (!floor.isSeen(x, y)) {
            return ' ';
        }
        if (!floor.isVisible(x, y)) {
            return floor.getTile(x, y).getDisplayChar();
        }
        Monster monster = floor.getMonsterAt(pos);
        if (monster != null) {
            return monster.getSymbol();
        }
        FloorItem floorItem = floor.getItemAt(pos);
        if (floorItem != null) {
            return floorItem.symbol();
        }
        return floor.getTile(x, y).getDisplayChar();
    }
}
