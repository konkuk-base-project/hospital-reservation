package service.reservation;

import model.User;
import repository.AppointmentRepository;
import repository.ReservationRepository;
import service.AuthContext;
import util.exception.ReservationException;
import util.file.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;
import util.file.VirtualTime;

public class ReservationService {
    private final AuthContext authContext;
    private final AppointmentRepository appointmentRepository;
    private final ReservationRepository reservationRepository;

    private static final Pattern RESERVATION_ID_PATTERN = Pattern.compile("^R\\d{8}$");
    private static final Pattern DOCTOR_ID_PATTERN = Pattern.compile("^D\\d{5}$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public ReservationService(AuthContext authContext) {
        this.authContext = authContext;
        this.appointmentRepository = new AppointmentRepository();
        this.reservationRepository = new ReservationRepository();
    }

    /**
     * 6.2.1 예약 생성
     */
    public void createReservation(String[] args) throws ReservationException {
        if (!authContext.isLoggedIn()) {
            throw new ReservationException("로그인이 필요합니다.");
        }

        if (args.length != 3) {
            throw new ReservationException("인자의 개수가 올바르지 않습니다 (형식: reserve <의사번호|의사이름> <날짜 YYYY-MM-DD> <시간 HH:MM>)");
        }

        String doctorIdOrName = args[0];
        String dateStr = args[1];
        String timeStr = args[2];

        // 의사 번호 확인
        String doctorId = resolveDoctorId(doctorIdOrName);

        // 날짜 검증
        LocalDate date = validateDate(dateStr);

        // 시간 검증
        LocalTime time = validateTime(timeStr);

        // 의사 근무 시간 확인
        validateDoctorWorkingHours(doctorId, date, time);

        // 예약 가능 여부 확인
        validateTimeSlotAvailable(doctorId, date, timeStr);

        try {
            // 예약번호 생성
            String reservationId = reservationRepository.getNextReservationId();

            // 의사 정보 가져오기
            String[] doctorInfo = getDoctorInfo(doctorId);
            String doctorName = doctorInfo[0];
            String deptCode = doctorInfo[1];

            // Appointment 파일에 예약 생성
            appointmentRepository.createAppointment(date, doctorId, timeStr, reservationId);

            // 환자 파일에 예약 추가
            addReservationToPatientFile(reservationId, date, timeStr, deptCode, doctorId);

            // 의사 파일에 예약 반영
            updateDoctorSchedule(doctorId, date, timeStr, reservationId);

            System.out.println("예약이 완료되었습니다. [예약번호: " + reservationId + "]");

        } catch (Exception e) {
            throw new ReservationException("예약 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 6.2.2 예약 조회
     */
    public void checkReservation(String[] args) throws ReservationException {
        if (!authContext.isLoggedIn()) {
            throw new ReservationException("로그인이 필요합니다.");
        }

        if (args.length != 1) {
            throw new ReservationException("인자의 개수가 올바르지 않습니다. (형식: check <예약번호>)");
        }

        String reservationId = args[0];

        // 예약번호 형식 검증
        if (!RESERVATION_ID_PATTERN.matcher(reservationId).matches()) {
            throw new ReservationException("예약번호 형식이 잘못되었습니다. (예: R00000001)");
        }

        try {
            User currentUser = authContext.getCurrentUser();
            String patientId = currentUser.getId();
            String patientFilePath = "data/patient/" + patientId + ".txt";

            List<String> lines = FileUtil.readLines(patientFilePath);

            // 예약 찾기
            String reservationLine = null;
            for (int i = 3; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.startsWith(reservationId)) {
                    reservationLine = line;
                    break;
                }
            }

            if (reservationLine == null) {
                throw new ReservationException("존재하지 않는 예약번호입니다.");
            }

            // 예약 정보 파싱
            String[] parts = reservationLine.split("\\s+");
            String resId = parts[0];
            String resDate = parts[1];
            String startTime = parts[2];
            String endTime = parts[3];
            String deptCode = parts[4];
            String doctorId = parts[5];
            String status = parts[6];

            // 의사 이름 가져오기
            String doctorName = getDoctorInfo(doctorId)[0];
            String deptName = getDepartmentName(deptCode);
            String statusText = getStatusString(status);

            // 출력
            System.out.println("======================================================================================");
            System.out.println("예약 상세 내역");
            System.out.println("======================================================================================");
            System.out.println("예약번호: " + resId);
            System.out.println("날짜: " + resDate);
            System.out.println("시간: " + startTime + "-" + endTime);
            System.out.println("진료과: " + deptName);
            System.out.println("의사: " + doctorName);
            System.out.println("상태: " + statusText);

        } catch (ReservationException e) {
            // ReservationException은 그대로 다시 던지기
            throw e;
        } catch (Exception e) {
            throw new ReservationException("예약 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 6.2.3 예약 수정
     */
    public void modifyReservation(String[] args) throws ReservationException {
        if (!authContext.isLoggedIn()) {
            throw new ReservationException("로그인이 필요합니다.");
        }

        if (args.length != 3) {
            throw new ReservationException("인자의 개수가 올바르지 않습니다. (형식: modify <예약번호> <새날짜 YYYY-MM-DD> <새시간 HH:MM>)");
        }

        String reservationId = args[0];
        String newDateStr = args[1];
        String newTimeStr = args[2];

        // 예약번호 형식 검증
        if (!RESERVATION_ID_PATTERN.matcher(reservationId).matches()) {
            throw new ReservationException("예약번호 형식이 잘못되었습니다. (예: R00000001)");
        }

        // 날짜 검증
        LocalDate newDate = validateDate(newDateStr);

        // 시간 검증
        LocalTime newTime = validateTime(newTimeStr);

        try {
            User currentUser = authContext.getCurrentUser();
            String patientId = currentUser.getId();
            String patientFilePath = "data/patient/" + patientId + ".txt";

            List<String> lines = FileUtil.readLines(patientFilePath);

            // 예약 찾기
            int reservationLineIndex = -1;
            String[] oldReservationParts = null;

            for (int i = 3; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.startsWith(reservationId)) {
                    reservationLineIndex = i;
                    oldReservationParts = line.split("\\s+");
                    break;
                }
            }

            if (reservationLineIndex == -1) {
                throw new ReservationException("존재하지 않는 예약번호입니다.");
            }

            // 상태 확인 (1: 예약완료만 수정 가능)
            String status = oldReservationParts[6];
            if (!"1".equals(status)) {
                throw new ReservationException("예약완료 상태인 경우에만 수정 가능합니다.");
            }

            String oldDateStr = oldReservationParts[1];
            String oldTimeStr = oldReservationParts[2];
            String doctorId = oldReservationParts[5];

            // 의사 근무 시간 확인
            validateDoctorWorkingHours(doctorId, newDate, newTime);

            // 새 시간대 예약 가능 여부 확인
            validateTimeSlotAvailable(doctorId, newDate, newTimeStr);

            // 기존 예약 취소
            LocalDate oldDate = LocalDate.parse(oldDateStr, DATE_FORMATTER);
            appointmentRepository.cancelAppointment(oldDate, reservationId);
            updateDoctorSchedule(doctorId, oldDate, oldTimeStr, "0");

            // 새 예약 생성
            appointmentRepository.createAppointment(newDate, doctorId, newTimeStr, reservationId);
            updateDoctorSchedule(doctorId, newDate, newTimeStr, reservationId);

            // 환자 파일 업데이트
            String endTime = calculateEndTime(newTimeStr);
            String newReservationLine = String.join(" ",
                    reservationId, newDateStr, newTimeStr, endTime,
                    oldReservationParts[4], doctorId, "1"
            );
            lines.set(reservationLineIndex, newReservationLine);

            Path patientFile = FileUtil.getResourcePath(patientFilePath);
            Files.write(patientFile, lines);

            System.out.println("예약이 변경되었습니다. [예약번호: " + reservationId + "]");

        } catch (ReservationException e) {
            // ReservationException은 그대로 다시 던지기
            throw e;
        } catch (Exception e) {
            throw new ReservationException("예약 수정 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 6.2.4 예약 취소
     */
    public void cancelReservation(String[] args) throws ReservationException {
        if (!authContext.isLoggedIn()) {
            throw new ReservationException("로그인이 필요합니다.");
        }

        if (args.length != 1) {
            throw new ReservationException("인자의 개수가 올바르지 않습니다. (형식: cancel <예약번호>)");
        }

        String reservationId = args[0];

        // 예약번호 형식 검증
        if (!RESERVATION_ID_PATTERN.matcher(reservationId).matches()) {
            throw new ReservationException("예약번호 형식이 잘못되었습니다. (예: R00000001)");
        }

        try {
            User currentUser = authContext.getCurrentUser();
            String patientId = currentUser.getId();
            String patientFilePath = "data/patient/" + patientId + ".txt";

            List<String> lines = FileUtil.readLines(patientFilePath);

            // 예약 찾기
            int reservationLineIndex = -1;
            String[] reservationParts = null;

            for (int i = 3; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.startsWith(reservationId)) {
                    reservationLineIndex = i;
                    reservationParts = line.split("\\s+");
                    break;
                }
            }

            if (reservationLineIndex == -1) {
                throw new ReservationException("존재하지 않는 예약번호입니다.");
            }

            // 상태 확인 (1: 예약완료만 취소 가능)
            String status = reservationParts[6];
            if (!"1".equals(status)) {
                throw new ReservationException("예약완료 상태인 경우에만 취소가 가능합니다.");
            }

            String dateStr = reservationParts[1];
            String timeStr = reservationParts[2];
            String doctorId = reservationParts[5];

            // Appointment 파일에서 취소
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            appointmentRepository.cancelAppointment(date, reservationId);

            // 의사 파일에서 취소
            updateDoctorSchedule(doctorId, date, timeStr, "0");

            // 환자 파일에서 상태 변경 (1 -> 3: 취소)
            reservationParts[6] = "3";
            String updatedLine = String.join(" ", reservationParts);
            lines.set(reservationLineIndex, updatedLine);

            Path patientFile = FileUtil.getResourcePath(patientFilePath);
            Files.write(patientFile, lines);

            System.out.println("예약이 취소되었습니다. [예약번호: " + reservationId + "]");

        } catch (ReservationException e) {
            // ReservationException은 그대로 다시 던지기
            throw e;
        } catch (Exception e) {
            throw new ReservationException("예약 취소 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 6.2.5 진료과 예약 생성
     */
    public void reserveMajor(String[] args) throws ReservationException {
        if (!authContext.isLoggedIn()) {
            throw new ReservationException("로그인이 필요합니다.");
        }

        if (args.length != 3) {
            throw new ReservationException("인자의 개수가 올바르지 않습니다 (형식: reserve-major <진료과코드> <날짜 YYYY-MM-DD> <시간 HH:MM>)");
        }

        String deptCode = args[0];
        String dateStr = args[1];
        String timeStr = args[2];

        LocalDate date = validateDate(dateStr);
        LocalTime time = validateTime(timeStr);

        // 진료과 소속 의사 목록에서 예약 가능한 의사 찾기
        String selectedDoctorId = null;
        try {
            List<String> doctorLines = FileUtil.readLines("data/doctor/doctorlist.txt");
            for (int i = 1; i < doctorLines.size(); i++) {
                String line = doctorLines.get(i).trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+");
                if (parts.length < 3) continue;
                String doctorId = parts[0];
                String doctorDept = parts[2];
                if (!deptCode.equals(doctorDept)) continue;

                try {
                    // 근무 시간 및 요일 확인
                    validateDoctorWorkingHours(doctorId, date, time);
                    // 해당 시간대 예약 가능 여부 확인 (파일 기반/스케줄 기반 모두 검사)
                    validateTimeSlotAvailable(doctorId, date, timeStr);
                    // 성공하면 해당 의사 선택
                    selectedDoctorId = doctorId;
                    break;
                } catch (ReservationException ignore) {
                    // 이 의사는 불가능하면 다음 의사 검사
                }
            }
        } catch (IOException e) {
            throw new ReservationException("의사 목록을 불러오는 중 오류가 발생했습니다.");
        }

        if (selectedDoctorId == null) {
            throw new ReservationException("해당 진료과에서 예약 가능한 의사가 없습니다.");
        }

        try {
            // 예약번호 생성
            String reservationId = reservationRepository.getNextReservationId();

            // 의사 정보
            String[] doctorInfo = getDoctorInfo(selectedDoctorId);
            String doctorName = doctorInfo[0];
            String dept = doctorInfo[1];

            // Appointment 파일에 예약 생성
            appointmentRepository.createAppointment(date, selectedDoctorId, timeStr, reservationId);

            // 환자 파일에 예약 추가
            addReservationToPatientFile(reservationId, date, timeStr, dept, selectedDoctorId);

            // 의사 파일에 예약 반영
            updateDoctorSchedule(selectedDoctorId, date, timeStr, reservationId);

            System.out.println("예약이 완료되었습니다. [예약번호: " + reservationId + ", 의사: " + doctorName + "]");

        } catch (Exception e) {
            throw new ReservationException("예약 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // ========== 헬퍼 메서드 ==========

    private String resolveDoctorId(String doctorIdOrName) throws ReservationException {
        if (DOCTOR_ID_PATTERN.matcher(doctorIdOrName).matches()) {
            if (!FileUtil.resourceExists("data/doctor/" + doctorIdOrName + ".txt")) {
                throw new ReservationException("존재하지 않는 의사번호입니다.");
            }
            return doctorIdOrName;
        }

        // 의사 이름으로 검색
        try {
            List<String> lines = FileUtil.readLines("data/doctor/doctorlist.txt");
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length >= 2 && parts[1].equals(doctorIdOrName)) {
                    return parts[0];
                }
            }
        } catch (IOException e) {
            throw new ReservationException("의사 정보를 조회하는 중 오류가 발생했습니다.");
        }

        throw new ReservationException("존재하지 않는 의사입니다.");
    }

    private LocalDate validateDate(String dateStr) throws ReservationException {
        try {
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            if (date.isBefore(VirtualTime.currentDate().plusDays(1))) {
                throw new ReservationException("과거 날짜로는 예약할 수 없습니다.");
            }
            return date;
        } catch (DateTimeParseException e) {
            throw new ReservationException("날짜 형식이 잘못되었습니다. (예: 2025-10-10)");
        }
    }

    private LocalTime validateTime(String timeStr) throws ReservationException {
        try {
            LocalTime time = LocalTime.parse(timeStr, TIME_FORMATTER);
            int minute = time.getMinute();
            if (minute % 10 != 0) {
                throw new ReservationException("예약은 10분 단위 시간만 가능합니다. (예: 10:20, 10:30)");
            }
            return time;
        } catch (DateTimeParseException e) {
            throw new ReservationException("시간 형식이 잘못되었습니다. (예: 10:30)");
        }
    }

    private void validateDoctorWorkingHours(String doctorId, LocalDate date, LocalTime time) throws ReservationException {
        try {
            String doctorFilePath = "data/doctor/" + doctorId + ".txt";
            List<String> lines = FileUtil.readLines(doctorFilePath);

            // 근무 시간 확인 (09:00 ~ 17:50)
            if (time.isBefore(LocalTime.of(9, 0)) || time.isAfter(LocalTime.of(17, 50))) {
                throw new ReservationException("해당 시간은 의사의 근무 시간이 아닙니다.");
            }

            // 요일별 근무 확인 (월~금: 0~4)
            int dayOfWeek = date.getDayOfWeek().getValue() - 1; // 0: 월요일
            if (dayOfWeek >= 5) {
                throw new ReservationException("주말에는 진료가 불가능합니다.");
            }

            String[] weekdaySchedule = lines.get(1).split("\\s+");
            if (!"1".equals(weekdaySchedule[dayOfWeek])) {
                throw new ReservationException("해당 요일에는 의사가 진료하지 않습니다.");
            }

        } catch (IOException e) {
            throw new ReservationException("의사 정보를 조회하는 중 오류가 발생했습니다.");
        }
    }

    private void validateTimeSlotAvailable(String doctorId, LocalDate date, String timeStr) throws ReservationException {
        try {
            List<String> availableSlots = appointmentRepository.getAvailableTimeSlots(date, doctorId);
            if (!availableSlots.contains(timeStr)) {
                throw new ReservationException("이미 예약된 시간대입니다.");
            }
        } catch (util.exception.AppointmentFileException e) {
            // 예약 파일이 없는 경우 - 의사 스케줄에서 확인
            if (e.getErrorType() == util.exception.AppointmentFileException.ErrorType.FILE_NOT_FOUND) {
                try {
                    if (!checkDoctorSchedule(doctorId, date, timeStr)) {
                        throw new ReservationException("해당 시간은 예약할 수 없습니다.");
                    }
                } catch (IOException ioException) {
                    throw new ReservationException("의사 스케줄을 확인하는 중 오류가 발생했습니다.");
                }
            } else {
                throw new ReservationException("예약 가능 시간을 확인하는 중 오류가 발생했습니다: " + e.getMessage());
            }
        } catch (ReservationException e) {
            // ReservationException은 그대로 다시 던지기 (메시지 중복 방지)
            throw e;
        } catch (Exception e) {
            // 그 외 예외만 감싸서 던지기
            throw new ReservationException("예약 가능 시간을 확인하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 의사 스케줄 파일에서 예약 가능 여부 확인
     */
    private boolean checkDoctorSchedule(String doctorId, LocalDate date, String timeStr) throws IOException {
        String doctorFilePath = "data/doctor/" + doctorId + ".txt";
        List<String> lines = FileUtil.readLines(doctorFilePath);

        String dateStr = date.format(DATE_FORMATTER);
        int slotIndex = getSlotIndex(timeStr);

        // 의사 파일에서 해당 날짜 찾기
        for (int i = 3; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;

            if (line.startsWith(dateStr)) {
                String[] parts = line.split("\\s+");
                // 슬롯 인덱스가 범위 내인지 확인 (날짜 + 54개 슬롯)
                if (slotIndex + 1 < parts.length) {
                    // "0"이면 예약 가능, 아니면 예약 불가
                    return "0".equals(parts[slotIndex + 1]);
                }
            }
        }

        // 해당 날짜가 의사 파일에 없으면 예약 가능으로 판단
        // (updateDoctorSchedule()에서 새 날짜 줄이 자동으로 생성됨)
        return true;
    }

    private String[] getDoctorInfo(String doctorId) throws IOException {
        List<String> lines = FileUtil.readLines("data/doctor/" + doctorId + ".txt");
        String[] parts = lines.get(0).split("\\s+");
        return new String[]{parts[1], parts[2]}; // [이름, 진료과코드]
    }

    private void addReservationToPatientFile(String reservationId, LocalDate date, String startTime, String deptCode, String doctorId) throws IOException {
        User currentUser = authContext.getCurrentUser();
        String patientId = currentUser.getId();
        String patientFilePath = "data/patient/" + patientId + ".txt";

        String endTime = calculateEndTime(startTime);
        String dateStr = date.format(DATE_FORMATTER);
        String reservationLine = String.join(" ", reservationId, dateStr, startTime, endTime, deptCode, doctorId, "1");

        FileUtil.appendLine(patientFilePath, reservationLine);
    }

    private void updateDoctorSchedule(String doctorId, LocalDate date, String timeStr, String value) throws IOException {
        String doctorFilePath = "data/doctor/" + doctorId + ".txt";
        List<String> lines = FileUtil.readLines(doctorFilePath);

        String dateStr = date.format(DATE_FORMATTER);
        int slotIndex = getSlotIndex(timeStr);

        boolean found = false;
        for (int i = 3; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.startsWith(dateStr)) {
                String[] parts = line.split("\\s+");
                parts[slotIndex + 1] = value; // +1은 날짜 칼럼 때문
                lines.set(i, String.join(" ", parts));
                found = true;
                break;
            }
        }

        if (!found) {
            // 해당 날짜 스케줄이 없으면 새로 생성
            String[] slots = new String[55];
            slots[0] = dateStr;
            for (int i = 1; i <= 54; i++) {
                slots[i] = "0";
            }
            slots[slotIndex + 1] = value;
            lines.add(String.join(" ", slots));
        }

        Path doctorFile = FileUtil.getResourcePath(doctorFilePath);
        Files.write(doctorFile, lines);
    }

    private String calculateEndTime(String startTime) {
        LocalTime start = LocalTime.parse(startTime, TIME_FORMATTER);
        LocalTime end = start.plusMinutes(10);
        return end.format(TIME_FORMATTER);
    }

    private int getSlotIndex(String timeStr) {
        LocalTime time = LocalTime.parse(timeStr, TIME_FORMATTER);
        int hour = time.getHour();
        int minute = time.getMinute();
        return (hour - 9) * 6 + (minute / 10);
    }

    private String getDepartmentName(String code) {
        switch (code) {
            case "IM": return "내과";
            case "GS": return "일반외과";
            case "OB": return "산부인과";
            case "PED": return "소아과";
            case "PSY": return "정신과";
            case "DERM": return "피부과";
            case "ENT": return "이비인후과";
            case "ORTH": return "정형외과";
            default: return code;
        }
    }

    private String getStatusString(String code) {
        switch (code) {
            case "1": return "예약완료";
            case "2": return "진료완료";
            case "3": return "취소";
            case "4": return "미방문";
            default: return "알 수 없음";
        }
    }
}