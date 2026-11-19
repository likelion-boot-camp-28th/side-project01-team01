package com.textrpg.util;

import java.util.List;
import java.util.Random;

/**
 * 게임 전체에서 쓰는 단 하나의 난수 생성기.
 * java.util.Random 을 직접 만들지 말고 반드시 이 클래스를 통한다.
 * 테스트할 때 setSeed() 로 같은 결과를 재현할 수 있다.
 */
public final class RandomUtil {

    private static Random random = new Random();

    private RandomUtil() {
    }

    public static void setSeed(long seed) {
        random = new Random(seed);
    }

    /** min 이상 max 이하의 정수 (양 끝 포함). */
    public static int nextInt(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min > max: " + min + " > " + max);
        }
        return min + random.nextInt(max - min + 1);
    }

    /** percent 확률(0~100)로 true. 예: chance(15) 는 15% 확률. */
    public static boolean chance(double percent) {
        return random.nextDouble() * 100.0 < percent;
    }

    /** min 이상 max 미만의 실수. 데미지 편차(0.9~1.1) 계산용. */
    public static double nextDouble(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    public static <T> T pick(List<T> list) {
        if (list.isEmpty()) {
            throw new IllegalArgumentException("빈 리스트에서는 고를 수 없습니다.");
        }
        return list.get(random.nextInt(list.size()));
    }
}
