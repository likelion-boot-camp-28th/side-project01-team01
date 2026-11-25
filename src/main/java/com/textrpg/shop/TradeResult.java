package com.textrpg.shop;

import java.util.Objects;

/**
 * 상점 거래(구매·판매·치료) 결과. 거래가 이루어졌는지와 화면에 보여 줄 문장을 함께 담는다.
 * 호출한 쪽이 골드 변화 같은 부수 효과로 성공 여부를 추측하지 않아도 되게 한다.
 *
 * @param success 거래가 이루어졌으면 true. 번호가 없거나 골드·가방이 모자라 아무것도 바뀌지 않았으면 false
 * @param message 화면에 보여 줄 결과 문장
 */
public record TradeResult(boolean success, String message) {

    public TradeResult {
        Objects.requireNonNull(message, "message");
    }

    /** 거래가 이루어졌다. */
    public static TradeResult done(String message) {
        return new TradeResult(true, message);
    }

    /** 거래가 이루어지지 않았다. 골드·가방·진열은 그대로다. */
    public static TradeResult rejected(String message) {
        return new TradeResult(false, message);
    }
}
