package service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import repository.AuthRepository;
import repository.PatientRepository;
import service.admin.AdminService;
import service.admin.ReserveListCommand;
import service.admin.UserSearchCommand;
import service.auth.AuthService;
import service.auth.LoginCommand;
import service.auth.LogoutCommand;
import service.auth.SignupCommand;
import service.common.HelpCommand;
import service.reservation.CancelCommand;
import service.reservation.CheckCommand;
import service.reservation.ModifyCommand;
import service.reservation.ReservationService;
import service.reservation.ReserveCommand;
import service.search.DeptCommand;
import service.search.DoctorCommand;
import service.search.MyListCommand;
import service.search.SearchService;

public class CommandHandler {
    private final AuthContext authContext;
    private final Map<String, Command> commands;
    private final HelpCommand helpCommand;

    public CommandHandler() {
        this.authContext = new AuthContext();
        PatientRepository patientRepository = new PatientRepository();
        AuthRepository authRepository = new AuthRepository();
        AuthService authService = new AuthService(patientRepository, authRepository, authContext);

        SearchService searchService = new SearchService(authContext);
        ReservationService reservationService = new ReservationService(authContext); // 추가
        AdminService adminService = new AdminService();

        this.commands = new HashMap<>();
        // 인증
        commands.put("signup", new SignupCommand(authService));
        commands.put("login", new LoginCommand(authService));
        commands.put("logout", new LogoutCommand(authService));

        // 검색 및 조회
        commands.put("mylist", new MyListCommand(searchService));
        commands.put("dept", new DeptCommand(searchService));
        commands.put("doctor", new DoctorCommand(searchService));

        // 예약 관리
        commands.put("reserve", new ReserveCommand(reservationService));
        commands.put("check", new CheckCommand(reservationService));
        commands.put("modify", new ModifyCommand(reservationService));
        commands.put("cancel", new CancelCommand(reservationService));

        // 관리자 명령어
        commands.put("user", new UserSearchCommand(adminService));
        commands.put("reserve-list", new ReserveListCommand(adminService));

        // 공통 명령어
        this.helpCommand = new HelpCommand(commands, "Main");
        commands.put("help", helpCommand);

        // 새로운 명령어 추가 시 여기 등록

    }

    public String getPrompt() {
        return authContext.getPrompt();
    }

    public boolean handle(String input) {
        if (input.trim().isEmpty()) {
            return false;
        }

        String[] tokens = input.split("\\s+");
        String commandName = tokens[0].toLowerCase();
        String[] args = Arrays.copyOfRange(tokens, 1, tokens.length);

        if ("exit".equals(commandName)) {

            // exit 뒤에 인자가 있을 경우 — 오류 메시지 출력
            if (tokens.length > 1) {
                System.out.println("[오류] 불필요한 인자가 입력되었습니다. (형식: exit)");
                return false;
            }

            System.out.print("프로그램을 종료하시겠습니까? (Y/N): ");
            Scanner scanner = new Scanner(System.in);
            String confirm = scanner.nextLine();
            // Y/y 입력
            if (confirm.equalsIgnoreCase("Y")) {
                System.out.println("프로그램을 종료합니다. 감사합니다.");
                System.out.println("[프로그램 종료]");
                return true; 
            }

            // N/n 입력
            if (confirm.equalsIgnoreCase("N")) {
                System.out.println("종료가 취소되었습니다.");
                return false; 
            }

            // 별다른 메시지 없이 Main 프롬프트로 복귀
            return false;
        }

        Command command = commands.get(commandName);

        if (commandName.equals("help") && command instanceof service.common.HelpCommand helpCmd) {
            helpCmd.updateContext(authContext.getPrompt());
        }

        /* 관리자 전용 명령어 차단 로직 추가 */
        if (authContext.getPrompt().equals("User") &&
                (commandName.equals("user") || commandName.equals("reserve-list"))) {
            System.out.println("[오류] 알 수 없는 명령어입니다. 'help'를 입력하여 도움말을 확인하세요.");
            return false;
        }

        if (command != null) {
            command.execute(args);
        } else {
            System.out.println("[오류] 알 수 없는 명령어입니다. 'help'를 입력하여 도움말을 확인하세요.");
        }
        return false;
    }
}