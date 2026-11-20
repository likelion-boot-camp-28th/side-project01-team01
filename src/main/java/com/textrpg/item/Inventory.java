package com.textrpg.item;

import java.util.ArrayList;
import java.util.List;

/**
 * 가방. 기본 CAPACITY 칸이고, 영구 강화(큰 가방, P1)로 칸이 늘어난다.
 * 번호(index)는 0부터 시작한다. 화면에 1번부터 보여 주는 변환은 UI 가 한다.
 */
public class Inventory {

    public static final int CAPACITY = 12;

    private final List<Item> items = new ArrayList<>();
    private int capacity = CAPACITY;

    /** 가방에 넣는다. 가득 차 있으면 넣지 않고 false. */
    public boolean add(Item item) {
        if (isFull()) {
            return false;
        }
        items.add(item);
        return true;
    }

    /** index 번 아이템을 꺼내서 돌려준다. 번호가 잘못되면 IndexOutOfBoundsException. */
    public Item remove(int index) {
        return items.remove(index);
    }

    /** index 번 아이템을 꺼내지 않고 본다. 번호가 잘못되면 IndexOutOfBoundsException. */
    public Item get(int index) {
        return items.get(index);
    }

    public boolean isFull() {
        return items.size() >= capacity;
    }

    public int size() {
        return items.size();
    }

    public int getCapacity() {
        return capacity;
    }

    /** 가방 칸을 늘린다 (큰 가방 강화, P1). */
    public void addCapacity(int extra) {
        capacity += Math.max(0, extra);
    }

    /** 바깥에서 고칠 수 없는 복사본을 돌려준다. */
    public List<Item> getItems() {
        return List.copyOf(items);
    }
}
