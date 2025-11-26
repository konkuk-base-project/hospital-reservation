package service.common;

import java.util.Map;

import service.Command;

public class HelpCommand implements Command {
    private final Map<String, Command> commands;
    private String context; // Main / User / Admin / Doctor

    public HelpCommand(Map<String, Command> commands, String context) {
        this.commands = commands;
        this.context = context;
    }

    @Override
    public void execute(String[] args) {
        // 인자가 있을 경우 (특정 명령어 설명)
        if (args.length == 1) {
            String cmd = args[0];
            Command command = commands.get(cmd);
            if (command == null) {
                System.out.printf("[오류] '%s'는 존재하지 않는 명령어입니다.%n", cmd);
                System.out.println("사용 가능한 명령어를 확인하려면 'help'를 입력하세요.");
                return;
            }
            return;
        }

        // 인자 없을 때 → 현재 프롬프트에 맞는 목록 출력
        System.out.println("======================================================================================");
        System.out.println("사용 가능한 명령어");
        System.out.println("======================================================================================");

        switch (context) {
            case "Admin" -> printAdminCommands();
            case "User" -> printUserCommands();
            case "Doctor" -> printDoctorCommands();
            default -> printMainCommands();
        }

        System.out.println("======================================================================================");
    }

    private void printMainCommands() {
        System.out.println("signup - 새로운 환자 회원 가입");
        System.out.println("signup-doctor - 새로운 의사 회원가입");
        System.out.println("login - 기존 회원 로그인");
        System.out.println("help - 도움말 표시");
        System.out.println("exit - 프로그램 종료");
    }

    private void printAdminCommands() {
        System.out.println("[회원 관리]");
        System.out.println("user - 회원 기본 정보 조회 (옵션: resv 입력 시 예약 내역 포함)");
        System.out.println("[예약 관리]");
        System.out.println("reserve-list - 날짜별 전체 예약 현황 확인");
        System.out.println("[시스템]");
        System.out.println("logout - 로그아웃");
        System.out.println("help - 도움말 표시");
        System.out.println("exit - 프로그램 종료");
    }

    private void printUserCommands() {
        System.out.println("[예약 관리]");
        System.out.println("reserve - 새로운 예약 생성");
        System.out.println("check - 예약 조회");
        System.out.println("modify - 예약 수정");
        System.out.println("cancel - 예약 취소");
        System.out.println("reserve-major - 진료과로 예약 생성");
        System.out.println("[조회 기능]");
        System.out.println("mylist - 내 예약 목록 전체 조회");
        System.out.println("dept - 진료과별 예약 가능 시간 검색");
        System.out.println("doctor - 의사별 예약 가능 시간 검색");
        System.out.println("[시스템]");
        System.out.println("logout - 로그아웃");
        System.out.println("help - 도움말 표시");
        System.out.println("exit - 프로그램 종료");
    }

    private void printDoctorCommands() {
        System.out.println("[진료일정관리]");
        System.out.println("set-schedule - 진료일정설정");
        System.out.println("view-schedule - 진료일정조회");
        System.out.println("modify-schedule - 진료일정수정");
        System.out.println("delete-schedule - 진료일정삭제");
        System.out.println("[시스템]");
        System.out.println("logout - 로그아웃");
        System.out.println("help - 도움말 표시");
        System.out.println("exit - 프로그램 종료");
    }

    public void updateContext(String context) {
        this.context = context;
    }
}