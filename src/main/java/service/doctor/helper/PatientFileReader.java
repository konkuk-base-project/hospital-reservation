package service.doctor.helper;

import util.file.FileUtil;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 환자 파일 읽기 전용 헬퍼 클래스
 */
public class PatientFileReader {

    /**
     * 예약 정보를 담는 클래스
     */
    public static class ReservationData {
        public String reservationId;
        public String patientId;
        public String patientName;
        public String date;
        public String startTime;
        public String endTime;
        public String deptCode;
        public String doctorId;
        public String status;
    }

    /**
     * 특정 예약번호로 예약 정보 찾기
     */
    public static ReservationData findReservationById(String reservationId) throws IOException {
        List<String> patientList = FileUtil.readLines("data/patient/patientlist.txt");

        for (int i = 1; i < patientList.size(); i++) {
            String line = patientList.get(i).trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            if (parts.length < 5) continue;

            String patientId = parts[0];
            String patientName = parts[2];
            String patientFilePath = "data/patient/" + patientId + ".txt";

            if (!FileUtil.resourceExists(patientFilePath)) continue;

            List<String> patientLines = FileUtil.readLines(patientFilePath);

            // 4행부터 예약 내역
            for (int j = 3; j < patientLines.size(); j++) {
                String reservationLine = patientLines.get(j).trim();
                if (reservationLine.isEmpty()) continue;

                String[] resParts = reservationLine.split("\\s+");
                if (resParts.length < 7) continue;

                if (resParts[0].equals(reservationId)) {
                    ReservationData data = new ReservationData();
                    data.reservationId = resParts[0];
                    data.patientId = patientId;
                    data.patientName = patientName;
                    data.date = resParts[1];
                    data.startTime = resParts[2];
                    data.endTime = resParts[3];
                    data.deptCode = resParts[4];
                    data.doctorId = resParts[5];
                    data.status = resParts[6];
                    return data;
                }
            }
        }

        return null;
    }

    /**
     * 특정 의사의 미래 예약 찾기 (요일 필터링)
     */
    public static List<ReservationData> findFutureReservationsByDoctorAndDay(
            String doctorId, String dayCode, LocalDate currentDate) throws IOException {

        return findReservationsByFilter(
                doctorId,
                "1",  // 예약완료 상태만
                (resDate) -> resDate.isAfter(currentDate),  // 미래만
                (resDayCode) -> resDayCode.equals(dayCode)  // 특정 요일만
        );
    }

    /**
     * 특정 의사의 미래 예약 찾기 (시간 범위 필터링)
     */
    public static List<ReservationData> findFutureReservationsByDoctorAndTimeRange(
            String doctorId, String dayCode, LocalDate currentDate,
            String newStartTime, String newEndTime) throws IOException {

        return findReservationsByFilter(
                doctorId,
                "1",  // 예약완료 상태만
                (resDate) -> resDate.isAfter(currentDate),  // 미래만
                (resDayCode) -> resDayCode.equals(dayCode),  // 특정 요일
                (startTime) -> {
                    // 새 시간 범위를 벗어나는지 확인
                    return startTime.compareTo(newStartTime) < 0 ||
                            startTime.compareTo(newEndTime) >= 0;
                }
        );
    }

    /**
     * 특정 의사의 처리 대기 중인 예약 찾기
     */
    public static List<ReservationData> findPendingReservationsByDoctor(
            String doctorId, LocalDate currentDate) throws IOException {

        return findReservationsByFilter(
                doctorId,
                "1",  // 예약완료 상태만
                (resDate) -> !resDate.isAfter(currentDate),  // 과거 또는 오늘
                (resDayCode) -> true  // 모든 요일
        );
    }

    /**
     * 통합 필터링 메서드 (내부 사용)
     */
    private static List<ReservationData> findReservationsByFilter(
            String doctorId,
            String statusFilter,
            DateFilter dateFilter,
            DayFilter dayFilter) throws IOException {
        return findReservationsByFilter(doctorId, statusFilter, dateFilter, dayFilter, null);
    }

    private static List<ReservationData> findReservationsByFilter(
            String doctorId,
            String statusFilter,
            DateFilter dateFilter,
            DayFilter dayFilter,
            TimeFilter timeFilter) throws IOException {

        List<ReservationData> result = new ArrayList<>();
        List<String> patientList = FileUtil.readLines("data/patient/patientlist.txt");

        for (int i = 1; i < patientList.size(); i++) {
            String line = patientList.get(i).trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            if (parts.length < 5) continue;

            String patientId = parts[0];
            String patientName = parts[2];
            String patientFilePath = "data/patient/" + patientId + ".txt";

            if (!FileUtil.resourceExists(patientFilePath)) continue;

            List<String> patientLines = FileUtil.readLines(patientFilePath);

            for (int j = 3; j < patientLines.size(); j++) {
                String resLine = patientLines.get(j).trim();
                if (resLine.isEmpty()) continue;

                String[] resParts = resLine.split("\\s+");
                if (resParts.length < 7) continue;

                String reservationId = resParts[0];
                String date = resParts[1];
                String startTime = resParts[2];
                String endTime = resParts[3];
                String deptCode = resParts[4];
                String resDoctorId = resParts[5];
                String status = resParts[6];

                // 의사 및 상태 필터
                if (!resDoctorId.equals(doctorId) || !status.equals(statusFilter)) {
                    continue;
                }

                // 날짜 필터
                LocalDate resDate = LocalDate.parse(date,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                if (!dateFilter.test(resDate)) {
                    continue;
                }

                // 요일 필터
                String resDayCode = getDayCodeFromDate(resDate);
                if (!dayFilter.test(resDayCode)) {
                    continue;
                }

                // 시간 필터 (선택적)
                if (timeFilter != null && !timeFilter.test(startTime)) {
                    continue;
                }

                ReservationData data = new ReservationData();
                data.reservationId = reservationId;
                data.patientId = patientId;
                data.patientName = patientName;
                data.date = date;
                data.startTime = startTime;
                data.endTime = endTime;
                data.deptCode = deptCode;
                data.doctorId = resDoctorId;
                data.status = status;

                result.add(data);
            }
        }

        return result;
    }

    /**
     * 날짜로부터 요일 코드 얻기
     */
    private static String getDayCodeFromDate(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "MON";
            case TUESDAY -> "TUE";
            case WEDNESDAY -> "WED";
            case THURSDAY -> "THU";
            case FRIDAY -> "FRI";
            case SATURDAY -> "SAT";
            case SUNDAY -> "SUN";
        };
    }

    // 함수형 인터페이스
    @FunctionalInterface
    private interface DateFilter {
        boolean test(LocalDate date);
    }

    @FunctionalInterface
    private interface DayFilter {
        boolean test(String dayCode);
    }

    @FunctionalInterface
    private interface TimeFilter {
        boolean test(String time);
    }
}