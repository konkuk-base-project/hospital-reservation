package service.common;

import java.util.Scanner;

import service.Command;

public class ExitCommand implements Command {

    private final String promptName; // Main, User, Admin
    private final Scanner scanner;

    public ExitCommand(String promptName, Scanner scanner) {
        this.promptName = promptName;
        this.scanner = scanner;
    }

    @Override
    public void execute(String[] args) {
        // 비정상 결과 (기획서 기준: 인자 존재 시)
        if (args.length > 0) {
            System.out.println("[오류] 불필요한 인자가 입력되었습니다. (형식: exit)");
            System.out.printf("%s >%n", promptName);
            return;
        }

        // 정상 동작
        System.out.print("프로그램을 종료하시겠습니까? (Y/N): ");
        String input = scanner.nextLine().trim();

        if (input.equalsIgnoreCase("Y")) {
            System.out.println("프로그램을 종료합니다. 감사합니다.");
            System.out.println("[프로그램 종료]");
            System.exit(0);
        } else if (input.equalsIgnoreCase("N")) {
            System.out.println("종료가 취소되었습니다.");
            System.out.printf("%s >%n", promptName);
        }
    }
}
