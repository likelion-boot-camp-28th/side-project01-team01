package com.textrpg.ui;

import java.util.Scanner;

/**
 * 모든 키보드 입력의 단일 창구. Scanner 는 프로그램 전체에서 이 객체 하나만 만든다.
 */
public class InputHandler {

    private final Scanner scanner = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8);

    /**
     * 한 줄을 읽어 앞뒤 공백을 제거해 돌려준다. 전각 공백(U+3000) 같은 유니코드 공백도 지운다.
     *
     * @throws InputClosedException 입력 스트림이 끝났을 때
     */
    public String readLine(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        if (!scanner.hasNextLine()) {
            throw new InputClosedException();
        }
        // trim() 은 U+0020 이하만 지워 전각 공백이 남는다. 세이브(SaveFileWriter)가 strip() 기준으로
        // 앞뒤 공백을 검사하므로, 이름 끝에 전각 공백이 있으면 저장하다 예외가 났다
        return scanner.nextLine().strip();
    }

    /**
     * min~max 사이 정수가 들어올 때까지 다시 묻는다.
     *
     * @throws InputClosedException 입력 스트림이 끝났을 때
     */
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

    /**
     * y/n 확인. y 또는 yes 면 true.
     *
     * @throws InputClosedException 입력 스트림이 끝났을 때
     */
    public boolean confirm(String prompt) {
        String line = readLine(prompt + " (y/n): ").toLowerCase();
        return line.equals("y") || line.equals("yes");
    }
}
