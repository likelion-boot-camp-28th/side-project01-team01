package com.textrpg.ui;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * 모든 키보드 입력의 단일 창구. Scanner 는 프로그램 전체에서 이 객체 하나만 만든다.
 *
 * <p>입력 프롬프트와 입력 오류 안내("1 ~ 3 사이의 숫자를 입력하세요.")는 입력과 붙어 있으므로 이 클래스가 출력한다
 * (Team_Rules 1.2 의 예외). 읽고 쓰는 대상은 생성자로 받을 수 있어, 콘솔 없이 InputHandlerCheck 로 확인한다.
 */
public class InputHandler {

    private final Scanner scanner;
    private final PrintStream out;

    /** 게임에서 쓰는 기본 생성자: 키보드(System.in)에서 읽고 프롬프트는 콘솔(System.out)에 쓴다. */
    public InputHandler() {
        this(System.in, System.out);
    }

    /** 확인용: 미리 준비한 입력과 프롬프트를 받을 출력 대상을 넣는다. */
    InputHandler(InputStream in, PrintStream out) {
        this.scanner = new Scanner(in, StandardCharsets.UTF_8);
        this.out = out;
    }

    /**
     * 한 줄을 읽어 앞뒤 공백을 제거해 돌려준다. 전각 공백(U+3000) 같은 유니코드 공백도 지운다.
     *
     * @throws InputClosedException 입력 스트림이 끝났을 때
     */
    public String readLine(String prompt) {
        out.print(prompt);
        out.flush();
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
            out.println(min + " ~ " + max + " 사이의 숫자를 입력하세요.");
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
