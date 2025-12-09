package service.doctor;

import model.User;
import service.AuthContext;
import service.doctor.helper.PatientFileReader;
import util.exception.DoctorScheduleException;
import util.file.FileUtil;
import repository.AppointmentRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

public class DoctorService {
    private final AuthContext authContext;
    private final AppointmentRepository appointmentRepository;
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

    public DoctorService(AuthContext authContext, AppointmentRepository appointmentRepository) {
        this.authContext = authContext;
        this.appointmentRepository = appointmentRepository;
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

            // 요일 인덱스 찾기
            int dayIndex = getDayIndex(dayCode);

            if (dayIndex < lines.size()) {
                String line = lines.get(dayIndex).trim();
                String[] parts = line.split("\\s+");

                // 이미 일정이 있는지 확인
                if (parts.length == 3 && !parts[1].equals("0")) {
                    throw new DoctorScheduleException("진료 일정이 이미 존재합니다.");
                }

                // 일정 설정
                lines.set(dayIndex, dayCode + " " + startTime + " " + endTime);
                updated = true;
            }

            if (updated) {
                Path masterFile = FileUtil.getResourcePath(masterFilePath);
                Files.write(masterFile, lines);

                // {doctorId}.txt 파일의 요일별 근무 여부도 업데이트
                String doctorFilePath = "data/doctor/" + doctorId + ".txt";
                List<String> doctorLines = FileUtil.readLines(doctorFilePath);

                if (doctorLines.size() >= 2) {
                    String[] weekdaySchedule = doctorLines.get(1).split("\\s+");
                    if (weekdaySchedule.length == 5 && dayIndex >= 0 && dayIndex < 5) {
                        weekdaySchedule[dayIndex] = "1";  // 해당 요일 근무 가능으로 설정
                        doctorLines.set(1, String.join(" ", weekdaySchedule));

                        Path doctorFile = FileUtil.getResourcePath(doctorFilePath);
                        Files.write(doctorFile, doctorLines);
                    }
                }

                System.out.println("진료 일정이 설정되었습니다.");
                System.out.println("- 요일: " + DAY_MAP_ENG_TO_KOR.get(dayCode) + " (" + dayCode + ")");
                System.out.println("- 시간: " + startTime + " ~ " + endTime);
                System.out.println("이 일정은 매주 " + DAY_MAP_ENG_TO_KOR.get(dayCode) + "에 적용됩니다.");
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

            LocalDate currentDate = util.file.VirtualTime.currentDate();
            List<PatientFileReader.ReservationData> affectedReservations =
                    PatientFileReader.findFutureReservationsByDoctorAndTimeRange(
                            doctorId, dayCode, currentDate, startTime, endTime);

            if (!affectedReservations.isEmpty()) {
                System.out.println("[경고] 다음 미래 예약이 새 일정 범위를 벗어나므로 자동 취소됩니다:");
                for (PatientFileReader.ReservationData res : affectedReservations) {
                    System.out.printf("- %s (%s) | %s | %s-%s | %s (%s)%n",
                            res.date, getDayOfWeekInKorean(res.date),
                            res.reservationId, res.startTime, res.endTime,
                            res.patientName, res.patientId);
                }

                System.out.print("\n이 예약들을 취소하고 일정을 수정하시겠습니까? (Y/N): ");
                Scanner scanner = new Scanner(System.in);
                String confirm = scanner.nextLine().trim();

                if (!confirm.equalsIgnoreCase("Y")) {
                    System.out.println("일정 수정이 취소되었습니다.");
                    return;
                }

                for (PatientFileReader.ReservationData res : affectedReservations) {
                    cancelReservation(res.patientId, res.reservationId);
                }

                System.out.printf("\n%d건의 미래 예약이 자동 취소되었습니다.%n",
                        affectedReservations.size());
            }

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

            LocalDate currentDate = util.file.VirtualTime.currentDate();
            List<PatientFileReader.ReservationData> futureReservations =
                    PatientFileReader.findFutureReservationsByDoctorAndDay(
                            doctorId, dayCode, currentDate);

            if (!futureReservations.isEmpty()) {
                System.out.println("[경고] 다음 미래 예약이 자동으로 취소됩니다:");
                for (PatientFileReader.ReservationData res : futureReservations) {
                    System.out.printf("- %s (%s) | %s | %s-%s | %s (%s)%n",
                            res.date, getDayOfWeekInKorean(res.date),
                            res.reservationId, res.startTime, res.endTime,
                            res.patientName, res.patientId);
                }
                System.out.println();
            }

            // 확인 메시지 출력
            System.out.print(DAY_MAP_ENG_TO_KOR.get(dayCode) +
                    "의 진료 일정을 삭제하시겠습니까? (Y/N): ");
            Scanner scanner = new Scanner(System.in);
            String confirm = scanner.nextLine().trim();

            if (!confirm.equalsIgnoreCase("Y")) {
                System.out.println("삭제가 취소되었습니다.");
                return;
            }

            // 예약 취소 처리
            if (!futureReservations.isEmpty()) {
                for (PatientFileReader.ReservationData res : futureReservations) {
                    cancelReservation(res.patientId, res.reservationId);
                }
                System.out.printf("\n%d건의 미래 예약이 자동 취소되었습니다.%n",
                        futureReservations.size());
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

    /**
     * 6.6.5 예약 상태 처리 (진료 완료)
     */
    public void completeAppointment(String[] args) throws DoctorScheduleException {
        if (!authContext.isLoggedIn() || !"DOCTOR".equals(authContext.getCurrentUser().getRole())) {
            throw new DoctorScheduleException("의사 계정만 사용할 수 있습니다.");
        }

        if (args.length != 1) {
            throw new DoctorScheduleException("인자의 개수가 올바르지 않습니다. (형식: complete <예약번호>)");
        }

        String reservationId = args[0];

        // 예약번호 형식 검증
        if (!reservationId.matches("^R\\d{8}$")) {
            throw new DoctorScheduleException("예약번호 형식이 잘못되었습니다. (예: R00000001)");
        }

        try {
            User currentUser = authContext.getCurrentUser();
            String doctorId = currentUser.getId();

            PatientFileReader.ReservationData resInfo =
                    PatientFileReader.findReservationById(reservationId);

            if (resInfo == null) {
                throw new DoctorScheduleException("존재하지 않는 예약번호입니다.");
            }

            // 본인의 예약인지 확인
            if (!resInfo.doctorId.equals(doctorId)) {
                throw new DoctorScheduleException("본인의 예약만 처리할 수 있습니다. (담당 의사: " + getDoctorNameById(resInfo.doctorId) + ")");
            }

            // 예약 상태 확인 (1: 예약완료만 처리 가능)
            if (!resInfo.status.equals("1")) {
                String statusText = getStatusText(resInfo.status);
                throw new DoctorScheduleException("이미 처리된 예약입니다. (현재 상태: " + statusText + ")");
            }

            // 예약 시간 경과 확인 - 수정된 부분
            LocalDateTime now = util.file.VirtualTime.currentDateTime();
            LocalDate resDate = LocalDate.parse(resInfo.date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalTime resTime = LocalTime.parse(resInfo.startTime, DateTimeFormatter.ofPattern("HH:mm"));
            LocalDateTime reservationDateTime = LocalDateTime.of(resDate, resTime);

            // 예약 시간이 아직 경과하지 않은 경우
            if (reservationDateTime.isAfter(now)) {
                throw new DoctorScheduleException(String.format(
                        "예약 시간이 아직 경과하지 않았습니다. (예약 시간: %s %s, 현재: %s)",
                        resInfo.date, resInfo.startTime,
                        now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                ));
            }

            // 환자 파일 업데이트 (상태 1 -> 2)
            updatePatientReservationStatus(resInfo.patientId, reservationId, "2");

            // Appointment 파일도 업데이트
            try {
                appointmentRepository.updateAppointmentStatus(resDate, reservationId, "2");
            } catch (util.exception.AppointmentFileException e) {
                throw new DoctorScheduleException("예약 상태 업데이트 중 오류가 발생했습니다: " + e.getMessage());
            }

            System.out.println("예약이 진료완료 처리되었습니다.");
            System.out.println("- 예약번호: " + reservationId);
            System.out.println("- 환자: " + resInfo.patientName + " (" + resInfo.patientId + ")");
            System.out.println("- 진료: " + resInfo.date + " " + resInfo.startTime + "-" + resInfo.endTime + " | " + getDeptName(resInfo.deptCode) + " | " + getDoctorNameById(doctorId));

        } catch (IOException e) {
            throw new DoctorScheduleException("진료 완료 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 6.6.6 예약 상태 처리 (노쇼)
     */
    public void noshowAppointment(String[] args) throws DoctorScheduleException {
        if (!authContext.isLoggedIn() || !"DOCTOR".equals(authContext.getCurrentUser().getRole())) {
            throw new DoctorScheduleException("의사 계정만 사용할 수 있습니다.");
        }

        if (args.length != 1) {
            throw new DoctorScheduleException("인자의 개수가 올바르지 않습니다. (형식: noshow <예약번호>)");
        }

        String reservationId = args[0];

        // 예약번호 형식 검증
        if (!reservationId.matches("^R\\d{8}$")) {
            throw new DoctorScheduleException("예약번호 형식이 잘못되었습니다. (예: R00000001)");
        }

        try {
            User currentUser = authContext.getCurrentUser();
            String doctorId = currentUser.getId();

            PatientFileReader.ReservationData resInfo =
                    PatientFileReader.findReservationById(reservationId);

            if (resInfo == null) {
                throw new DoctorScheduleException("존재하지 않는 예약번호입니다.");
            }

            // 본인의 예약인지 확인
            if (!resInfo.doctorId.equals(doctorId)) {
                throw new DoctorScheduleException("본인의 예약만 처리할 수 있습니다. (담당 의사: " + getDoctorNameById(resInfo.doctorId) + ")");
            }

            // 예약 상태 확인 (1: 예약완료만 처리 가능)
            if (!resInfo.status.equals("1")) {
                String statusText = getStatusText(resInfo.status);
                throw new DoctorScheduleException("이미 처리된 예약입니다. (현재 상태: " + statusText + ")");
            }

            // 예약 시간 경과 확인 - 수정된 부분
            LocalDateTime now = util.file.VirtualTime.currentDateTime();
            LocalDate resDate = LocalDate.parse(resInfo.date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalTime resTime = LocalTime.parse(resInfo.startTime, DateTimeFormatter.ofPattern("HH:mm"));
            LocalDateTime reservationDateTime = LocalDateTime.of(resDate, resTime);

            // 예약 시간이 아직 경과하지 않은 경우
            if (reservationDateTime.isAfter(now)) {
                throw new DoctorScheduleException(String.format(
                        "예약 시간이 아직 경과하지 않았습니다. (예약 시간: %s %s, 현재: %s)",
                        resInfo.date, resInfo.startTime,
                        now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                ));
            }

            // 환자 파일 업데이트 (상태 1 -> 4)
            updatePatientReservationStatus(resInfo.patientId, reservationId, "4");

            // Appointment 파일도 업데이트
            try {
                appointmentRepository.updateAppointmentStatus(resDate, reservationId, "4");
            } catch (util.exception.AppointmentFileException e) {
                throw new DoctorScheduleException("예약 상태 업데이트 중 오류가 발생했습니다: " + e.getMessage());
            }

            // 노쇼 횟수 증가
            int noshowCount = incrementNoshowCount(resInfo.patientId);

            System.out.println("예약이 노쇼 처리되었습니다.");
            System.out.println("- 예약번호: " + reservationId);
            System.out.println("- 환자: " + resInfo.patientName + " (" + resInfo.patientId + ")");
            System.out.println("- 진료: " + resInfo.date + " " + resInfo.startTime + "-" + resInfo.endTime + " | " + getDeptName(resInfo.deptCode) + " | " + getDoctorNameById(doctorId));
            System.out.println("- 노쇼 누적: " + noshowCount + "회");

            if (noshowCount >= 3) {
                System.out.println("[경고] 해당 환자는 노쇼 3회로 예약 제한 대상입니다.");
            }

        } catch (IOException e) {
            throw new DoctorScheduleException("노쇼 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 6.6.7 처리 가능한 예약 목록 조회
     */
    public void showPendingAppointments(String[] args) throws DoctorScheduleException {
        if (!authContext.isLoggedIn() || !"DOCTOR".equals(authContext.getCurrentUser().getRole())) {
            throw new DoctorScheduleException("의사 계정만 사용할 수 있습니다.");
        }

        if (args.length != 0) {
            throw new DoctorScheduleException("인자가 없어야 합니다.");
        }

        try {
            User currentUser = authContext.getCurrentUser();
            String doctorId = currentUser.getId();
            LocalDate currentDate = util.file.VirtualTime.currentDate();

            List<PatientFileReader.ReservationData> pendingReservations =
                    PatientFileReader.findPendingReservationsByDoctor(doctorId, currentDate);

            // 데이터가 없는 경우 한 줄만 출력
            if (pendingReservations.isEmpty()) {
                System.out.println("처리 대기 중인 예약이 없습니다.");
                return;
            }

            // 데이터가 있는 경우 기존 형식으로 출력
            System.out.println("======================================================================================");
            System.out.println("처리 대기 중인 예약 (총 " + pendingReservations.size() + "건)");
            System.out.println("======================================================================================");

            // 날짜 순으로 정렬
            pendingReservations.sort((a, b) -> {
                int dateCompare = a.date.compareTo(b.date);
                if (dateCompare != 0) return dateCompare;
                return a.startTime.compareTo(b.startTime);
            });

            for (PatientFileReader.ReservationData info : pendingReservations) {
                System.out.printf("%s | %s %s-%s | %s | %s (%s)%n",
                        info.reservationId,
                        info.date,
                        info.startTime,
                        info.endTime,
                        getDeptName(info.deptCode),
                        info.patientName,
                        info.patientId
                );
            }
            System.out.println("======================================================================================");

        } catch (IOException e) {
            throw new DoctorScheduleException("처리 대기 예약 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // ========== 파일 업데이트 헬퍼 메서드 ==========

    /**
     * 환자 파일의 예약 상태 업데이트
     */
    private void updatePatientReservationStatus(String patientId, String reservationId, String newStatus) throws IOException {
        String patientFilePath = "data/patient/" + patientId + ".txt";
        List<String> lines = FileUtil.readLines(patientFilePath);

        for (int i = 3; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            if (parts.length >= 7 && parts[0].equals(reservationId)) {
                parts[6] = newStatus;
                lines.set(i, String.join(" ", parts));
                break;
            }
        }

        Path patientFile = FileUtil.getResourcePath(patientFilePath);
        Files.write(patientFile, lines);
    }

    /**
     * 예약 취소 처리
     */
    private void cancelReservation(String patientId, String reservationId)
            throws IOException {
        updatePatientReservationStatus(patientId, reservationId, "3");
    }

    /**
     * 노쇼 횟수 증가
     */
    private int incrementNoshowCount(String patientId) throws IOException {
        String patientFilePath = "data/patient/" + patientId + ".txt";
        List<String> lines = FileUtil.readLines(patientFilePath);

        // 1행: 환자 기본 정보에서 노쇼 횟수 증가
        String[] parts = lines.get(0).split("\\s+");
        int noshowCount = 0;

        if (parts.length >= 5) {
            // 기존에 노쇼 횟수가 있으면 파싱
            try {
                noshowCount = Integer.parseInt(parts[4]);
            } catch (NumberFormatException e) {
                noshowCount = 0;
            }
        }

        noshowCount++;

        // 노쇼 횟수가 4개 요소인 경우 추가, 5개인 경우 수정
        if (parts.length == 4) {
            lines.set(0, String.join(" ", parts[0], parts[1], parts[2], parts[3], String.valueOf(noshowCount)));
        } else {
            parts[4] = String.valueOf(noshowCount);
            lines.set(0, String.join(" ", parts));
        }

        Path patientFile = FileUtil.getResourcePath(patientFilePath);
        Files.write(patientFile, lines);

        // patientlist.txt도 업데이트
        updatePatientListNoshowCount(patientId, noshowCount);

        return noshowCount;
    }

    /**
     * patientlist.txt의 노쇼 횟수 업데이트
     */
    private void updatePatientListNoshowCount(String patientId, int noshowCount) throws IOException {
        String patientListPath = "data/patient/patientlist.txt";
        List<String> lines = FileUtil.readLines(patientListPath);

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            if (parts.length >= 5 && parts[0].equals(patientId)) {
                if (parts.length == 5) {
                    lines.set(i, String.join(" ", parts[0], parts[1], parts[2], parts[3], parts[4], String.valueOf(noshowCount)));
                } else {
                    parts[5] = String.valueOf(noshowCount);
                    lines.set(i, String.join(" ", parts));
                }
                break;
            }
        }

        Path patientListFile = FileUtil.getResourcePath(patientListPath);
        Files.write(patientListFile, lines);
    }

    // ========== 기타 헬퍼 메서드 ==========

    /**
     * 의사 이름 조회
     */
    private String getDoctorNameById(String doctorId) {
        try {
            List<String> lines = FileUtil.readLines("data/doctor/" + doctorId + ".txt");
            if (!lines.isEmpty()) {
                String[] parts = lines.get(0).split("\\s+");
                return parts[1];
            }
        } catch (IOException e) {
            // 오류 시 doctorId 반환
        }
        return doctorId;
    }

    /**
     * 진료과 이름 반환
     */
    private String getDeptName(String code) {
        return switch (code) {
            case "IM" -> "내과";
            case "GS" -> "일반외과";
            case "OB" -> "산부인과";
            case "PED" -> "소아과";
            case "PSY" -> "정신과";
            case "DERM" -> "피부과";
            case "ENT" -> "이비인후과";
            case "ORTH" -> "정형외과";
            default -> code;
        };
    }

    /**
     * 예약 상태 텍스트 반환
     */
    private String getStatusText(String code) {
        return switch (code) {
            case "1" -> "예약완료";
            case "2" -> "진료완료";
            case "3" -> "취소";
            case "4" -> "노쇼";
            default -> "알 수 없음";
        };
    }

    /**
     * 날짜의 요일을 한글로 반환
     */
    private String getDayOfWeekInKorean(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr,
                DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
    }
}