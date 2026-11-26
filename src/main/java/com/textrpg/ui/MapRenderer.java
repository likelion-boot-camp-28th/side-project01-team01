package com.textrpg.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.textrpg.core.Position;
import com.textrpg.entity.Player;
import com.textrpg.item.FloorItem;
import com.textrpg.map.Floor;
import com.textrpg.map.Tile;
import com.textrpg.monster.Boss;
import com.textrpg.monster.Monster;
import com.textrpg.shop.Shop;

/**
 * 층을 화면에 찍을 문자 줄들로 바꾼다. 출력은 하지 않고 줄 목록만 돌려준다 (출력은 ConsoleView).
 *
 * <p>한 칸에 여러 개가 겹치면 우선순위: 플레이어 {@code @} → 상인 {@code M} → 몬스터 기호 → 바닥 아이템 기호 → 타일 기호.
 * 시야(FieldOfView)에 따라 안 본 칸은 공백, 본 적 있지만 지금 안 보이는 칸은 타일만 보인다(몬스터·아이템 숨김).
 * 상인은 움직이지 않으므로 계단처럼 한 번 본 뒤에는 계속 보인다.
 * 시야는 그리기 전에 FieldOfView.update() 로 갱신되어 있어야 한다.
 *
 * <p>색(Painter.ANSI)을 쓰면 칸마다 Style 색을 입힌다. 몬스터는 빨강, 상인은 굵은 노랑, 아이템은 종류별 색, 지금 안 보이는 칸은 회색이다.
 * 같은 색이 이어지는 칸은 한 번에 감싸 색 코드 수를 줄인다.
 */
public class MapRenderer {

    /**
     * 층을 시야에 맞춰 색 없는 문자 줄로 만든다.
     *
     * @param floor  그릴 층
     * @param player 플레이어 (위치에 @ 를 찍는다)
     * @return Floor.HEIGHT 줄, 각 줄 Floor.WIDTH 자 (수정 불가)
     */
    public List<String> render(Floor floor, Player player) {
        return render(floor, player, Painter.PLAIN);
    }

    /**
     * 층을 시야에 맞춰 문자 줄로 만든다. painter 가 ANSI 면 색 코드가 들어가므로 줄 길이는 WIDTH 보다 길다.
     *
     * @return Floor.HEIGHT 줄. 색 코드를 지우면(Painter.strip) 각 줄 Floor.WIDTH 자 (수정 불가)
     */
    List<String> render(Floor floor, Player player, Painter painter) {
        List<String> lines = new ArrayList<>(Floor.HEIGHT);
        for (int y = 0; y < Floor.HEIGHT; y++) {
            StringBuilder row = new StringBuilder();
            StringBuilder run = new StringBuilder();
            Style runStyle = null;
            for (int x = 0; x < Floor.WIDTH; x++) {
                Glyph glyph = glyphAt(floor, player, x, y);
                if (!run.isEmpty() && !Objects.equals(glyph.style(), runStyle)) {
                    row.append(paint(painter, runStyle, run));
                    run.setLength(0);
                }
                runStyle = glyph.style();
                run.append(glyph.symbol());
            }
            row.append(paint(painter, runStyle, run));
            lines.add(row.toString());
        }
        return List.copyOf(lines);
    }

    private static String paint(Painter painter, Style style, CharSequence text) {
        return style == null ? text.toString() : painter.paint(style, text.toString());
    }

    /** (x, y) 칸에 찍을 문자와 색. 우선순위는 클래스 설명 참고. 색이 없으면(기본 글자색) style 이 null. */
    private Glyph glyphAt(Floor floor, Player player, int x, int y) {
        Position pos = new Position(x, y);
        if (player.getPos().equals(pos)) {
            return new Glyph(player.getSymbol(), Style.PLAYER);
        }
        if (!floor.isSeen(x, y)) {
            return new Glyph(' ', null);
        }
        if (floor.isMerchantAt(pos)) {
            return new Glyph(Shop.MERCHANT_SYMBOL, Style.MERCHANT);
        }
        Tile tile = floor.getTile(x, y);
        if (!floor.isVisible(x, y)) {
            return new Glyph(tile.getDisplayChar(), tile == Tile.STAIRS_DOWN ? Style.STAIRS : Style.REMEMBERED);
        }
        Monster monster = floor.getMonsterAt(pos);
        if (monster != null) {
            return new Glyph(monster.getSymbol(), monster instanceof Boss ? Style.BOSS : Style.MONSTER);
        }
        FloorItem floorItem = floor.getItemAt(pos);
        if (floorItem != null) {
            return new Glyph(floorItem.symbol(), itemStyle(floorItem));
        }
        return new Glyph(tile.getDisplayChar(), tileStyle(tile));
    }

    private static Style itemStyle(FloorItem floorItem) {
        if (floorItem.isGold()) {
            return Style.GOLD;
        }
        return ConsoleView.itemStyle(floorItem.item().getType());
    }

    /** 지금 보이는 타일의 색. 바닥·벽·복도는 기본 글자색이라 회색인 "기억한 칸" 과 구분된다. */
    private static Style tileStyle(Tile tile) {
        return switch (tile) {
            case DOOR -> Style.DOOR;
            case STAIRS_DOWN -> Style.STAIRS;
            case ROCK, WALL, FLOOR, CORRIDOR -> null;
        };
    }

    /** 칸 하나: 찍을 문자와 색 (null 이면 기본 글자색). */
    private record Glyph(char symbol, Style style) {
    }
}
