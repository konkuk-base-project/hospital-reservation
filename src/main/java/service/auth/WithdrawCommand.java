package service.auth;

import java.util.List;
import java.util.Scanner;
import service.Command;
import util.exception.LoginException;

public class WithdrawCommand implements Command {
    private final AuthService authService;
    private final Scanner scanner;

    public WithdrawCommand(AuthService authService, Scanner scanner) {
        this.authService = authService;
        this.scanner = scanner;
    }

    @Override
    public void execute(String[] args) {
        if (args.length != 1) {
            System.out.println("[오류] 인자의 개수가 올바르지 않습니다. (형식: withdraw <비밀번호>)");
            return;
        }

        String password = args[0];

        try {
            List<String> futureReservations = authService.getFutureReservations(password);

            if (!futureReservations.isEmpty()) {
                System.out.println("======================================================================================");
                System.out.println("[경고] 미래 예약 확인");
                System.out.println("======================================================================================");
                System.out.println("아래의 예약이 자동 취소됩니다:");
                for (String res : futureReservations) {
                    System.out.println("- " + res);
                }
                System.out.println();
            }

            System.out.println("======================================================================================");
            System.out.println("회원 탈퇴 확인");
            System.out.println("======================================================================================");
            System.out.println("정말로 탈퇴하시겠습니까?");
            System.out.println("탈퇴 시 모든 개인정보가 삭제되며 복구할 수 없습니다.");
            System.out.print("계속하시려면 Y, 취소하시려면 N을 입력하세요: ");

            String confirm = scanner.nextLine().trim();

            if (confirm.equalsIgnoreCase("Y")) {
                authService.withdraw(password);
                if (!futureReservations.isEmpty()) {
                    System.out.println();
                    System.out.println("미래 예약 " + futureReservations.size() + "건이 자동 취소되었습니다.");
                }
                System.out.println("회원 탈퇴가 완료되었습니다. 그동안 이용해주셔서 감사합니다.");
                System.out.println("[프로그램 종료]");
                System.exit(0);
            } else {
                System.out.println("탈퇴가 취소되었습니다.");
            }

        } catch (LoginException e) {
            System.out.println("[오류] " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[오류] 탈퇴 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
