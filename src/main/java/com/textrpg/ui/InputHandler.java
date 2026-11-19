package com.textrpg.ui;

import java.util.Scanner;

/**
 * 모든 키보드 입력의 단일 창구. Scanner 는 프로그램 전체에서 이 객체 하나만 만든다.
 */
public class InputHandler {

    private final Scanner scanner = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8);

    /** 한 줄을 읽어 앞뒤 공백을 제거해 돌려준다. 입력 스트림이 끝나면 빈 문자열. */
    public String readLine(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        if (!scanner.hasNextLine()) {
            return "";
        }
        return scanner.nextLine().trim();
    }

    /** min~max 사이 정수가 들어올 때까지 다시 묻는다. */
    public int readInt(String prompt, int min, int max) {
        while (true) {
            String line = readLine(prompt);
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // 아래 안내 후 다시 입력받는다.
            }
            System.out.println(min + " ~ " + max + " 사이의 숫자를 입력하세요.");
        }
    }

    /** y/n 확인. y 또는 yes 면 true. */
    public boolean confirm(String prompt) {
        String line = readLine(prompt + " (y/n): ").toLowerCase();
        return line.equals("y") || line.equals("yes");
    }
}
