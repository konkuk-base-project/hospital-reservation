package service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import repository.AuthRepository;
import repository.PatientRepository;
import service.auth.AuthService;
import service.auth.LoginCommand;
import service.auth.LogoutCommand;
import service.auth.SignupCommand;
import service.search.DeptCommand;
import service.search.DoctorCommand;
import service.search.MyListCommand;
import service.search.SearchService;
import service.reservation.ReservationService;  // 추가
import service.reservation.ReserveCommand;      // 추가
import service.reservation.CheckCommand;        // 추가
import service.reservation.ModifyCommand;       // 추가
import service.reservation.CancelCommand;       // 추가

public class CommandHandler {
    private final AuthContext authContext;
    private final Map<String, Command> commands;

    public CommandHandler() {
        this.authContext = new AuthContext();
        PatientRepository patientRepository = new PatientRepository();
        AuthRepository authRepository = new AuthRepository();
        AuthService authService = new AuthService(patientRepository, authRepository, authContext);

        SearchService searchService = new SearchService(authContext);
        ReservationService reservationService = new ReservationService(authContext);  // 추가

        this.commands = new HashMap<>();
        // 인증
        commands.put("signup", new SignupCommand(authService));
        commands.put("login", new LoginCommand(authService));
        commands.put("logout", new LogoutCommand(authService));

        // 검색 및 조회
        commands.put("mylist", new MyListCommand(searchService));
        commands.put("dept", new DeptCommand(searchService));
        commands.put("doctor", new DoctorCommand(searchService));

        // 예약 관리 (새로 추가!)
        commands.put("reserve", new ReserveCommand(reservationService));
        commands.put("check", new CheckCommand(reservationService));
        commands.put("modify", new ModifyCommand(reservationService));
        commands.put("cancel", new CancelCommand(reservationService));
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
            System.out.print("프로그램을 종료하시겠습니까? (Y/N): ");
            Scanner scanner = new Scanner(System.in);
            String confirm = scanner.nextLine();
            if (confirm.equalsIgnoreCase("Y")) {
                return true;
            } else {
                System.out.println("종료가 취소되었습니다.");
                return false;
            }
        }

        Command command = commands.get(commandName);

        if (command != null) {
            command.execute(args);
        } else {
            System.out.println("[오류] 알 수 없는 명령어입니다. 'help'를 입력하여 도움말을 확인하세요.");
        }
        return false;
    }
}