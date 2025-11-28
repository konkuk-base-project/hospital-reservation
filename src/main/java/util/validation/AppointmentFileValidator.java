package util.validation;

import util.exception.AppointmentFileException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class AppointmentFileValidator {

    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern DOCTOR_ID_PATTERN = Pattern.compile("D\\d{5}");
    private static final Pattern TIME_PATTERN = Pattern.compile("\\d{2}:\\d{2}");
    private static final Pattern RESERVATION_ID_PATTERN = Pattern.compile("R\\d{8}\\([1-4]\\)");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // 진료 시간 범위: 09:00 ~ 17:50
    private static final LocalTime START_TIME = LocalTime.of(9, 0);
    private static final LocalTime END_TIME = LocalTime.of(17, 50);
    private static final int TIME_INTERVAL_MINUTES = 10;

    /**
     * 날짜 형식을 검증합니다 (YYYY-MM-DD)
     *
     * @param dateStr 검증할 날짜 문자열
     * @param lineNumber 라인 번호
     * @throws AppointmentFileException 형식이 올바르지 않은 경우
     */
    public static void validateDateFormat(String dateStr, int lineNumber) throws AppointmentFileException {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new AppointmentFileException(
                AppointmentFileException.ErrorType.INVALID_DATE_FORMAT,
                "날짜가 비어있습니다",
                lineNumber
            );
        }

        String trimmedDate = dateStr.trim();

        if (!DATE_PATTERN.matcher(trimmedDate).matches()) {
            throw new AppointmentFileException(
                AppointmentFileException.ErrorType.INVALID_DATE_FORMAT,
                "입력값: " + trimmedDate,
                lineNumber
            );
        }

        try {
            LocalDate.parse(trimmedDate, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new AppointmentFileException(
                AppointmentFileException.ErrorType.INVALID_DATE_FORMAT,
                "유효하지 않은 날짜: " + trimmedDate,
                lineNumber
            );
        }
    }

    /**
     * 의사 번호 리스트를 검증합니다
     *
     * @param doctorListLine 의사 번호 리스트 라인 (공백으로 구분)
     * @param lineNumber 라인 번호
     * @return 검증된 의사 번호 배열
     * @throws AppointmentFileException 형식이 올바르지 않은 경우
     */
    public static String[] validateDoctorList(String doctorListLine, int lineNumber) throws AppointmentFileException {
        if (doctorListLine == null || doctorListLine.trim().isEmpty()) {
            throw new AppointmentFileException(
                AppointmentFileException.ErrorType.INVALID_DOCTOR_LIST,
                "의사 번호 리스트가 비어있습니다",
                lineNumber
            );
        }

        String[] parts = doctorListLine.trim().split("\\s+");

        if (parts.length < 2) {
            throw new AppointmentFileException(
                AppointmentFileException.ErrorType.INVALID_DOCTOR_LIST,
                "최소 2개 이상의 항목이 필요합니다 (TIME + 의사번호들)",
                lineNumber
            );
        }

        if (!"TIME".equals(parts[0])) {
            throw new AppointmentFileException(
                AppointmentFileException.ErrorType.INVALID_DOCTOR_LIST,
                "첫 번째 항목은 'TIME'이어야 합니다. 입력값: " + parts[0],
                lineNumber
            );
        }

        // TIME을 제외한 의사 번호들만 검증
        String[] doctorIds = new String[parts.length - 1];
        Set<String> duplicateCheck = new HashSet<>();

        for (int i = 1; i < parts.length; i++) {
            String doctorId = parts[i];

            // 형식 검증
            if (!DOCTOR_ID_PATTERN.matcher(doctorId).matches()) {
                throw new AppointmentFileException(
                    AppointmentFileException.ErrorType.INVALID_DOCTOR_LIST,
                    "잘못된 의사 번호 형식: " + doctorId + " (D##### 형식이어야 합니다)",
                    lineNumber
                );
            }

            // 중복 검증
            if (!duplicateCheck.add(doctorId)) {
                throw new AppointmentFileException(
                    AppointmentFileException.ErrorType.INVALID_DOCTOR_LIST,
                    "중복된 의사 번호: " + doctorId,
                    lineNumber
                );
            }

            doctorIds[i - 1] = doctorId;
        }

        return doctorIds;
    }

    /**
     * 시간 슬롯 라인을 검증합니다
     *
     * @param timeSlotLine 시간 슬롯 라인
     * @param expectedDoctorCount 예상되는 의사 수
     * @param lineNumber 라인 번호
     * @throws AppointmentFileException 형식이 올바르지 않은 경우
     */
    public static void validateTimeSlotLine(String timeSlotLine, int expectedDoctorCount, int lineNumber)
            throws AppointmentFileException {
        if (timeSlotLine == null || timeSlotLine.trim().isEmpty()) {
            throw new AppointmentFileException(
                AppointmentFileException.ErrorType.INVALID_TIME_SLOT,
                "시간 슬롯 라인이 비어있습니다",
                lineNumber
            );
        }

        String[] parts = timeSlotLine.trim().split("\\s+");

        // 시간 + 의사 수만큼의 예약 상태
        int expectedParts = 1 + expectedDoctorCount;
        if (parts.length != expectedParts) {
            throw new AppointmentFileException(
                AppointmentFileException.ErrorType.COLUMN_MISMATCH,
                String.format("예상 열 수: %d, 실제 열 수: %d", expectedParts, parts.length),
                lineNumber
            );
        }

        // 시간 형식 검증
        if (!TIME_PATTERN.matcher(parts[0]).matches()) {
            throw new AppointmentFileException(
                AppointmentFileException.ErrorType.INVALID_TIME_SLOT,
                "잘못된 시간 형식: " + parts[0] + " (HH:MM 형식이어야 합니다)",
                lineNumber
            );
        }

        // 시간 범위 및 간격 검증
        validateTimeRange(parts[0], lineNumber);

        // 각 예약 상태 검증
        for (int i = 1; i < parts.length; i++) {
            validateAppointmentStatus(parts[i], lineNumber, i);
        }
    }

    /**
     * 시간이 진료 시간 범위(09:00-17:50)와 10분 간격에 맞는지 검증합니다
     *
     * @param timeStr 검증할 시간 문자열
     * @param lineNumber 라인 번호
     * @throws AppointmentFileException 형식이 올바르지 않은 경우
     */
    public static void validateTimeRange(String timeStr, int lineNumber) throws AppointmentFileException {
        try {
            LocalTime time = LocalTime.parse(timeStr, TIME_FORMATTER);

            // 진료 시간 범위 검증 (09:00 ~ 17:50)
            if (time.isBefore(START_TIME) || time.isAfter(END_TIME)) {
                throw new AppointmentFileException(
                    AppointmentFileException.ErrorType.INVALID_TIME_SLOT,
                    String.format("시간은 %s ~ %s 사이여야 합니다. 입력값: %s",
                        START_TIME, END_TIME, timeStr),
                    lineNumber
                );
            }

            // 10분 간격 검증
            int minutes = time.getMinute();
            if (minutes % TIME_INTERVAL_MINUTES != 0) {
                throw new AppointmentFileException(
                    AppointmentFileException.ErrorType.INVALID_TIME_SLOT,
                    String.format("시간은 %d분 간격이어야 합니다. 입력값: %s (분: %d)",
                        TIME_INTERVAL_MINUTES, timeStr, minutes),
                    lineNumber
                );
            }

        } catch (DateTimeParseException e) {
            throw new AppointmentFileException(
                AppointmentFileException.ErrorType.INVALID_TIME_SLOT,
                "유효하지 않은 시간: " + timeStr,
                lineNumber
            );
        }
    }

    /**
     * 예약 상태를 검증합니다
     *
     * @param status 예약 상태 (0, R########(1-4) 또는 X)
     * @param lineNumber 라인 번호
     * @param columnNumber 열 번호
     * @throws AppointmentFileException 형식이 올바르지 않은 경우
     */
    public static void validateAppointmentStatus(String status, int lineNumber, int columnNumber)
            throws AppointmentFileException {
        if (status == null || status.trim().isEmpty()) {
            throw new AppointmentFileException(
                AppointmentFileException.ErrorType.INVALID_APPOINTMENT_STATUS,
                String.format("열 %d: 예약 상태가 비어있습니다", columnNumber),
                lineNumber
            );
        }

        String trimmedStatus = status.trim();

        // 0 (예약 가능)
        if ("0".equals(trimmedStatus)) {
            return;
        }

        // X (진료 불가)
        if ("X".equals(trimmedStatus)) {
            return;
        }

        // R########(1-4) (예약 번호와 상태 코드)
        // 1:예약완료, 2:진료완료, 3:취소, 4:노쇼
        if (RESERVATION_ID_PATTERN.matcher(trimmedStatus).matches()) {
            return;
        }

        throw new AppointmentFileException(
            AppointmentFileException.ErrorType.INVALID_APPOINTMENT_STATUS,
            String.format("열 %d: 잘못된 예약 상태 '%s' (0, X 또는 R########(1-4) 형식이어야 합니다)",
                columnNumber, trimmedStatus),
            lineNumber
        );
    }

    /**
     * 파일 라인 수를 검증합니다
     *
     * @param actualLines 실제 라인 수
     * @param minimumLines 최소 라인 수
     * @throws AppointmentFileException 라인 수가 부족한 경우
     */
    public static void validateMinimumLines(int actualLines, int minimumLines) throws AppointmentFileException {
        if (actualLines < minimumLines) {
            throw new AppointmentFileException(
                AppointmentFileException.ErrorType.MISSING_REQUIRED_LINE,
                String.format("최소 %d개 라인이 필요하지만 %d개만 있습니다", minimumLines, actualLines)
            );
        }
    }

    /**
     * 시간 슬롯이 완전한지 검증합니다 (09:00 ~ 17:50, 10분 간격)
     *
     * @param timeSlots 시간 슬롯 리스트
     * @throws AppointmentFileException 누락된 시간대가 있는 경우
     */
    public static void validateTimeSlotCompleteness(java.util.List<String> timeSlots) throws AppointmentFileException {
        // 예상되는 시간 슬롯 개수: 09:00 ~ 17:50, 10분 간격 = 54개
        int expectedSlots = 54;

        if (timeSlots.size() != expectedSlots) {
            throw new AppointmentFileException(
                AppointmentFileException.ErrorType.MISSING_REQUIRED_LINE,
                String.format("시간 슬롯은 %d개여야 합니다 (09:00~17:50, 10분 간격). 현재: %d개",
                    expectedSlots, timeSlots.size())
            );
        }

        // 시간 순서 및 연속성 검증
        Set<String> timeSet = new HashSet<>(timeSlots);
        if (timeSet.size() != timeSlots.size()) {
            throw new AppointmentFileException(
                AppointmentFileException.ErrorType.INVALID_TIME_SLOT,
                "중복된 시간 슬롯이 있습니다"
            );
        }

        // 모든 필요한 시간대가 있는지 확인
        LocalTime currentTime = START_TIME;
        int lineNumber = 3; // 시간 슬롯은 3행부터 시작

        for (String timeSlot : timeSlots) {
            String expectedTime = currentTime.format(TIME_FORMATTER);

            if (!timeSlot.equals(expectedTime)) {
                throw new AppointmentFileException(
                    AppointmentFileException.ErrorType.INVALID_TIME_SLOT,
                    String.format("예상 시간: %s, 실제 시간: %s", expectedTime, timeSlot),
                    lineNumber
                );
            }

            currentTime = currentTime.plusMinutes(TIME_INTERVAL_MINUTES);
            lineNumber++;
        }
    }
}
