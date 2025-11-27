package service.doctor;

import model.User;
import service.AuthContext;
import util.exception.DoctorScheduleException;
import util.file.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

public class DoctorService {
    private final AuthContext authContext;
    private static final Pattern TIME_PATTERN = Pattern.compile("^\\d{2}:\\d{2}$");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final Map<String, String> DAY_MAP_KOR_TO_ENG = Map.of(
            "월", "MON", "화", "TUE", "수", "WED", "목", "THU",
            "금", "FRI", "토", "SAT", "일", "SUN"
    );

    private static final Map<String, String> DAY_MAP_ENG_TO_KOR = Map.of(
            "MON", "월요일", "TUE", "화요일", "WED", "수요일", "THU", "목요일",
            "FRI", "금요일", "SAT", "토요일", "SUN", "일요일"
    );

    public DoctorService(AuthContext authContext) {
        this.authContext = authContext;
    }

    /**
     * 6.6.1 진료 일정 설정
     */
    public void setSchedule(String[] args) throws DoctorScheduleException {
        if (!authContext.isLoggedIn() || !"DOCTOR".equals(authContext.getCurrentUser().getRole())) {
            throw new DoctorScheduleException("의사 계정만 사용할 수 있습니다.");
        }

        if (args.length != 3) {
            throw new DoctorScheduleException("인자의 개수가 올바르지 않습니다. (형식: set-schedule <요일> <시작시간 HH:MM> <종료시간 HH:MM>)");
        }

        String dayInput = args[0].toUpperCase();
        String startTime = args[1];
        String endTime = args[2];

        // 요일 변환
        String dayCode = convertDayCode(dayInput);

        // 시간 검증
        LocalTime start = validateTime(startTime);
        LocalTime end = validateTime(endTime);

        // 시작시간 < 종료시간 검증
        if (!start.isBefore(end)) {
            throw new DoctorScheduleException("시작시간은 종료시간보다 앞서야 합니다.");
        }

        // 진료 시간 범위 검증 (09:00 ~ 18:00)
        if (start.isBefore(LocalTime.of(9, 0)) || end.isAfter(LocalTime.of(18, 0))) {
            throw new DoctorScheduleException("진료 시간은 09:00 ~ 18:00 범위 내에서만 설정 가능합니다.");
        }

        try {
            User currentUser = authContext.getCurrentUser();
            String doctorId = currentUser.getId();
            String masterFilePath = "data/doctor/" + doctorId + "-master.txt";

            List<String> lines = FileUtil.readLines(masterFilePath);
            boolean updated = false;
            boolean alreadyExists = false;

            // 요일 인덱스 찾기
            int dayIndex = getDayIndex(dayCode);

            if (dayIndex < lines.size()) {
                String line = lines.get(dayIndex).trim();
                String[] parts = line.split("\\s+");

                // 이미 일정이 있는지 확인
                if (parts.length == 3 && !parts[1].equals("0")) {
                    alreadyExists = true;
                }

                // 일정 업데이트
                lines.set(dayIndex, dayCode + " " + startTime + " " + endTime);
                updated = true;
            }

            if (updated) {
                Path masterFile = FileUtil.getResourcePath(masterFilePath);
                Files.write(masterFile, lines);

                if (alreadyExists) {
                    System.out.println("[경고] 진료 일정이 이미 존재합니다.");
                }

                System.out.println("진료 일정이 설정되었습니다.");
                System.out.println("- 요일: " + DAY_MAP_ENG_TO_KOR.get(dayCode) + " (" + dayCode + ")");
                System.out.println("- 시간: " + startTime + " ~ " + endTime);
                System.out.println("이 일정은 매주 적용됩니다.");
            }

        } catch (IOException e) {
            throw new DoctorScheduleException("진료 일정 설정 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 6.6.2 진료 일정 조회
     */
    public void viewSchedule(String[] args) throws DoctorScheduleException {
        if (!authContext.isLoggedIn() || !"DOCTOR".equals(authContext.getCurrentUser().getRole())) {
            throw new DoctorScheduleException("의사 계정만 사용할 수 있습니다.");
        }

        if (args.length != 0) {
            throw new DoctorScheduleException("인자가 없어야 합니다.");
        }

        try {
            User currentUser = authContext.getCurrentUser();
            String doctorId = currentUser.getId();
            String masterFilePath = "data/doctor/" + doctorId + "-master.txt";

            List<String> lines = FileUtil.readLines(masterFilePath);

            System.out.println("======================================================================================");
            System.out.println("주간 진료 일정");
            System.out.println("======================================================================================");

            for (String line : lines) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length == 3) {
                    String dayCode = parts[0];
                    String startTime = parts[1];
                    String endTime = parts[2];

                    String dayName = DAY_MAP_ENG_TO_KOR.get(dayCode);

                    if (startTime.equals("0") || endTime.equals("0")) {
                        System.out.println(dayName + " (" + dayCode + "): 설정된 일정 없음");
                    } else {
                        System.out.println(dayName + " (" + dayCode + "): " + startTime + "-" + endTime);
                    }
                }
            }
            System.out.println("======================================================================================");

        } catch (IOException e) {
            throw new DoctorScheduleException("진료 일정 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 6.6.3 진료 일정 수정
     */
    public void modifySchedule(String[] args) throws DoctorScheduleException {
        if (!authContext.isLoggedIn() || !"DOCTOR".equals(authContext.getCurrentUser().getRole())) {
            throw new DoctorScheduleException("의사 계정만 사용할 수 있습니다.");
        }

        if (args.length != 3) {
            throw new DoctorScheduleException("인자의 개수가 올바르지 않습니다. (형식: modify-schedule <요일> <시작시간 HH:MM> <종료시간 HH:MM>)");
        }

        String dayInput = args[0].toUpperCase();
        String startTime = args[1];
        String endTime = args[2];

        String dayCode = convertDayCode(dayInput);
        LocalTime start = validateTime(startTime);
        LocalTime end = validateTime(endTime);

        if (!start.isBefore(end)) {
            throw new DoctorScheduleException("시작시간은 종료시간보다 앞서야 합니다.");
        }

        try {
            User currentUser = authContext.getCurrentUser();
            String doctorId = currentUser.getId();
            String masterFilePath = "data/doctor/" + doctorId + "-master.txt";

            List<String> lines = FileUtil.readLines(masterFilePath);
            int dayIndex = getDayIndex(dayCode);

            if (dayIndex >= lines.size()) {
                throw new DoctorScheduleException("설정된 진료 일정이 없습니다.");
            }

            String[] parts = lines.get(dayIndex).trim().split("\\s+");
            if (parts.length != 3 || parts[1].equals("0")) {
                throw new DoctorScheduleException("설정된 진료 일정이 없습니다.");
            }

            String oldStart = parts[1];
            String oldEnd = parts[2];

            // 일정 변경
            lines.set(dayIndex, dayCode + " " + startTime + " " + endTime);
            Path masterFile = FileUtil.getResourcePath(masterFilePath);
            Files.write(masterFile, lines);

            System.out.println("진료 일정이 수정되었습니다.");
            System.out.println("- 기존: " + oldStart + " ~ " + oldEnd);
            System.out.println("- 변경: " + startTime + " ~ " + endTime);

        } catch (IOException e) {
            throw new DoctorScheduleException("진료 일정 수정 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 6.6.4 진료 일정 삭제
     */
    public void deleteSchedule(String[] args) throws DoctorScheduleException {
        if (!authContext.isLoggedIn() || !"DOCTOR".equals(authContext.getCurrentUser().getRole())) {
            throw new DoctorScheduleException("의사 계정만 사용할 수 있습니다.");
        }

        if (args.length != 1) {
            throw new DoctorScheduleException("인자의 개수가 올바르지 않습니다. (형식: delete-schedule <요일>)");
        }

        String dayInput = args[0].toUpperCase();
        String dayCode = convertDayCode(dayInput);

        try {
            User currentUser = authContext.getCurrentUser();
            String doctorId = currentUser.getId();
            String masterFilePath = "data/doctor/" + doctorId + "-master.txt";

            List<String> lines = FileUtil.readLines(masterFilePath);
            int dayIndex = getDayIndex(dayCode);

            if (dayIndex >= lines.size()) {
                throw new DoctorScheduleException("설정된 진료 일정이 없습니다.");
            }

            String[] parts = lines.get(dayIndex).trim().split("\\s+");
            if (parts.length != 3 || parts[1].equals("0")) {
                throw new DoctorScheduleException("설정된 진료 일정이 없습니다.");
            }

            // 확인 메시지 출력
            System.out.print(DAY_MAP_ENG_TO_KOR.get(dayCode) + "의 진료 일정을 삭제하시겠습니까? (Y/N): ");
            Scanner scanner = new Scanner(System.in);
            String confirm = scanner.nextLine().trim();

            if (!confirm.equalsIgnoreCase("Y")) {
                System.out.println("삭제가 취소되었습니다.");
                return;
            }

            // 일정 삭제 (0 0으로 설정)
            lines.set(dayIndex, dayCode + " 0 0");
            Path masterFile = FileUtil.getResourcePath(masterFilePath);
            Files.write(masterFile, lines);

            System.out.println(DAY_MAP_ENG_TO_KOR.get(dayCode) + "의 진료 일정이 삭제되었습니다.");

        } catch (IOException e) {
            throw new DoctorScheduleException("진료 일정 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // ========== 헬퍼 메서드 ==========

    private String convertDayCode(String dayInput) throws DoctorScheduleException {
        // 한글 변환
        if (DAY_MAP_KOR_TO_ENG.containsKey(dayInput)) {
            return DAY_MAP_KOR_TO_ENG.get(dayInput);
        }

        // 영문 대문자로 변환
        String dayCode = dayInput.toUpperCase();

        // 유효한 요일인지 확인
        if (!DAY_MAP_ENG_TO_KOR.containsKey(dayCode)) {
            throw new DoctorScheduleException("요일 형식이 잘못되었습니다. (사용 가능: MON, TUE, WED, THU, FRI, SAT, SUN 또는 월, 화, 수, 목, 금, 토, 일)");
        }

        return dayCode;
    }

    private LocalTime validateTime(String timeStr) throws DoctorScheduleException {
        if (!TIME_PATTERN.matcher(timeStr).matches()) {
            throw new DoctorScheduleException("시간 형식이 잘못되었습니다. (예: 09:00, 00:00~23:59)");
        }

        try {
            LocalTime time = LocalTime.parse(timeStr, TIME_FORMATTER);
            int minute = time.getMinute();
            if (minute % 10 != 0) {
                throw new DoctorScheduleException("시간은 10분 단위만 가능합니다. (예: 09:00, 09:10, 09:20)");
            }
            return time;
        } catch (DateTimeParseException e) {
            throw new DoctorScheduleException("시간 형식이 잘못되었습니다. (예: 09:00, 00:00~23:59)");
        }
    }

    private int getDayIndex(String dayCode) {
        return switch (dayCode) {
            case "MON" -> 0;
            case "TUE" -> 1;
            case "WED" -> 2;
            case "THU" -> 3;
            case "FRI" -> 4;
            case "SAT" -> 5;
            case "SUN" -> 6;
            default -> -1;
        };
    }
}