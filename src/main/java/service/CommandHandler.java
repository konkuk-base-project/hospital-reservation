package service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import repository.AppointmentRepository;
import repository.AuthRepository;
import repository.DoctorRepository;
import repository.MajorRepository;
import repository.PatientRepository;
import service.admin.AddMajorCommand;
import service.admin.AdminService;
import service.admin.ReserveListCommand;
import service.admin.UserSearchCommand;
import service.auth.AuthService;
import service.auth.LoginCommand;
import service.auth.LogoutCommand;
import service.auth.SignupCommand;
import service.auth.WithdrawCommand;
import service.common.HelpCommand;
import service.doctor.CompleteCommand;
import service.doctor.DeleteScheduleCommand;
import service.doctor.DoctorService;
import service.doctor.ModifyScheduleCommand;
import service.doctor.NoshowCommand;
import service.doctor.PendingCommand;
import service.doctor.SetScheduleCommand;
import service.doctor.ViewScheduleCommand;
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
        MajorRepository majorRepository = new MajorRepository();

        PatientRepository patientRepository = new PatientRepository();
        DoctorRepository doctorRepository = new DoctorRepository();
        AuthRepository authRepository = new AuthRepository();
        AppointmentRepository appointmentRepository = new AppointmentRepository();
        AuthService authService = new AuthService(patientRepository, doctorRepository, authRepository,
                appointmentRepository, authContext);

        SearchService searchService = new SearchService(authContext, majorRepository);
        ReservationService reservationService = new ReservationService(authContext);
        AdminService adminService = new AdminService(majorRepository);
        DoctorService doctorService = new DoctorService(authContext);

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
        commands.put("add-major", new AddMajorCommand(majorRepository, authContext));

        // 의사 명령어 (추가)
        commands.put("set-schedule", new SetScheduleCommand(doctorService));
        commands.put("view-schedule", new ViewScheduleCommand(doctorService));
        commands.put("modify-schedule", new ModifyScheduleCommand(doctorService));
        commands.put("delete-schedule", new DeleteScheduleCommand(doctorService));
        commands.put("complete", new CompleteCommand(doctorService));
        commands.put("noshow", new NoshowCommand(doctorService));
        commands.put("pending", new PendingCommand(doctorService));

        // 공통 명령어
        this.helpCommand = new HelpCommand(commands, "Main");
        commands.put("help", helpCommand);

        // 가상시간
        commands.put("time", args -> {
            if (args.length > 0) {
                System.out.println("[오류] 인자가 없어야 합니다.");
                return;
            }
    
        LocalDateTime now = util.file.VirtualTime.currentDateTime();
        System.out.println("[현재 가상 시간] " + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    });

        // 가상시간 설정
        commands.put("settime", args -> handleSetTime(args));

        // 새로운 명령어 추가 시 여기 등록
        commands.put("withdraw", new WithdrawCommand(authService, scanner));
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
                        commandName.equals("modify-schedule") || commandName.equals("delete-schedule") ||
                        commandName.equals("complete") || commandName.equals("noshow") ||
                        commandName.equals("pending"))) {
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

    // ===========================
    // settime 명령어 처리
    // ===========================
    private void handleSetTime(String[] args) {
        if (args.length != 2) {
            System.out.println("[오류] 인자의 개수가 올바르지 않습니다. (형식: settime <날짜 YYYY-MM-DD> <시간 HH:MM>)");
            return;
        }

        String dateStr = args[0];
        String timeStr = args[1];

        boolean hasError = false;

        // 날짜 형식 검증
        if (!dateStr.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            System.out.println("[오류] 날짜 형식이 잘못되었습니다. (예: 2025-10-10)");
            hasError = true;
        } else {
            try {
                LocalDate.parse(dateStr);
            } catch (Exception e) {
                System.out.println("[오류] 날짜 형식이 잘못되었습니다. (예: 2025-10-10)");
                hasError = true;
            }
        }

        // 시간 형식 검증
        if (!timeStr.matches("^\\d{2}:\\d{2}$")) {
            System.out.println("[오류] 시간 형식이 잘못되었습니다. (예: 14:30, 00:00~23:59)");
            hasError = true;
        } else {
            String[] timeParts = timeStr.split(":");
            int HH = Integer.parseInt(timeParts[0]);
            int MM = Integer.parseInt(timeParts[1]);

            if (HH < 0 || HH > 23 || MM < 0 || MM > 59) {
                System.out.println("[오류] 시간 형식이 잘못되었습니다. (예: 14:30, 00:00~23:59)");
                hasError = true;
            }
        }

        // 날짜/시간 둘 중 하나라도 잘못되면 종료
        if (hasError) return;

        // ===== 정상일 때만 여기로 내려옴 =====
        LocalDateTime newTime;
        String full = dateStr + " " + timeStr + ":00";

        try {
            newTime = LocalDateTime.parse(full,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
                System.out.println("[오류] 시간 형식이 잘못되었습니다. (예: 14:30, 00:00~23:59)");
                return;
        }

        LocalDateTime min = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        LocalDateTime max = LocalDateTime.of(2025, 12, 31, 23, 59, 59);

        if (newTime.isBefore(min) || newTime.isAfter(max)) {
                System.out.println("[오류] 설정 가능한 시간 범위를 벗어났습니다. (2025-01-01 ~ 2025-12-31)");
                return;
            }

        util.file.VirtualTime.setTime(newTime);

        System.out.println("가상 시간이 설정되었습니다.");
        System.out.println("현재 가상 시간: " + newTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }
}