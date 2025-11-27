package service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import repository.AuthRepository;
import repository.DoctorRepository;
import repository.PatientRepository;
import service.admin.AdminService;
import service.admin.ReserveListCommand;
import service.admin.UserSearchCommand;
import service.auth.AuthService;
import service.auth.LoginCommand;
import service.auth.LogoutCommand;
import service.auth.SignupCommand;
import service.common.HelpCommand;
import service.doctor.DoctorService;
import service.doctor.SetScheduleCommand;
import service.doctor.ViewScheduleCommand;
import service.doctor.ModifyScheduleCommand;
import service.doctor.DeleteScheduleCommand;
import service.reservation.CancelCommand;
import service.reservation.CheckCommand;
import service.reservation.ModifyCommand;
import service.reservation.ReservationService;
import service.reservation.ReserveCommand;
import service.reservation.ReserveMajorCommand;
import service.search.DeptCommand;
import service.search.DoctorCommand;
import service.search.MyListCommand;
import service.search.SearchService;

public class CommandHandler {
    private final AuthContext authContext;
    private final Map<String, Command> commands;
    private final HelpCommand helpCommand;
    private final Scanner scanner = new Scanner(System.in);

    public CommandHandler() {
        this.authContext = new AuthContext();
        PatientRepository patientRepository = new PatientRepository();
        DoctorRepository doctorRepository = new DoctorRepository(); // 추가
        AuthRepository authRepository = new AuthRepository();
        AuthService authService = new AuthService(patientRepository, doctorRepository, authRepository, authContext); // 수정

        SearchService searchService = new SearchService(authContext);
        ReservationService reservationService = new ReservationService(authContext);
        AdminService adminService = new AdminService();
        DoctorService doctorService = new DoctorService(authContext); // 추가

        this.commands = new HashMap<>();

        // 인증
        commands.put("signup", new SignupCommand(authService, false)); // 수정
        commands.put("signup-doctor", new SignupCommand(authService, true)); // 추가
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
        commands.put("reserve-major", new ReserveMajorCommand(reservationService));

        // 관리자 명령어
        commands.put("user", new UserSearchCommand(adminService));
        commands.put("reserve-list", new ReserveListCommand(adminService));

        // 의사 명령어 (추가)
        commands.put("set-schedule", new SetScheduleCommand(doctorService));
        commands.put("view-schedule", new ViewScheduleCommand(doctorService));
        commands.put("modify-schedule", new ModifyScheduleCommand(doctorService));
        commands.put("delete-schedule", new DeleteScheduleCommand(doctorService));

        // 공통 명령어
        this.helpCommand = new HelpCommand(commands, "Main");
        commands.put("help", helpCommand);
    }

    public String readCommand(Scanner scanner) {
        return scanner.nextLine();
    }

    public String getPrompt() {
        return authContext.getPrompt();
    }

    public boolean handle(String input, Scanner scanner) {
        if (input.trim().isEmpty()) {
            return false;
        }

        String[] tokens = input.split("\\s+");
        String commandName = tokens[0].toLowerCase();
        String[] args = Arrays.copyOfRange(tokens, 1, tokens.length);

        if ("exit".equals(commandName)) {
            if (tokens.length > 1) {
                System.out.println("[오류] 불필요한 인자가 입력되었습니다. (형식: exit)");
                return false;
            }

            System.out.print("프로그램을 종료하시겠습니까? (Y/N): ");
            String confirm = scanner.nextLine().trim();

            if (confirm.equalsIgnoreCase("Y")) {
                System.out.println("프로그램을 종료합니다. 감사합니다.");
                System.out.println("[프로그램 종료]");
                return true;
            }

            System.out.println("종료가 취소되었습니다.");
            return false;
        }

        if (commandName.equals("help") && commands.get("help") instanceof HelpCommand helpCmd) {
            helpCmd.updateContext(authContext.getPrompt());
        }

        // 관리자 전용 명령어 접근 차단
        if (!authContext.getPrompt().equals("Admin") &&
                (commandName.equals("user") || commandName.equals("reserve-list"))) {
            System.out.println("[오류] 알 수 없는 명령어입니다. 'help'를 입력하여 도움말을 확인하세요.");
            return false;
        }

        // 의사 전용 명령어 접근 차단 (추가)
        if (!authContext.getPrompt().equals("Doctor") &&
                (commandName.equals("set-schedule") || commandName.equals("view-schedule") ||
                        commandName.equals("modify-schedule") || commandName.equals("delete-schedule"))) {
            System.out.println("[오류] 알 수 없는 명령어입니다. 'help'를 입력하여 도움말을 확인하세요.");
            return false;
        }

        // 기본 명령 처리
        Command command = commands.get(commandName);
        if (command != null) {
            command.execute(Arrays.copyOfRange(tokens, 1, tokens.length));
        } else {
            System.out.println("[오류] 알 수 없는 명령어입니다. 'help'를 입력하여 도움말을 확인하세요.");
        }

        return false;
    }
}