package repository;

import util.exception.AppointmentFileException;
import util.file.FileUtil;
import util.validation.AppointmentFileValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 날짜별 예약 현황 파일을 처리하는 Repository 클래스
 * 파일 형식:
 * - 1행: 날짜 (YYYY-MM-DD)
 * - 2행: TIME + 의사번호 리스트 (공백으로 구분)
 * - 3행~: 시간 + 예약상태들 (0: 예약가능, R########: 예약번호, X: 진료불가)
 */
public class AppointmentRepository {

    private static final String APPOINTMENT_DIR = "data/appointment";
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MINIMUM_LINES = 3; // 날짜 + 의사목록 + 최소 1개 시간슬롯

    /**
     * 날짜별 예약 현황을 조회합니다
     *
     * @param date 조회할 날짜
     * @return 예약 현황 데이터
     * @throws AppointmentFileException 파일 읽기 또는 파싱 중 오류 발생 시
     */
    public AppointmentData getAppointmentsByDate(LocalDate date) throws AppointmentFileException {
        Path filePath = getAppointmentFilePath(date);

        try {
            List<String> lines = Files.readAllLines(filePath);
            return parseAppointmentFile(lines, date);

        } catch (NoSuchFileException e) {
            throw new AppointmentFileException(
                AppointmentFileException.ErrorType.FILE_NOT_FOUND,
                "파일: " + filePath.toString(),
                e
            );
        } catch (IOException e) {
            throw new AppointmentFileException(
                AppointmentFileException.ErrorType.FILE_READ_ERROR,
                "파일: " + filePath.toString(),
                e
            );
        }
    }

    /**
     * 특정 의사의 예약 가능한 시간대를 조회합니다
     *
     * @param date 조회할 날짜
     * @param doctorId 의사 번호
     * @return 예약 가능한 시간 리스트
     * @throws AppointmentFileException 파일 읽기 또는 파싱 중 오류 발생 시
     */
    public List<String> getAvailableTimeSlots(LocalDate date, String doctorId) throws AppointmentFileException {
        AppointmentData data = getAppointmentsByDate(date);

        int doctorIndex = -1;
        for (int i = 0; i < data.doctorIds.length; i++) {
            if (data.doctorIds[i].equals(doctorId)) {
                doctorIndex = i;
                break;
            }
        }

        if (doctorIndex == -1) {
            throw new AppointmentFileException(
                AppointmentFileException.ErrorType.INVALID_DOCTOR_LIST,
                "해당 날짜에 의사 번호 " + doctorId + "가 존재하지 않습니다"
            );
        }

        List<String> availableSlots = new ArrayList<>();
        for (TimeSlot slot : data.timeSlots) {
            if ("0".equals(slot.statuses[doctorIndex])) {
                availableSlots.add(slot.time);
            }
        }

        return availableSlots;
    }

    /**
     * 예약을 생성합니다
     *
     * @param date 예약 날짜
     * @param doctorId 의사 번호
     * @param time 예약 시간
     * @param reservationId 예약 번호
     * @throws AppointmentFileException 파일 처리 중 오류 발생 시
     */
    public void createAppointment(LocalDate date, String doctorId, String time, String reservationId)
            throws AppointmentFileException {
        AppointmentData data = getAppointmentsByDate(date);

        int doctorIndex = findDoctorIndex(data.doctorIds, doctorId);
        int timeSlotIndex = findTimeSlotIndex(data.timeSlots, time);

        // 예약 가능 여부 확인
        String currentStatus = data.timeSlots.get(timeSlotIndex).statuses[doctorIndex];
        if (!"0".equals(currentStatus)) {
            throw new AppointmentFileException(
                AppointmentFileException.ErrorType.INVALID_APPOINTMENT_STATUS,
                String.format("해당 시간대는 예약할 수 없습니다. 현재 상태: %s", currentStatus)
            );
        }

        // 예약 번호로 업데이트
        data.timeSlots.get(timeSlotIndex).statuses[doctorIndex] = reservationId;

        // 파일에 저장
        saveAppointmentData(date, data);
    }

    /**
     * 예약을 취소합니다
     *
     * @param date 예약 날짜
     * @param reservationId 취소할 예약 번호
     * @throws AppointmentFileException 파일 처리 중 오류 발생 시
     */
    public void cancelAppointment(LocalDate date, String reservationId) throws AppointmentFileException {
        AppointmentData data = getAppointmentsByDate(date);

        boolean found = false;
        for (TimeSlot slot : data.timeSlots) {
            for (int i = 0; i < slot.statuses.length; i++) {
                if (reservationId.equals(slot.statuses[i])) {
                    slot.statuses[i] = "0"; // 예약 가능 상태로 변경
                    found = true;
                    break;
                }
            }
            if (found) break;
        }

        if (!found) {
            throw new AppointmentFileException(
                AppointmentFileException.ErrorType.INVALID_APPOINTMENT_STATUS,
                "예약 번호 " + reservationId + "를 찾을 수 없습니다"
            );
        }

        saveAppointmentData(date, data);
    }

    /**
     * 예약 파일 경로를 생성합니다
     */
    private Path getAppointmentFilePath(LocalDate date) {
        String fileName = date.format(FILE_DATE_FORMAT) + ".txt";
        return FileUtil.getResourcePath(APPOINTMENT_DIR).resolve(fileName);
    }

    /**
     * 예약 파일을 파싱합니다
     */
    private AppointmentData parseAppointmentFile(List<String> lines, LocalDate expectedDate)
            throws AppointmentFileException {

        // 최소 라인 수 검증
        AppointmentFileValidator.validateMinimumLines(lines.size(), MINIMUM_LINES);

        // 1행: 날짜 검증
        String dateStr = lines.get(0).trim();
        AppointmentFileValidator.validateDateFormat(dateStr, 1);

        LocalDate fileDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        if (!fileDate.equals(expectedDate)) {
            throw new AppointmentFileException(
                AppointmentFileException.ErrorType.INVALID_DATE_FORMAT,
                String.format("파일의 날짜(%s)가 요청한 날짜(%s)와 다릅니다", fileDate, expectedDate),
                1
            );
        }

        // 2행: 의사 번호 리스트 검증
        String[] doctorIds = AppointmentFileValidator.validateDoctorList(lines.get(1), 2);

        // 3행~: 시간 슬롯 파싱
        List<TimeSlot> timeSlots = new ArrayList<>();
        List<String> timeSlotsForValidation = new ArrayList<>();

        for (int i = 2; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue; // 빈 줄은 건너뜀
            }

            int lineNumber = i + 1;
            AppointmentFileValidator.validateTimeSlotLine(line, doctorIds.length, lineNumber);

            String[] parts = line.split("\\s+");
            String time = parts[0];

            String[] statuses = new String[doctorIds.length];
            System.arraycopy(parts, 1, statuses, 0, doctorIds.length);

            timeSlots.add(new TimeSlot(time, statuses));
            timeSlotsForValidation.add(time);
        }

        if (timeSlots.isEmpty()) {
            throw new AppointmentFileException(
                AppointmentFileException.ErrorType.MISSING_REQUIRED_LINE,
                "시간 슬롯 데이터가 하나도 없습니다"
            );
        }

        // 시간 슬롯 완전성 검증 (09:00~17:50, 10분 간격, 순서대로)
        AppointmentFileValidator.validateTimeSlotCompleteness(timeSlotsForValidation);

        return new AppointmentData(fileDate, doctorIds, timeSlots);
    }

    /**
     * 예약 데이터를 파일에 저장합니다
     */
    private void saveAppointmentData(LocalDate date, AppointmentData data) throws AppointmentFileException {
        Path filePath = getAppointmentFilePath(date);

        try {
            List<String> lines = new ArrayList<>();

            // 1행: 날짜
            lines.add(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

            // 2행: TIME + 의사 번호들
            StringBuilder doctorLine = new StringBuilder("TIME");
            for (String doctorId : data.doctorIds) {
                doctorLine.append(" ").append(doctorId);
            }
            lines.add(doctorLine.toString());

            // 3행~: 시간 슬롯들
            for (TimeSlot slot : data.timeSlots) {
                StringBuilder slotLine = new StringBuilder(slot.time);
                for (String status : slot.statuses) {
                    slotLine.append(" ").append(status);
                }
                lines.add(slotLine.toString());
            }

            Files.write(filePath, lines);

        } catch (IOException e) {
            throw new AppointmentFileException(
                AppointmentFileException.ErrorType.FILE_WRITE_ERROR,
                "파일: " + filePath.toString(),
                e
            );
        }
    }

    /**
     * 의사 인덱스를 찾습니다
     */
    private int findDoctorIndex(String[] doctorIds, String doctorId) throws AppointmentFileException {
        for (int i = 0; i < doctorIds.length; i++) {
            if (doctorIds[i].equals(doctorId)) {
                return i;
            }
        }
        throw new AppointmentFileException(
            AppointmentFileException.ErrorType.INVALID_DOCTOR_LIST,
            "의사 번호 " + doctorId + "를 찾을 수 없습니다"
        );
    }

    /**
     * 시간 슬롯 인덱스를 찾습니다
     */
    private int findTimeSlotIndex(List<TimeSlot> timeSlots, String time) throws AppointmentFileException {
        for (int i = 0; i < timeSlots.size(); i++) {
            if (timeSlots.get(i).time.equals(time)) {
                return i;
            }
        }
        throw new AppointmentFileException(
            AppointmentFileException.ErrorType.INVALID_TIME_SLOT,
            "시간 " + time + "을 찾을 수 없습니다"
        );
    }

    /**
     * 예약 데이터를 담는 내부 클래스
     */
    public static class AppointmentData {
        public final LocalDate date;
        public final String[] doctorIds;
        public final List<TimeSlot> timeSlots;

        public AppointmentData(LocalDate date, String[] doctorIds, List<TimeSlot> timeSlots) {
            this.date = date;
            this.doctorIds = doctorIds;
            this.timeSlots = timeSlots;
        }
    }

    /**
     * 시간 슬롯을 담는 내부 클래스
     */
    public static class TimeSlot {
        public final String time;
        public final String[] statuses; // 각 의사별 예약 상태

        public TimeSlot(String time, String[] statuses) {
            this.time = time;
            this.statuses = statuses;
        }
    }
}
