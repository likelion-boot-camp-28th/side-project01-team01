package com.textrpg.ui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import com.textrpg.CheckRunner;

import static com.textrpg.CheckRunner.expect;
import static com.textrpg.CheckRunner.expectThrows;

/**
 * InputHandler 확인용. 키보드 대신 미리 준비한 입력을 넣고, 프롬프트는 메모리에 받아 확인한다.
 * JUnit 대신 main 으로 실행한다 (Team_Rules 2.11). 실패가 하나라도 있으면 종료 코드 1.
 */
public final class InputHandlerCheck {

    private InputHandlerCheck() {
    }

    public static void main(String[] args) {
        checkReadLine();
        checkReadInt();
        checkConfirm();
        checkClosedInput();

        CheckRunner.finish();
    }

    private static void checkReadLine() {
        ByteArrayOutputStream prompts = new ByteArrayOutputStream();
        InputHandler in = handler("  용사  \n용사　\n　\n", prompts);
        expect("앞뒤 공백을 지운다", in.readLine("이름: "), "용사");
        // 이름 끝 전각 공백이 남으면 세이브 저장에서 예외가 났다 (리뷰 H-1)
        expect("전각 공백도 지운다", in.readLine("이름: "), "용사");
        expect("전각 공백뿐이면 빈 문자열", in.readLine("이름: "), "");
        expect("프롬프트는 출력 대상에 쓴다", prompts.toString(StandardCharsets.UTF_8), "이름: 이름: 이름: ");
    }

    private static void checkReadInt() {
        ByteArrayOutputStream prompts = new ByteArrayOutputStream();
        InputHandler in = handler("abc\n9\n2\n", prompts);
        expect("범위 안의 숫자가 나올 때까지 묻는다", in.readInt("번호: ", 1, 3), 2);
        String guide = "1 ~ 3 사이의 숫자를 입력하세요." + System.lineSeparator();
        expect("잘못 넣을 때마다 안내", prompts.toString(StandardCharsets.UTF_8),
                "번호: " + guide + "번호: " + guide + "번호: ");
    }

    private static void checkConfirm() {
        InputHandler in = handler("y\nYES\nn\n\n", new ByteArrayOutputStream());
        expect("y 는 예", in.confirm("계속할까요?"), true);
        expect("YES 도 예 (대소문자 무시)", in.confirm("계속할까요?"), true);
        expect("n 은 아니오", in.confirm("계속할까요?"), false);
        expect("빈 입력은 아니오", in.confirm("계속할까요?"), false);
    }

    private static void checkClosedInput() {
        InputHandler in = handler("", new ByteArrayOutputStream());
        expectThrows("입력이 끝나면 InputClosedException", InputClosedException.class, () -> in.readLine("> "));
        InputHandler number = handler("abc\n", new ByteArrayOutputStream());
        expectThrows("숫자를 기다리다 입력이 끝나도 InputClosedException", InputClosedException.class,
                () -> number.readInt("번호: ", 1, 3));
    }

    private static InputHandler handler(String input, ByteArrayOutputStream prompts) {
        return new InputHandler(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(prompts, true, StandardCharsets.UTF_8));
    }
}
