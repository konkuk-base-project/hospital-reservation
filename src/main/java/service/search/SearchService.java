package service.search;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import model.User;
import repository.AppointmentRepository;
import service.AuthContext;
import util.exception.AppointmentFileException;
import util.exception.SearchException;
import util.file.FileUtil;

public class SearchService {
    private final AuthContext authContext;
    private final AppointmentRepository appointmentRepository;

    public SearchService(AuthContext authContext) {
        this.authContext = authContext;
        this.appointmentRepository = new AppointmentRepository();
    }

    /**
     * 6.3.1 전체 예약 조회
     */
    public void showMyList(String[] args) throws SearchException {
        if (!authContext.isLoggedIn()) {
            throw new SearchException("로그인이 필요합니다.");
        }

        if (args.length > 0) {
            throw new SearchException("인자가 없어야 합니다.");
        }

        User currentUser = authContext.getCurrentUser();
        String patientId = currentUser.getId();

        if (patientId == null) {
            throw new SearchException("환자 정보를 찾을 수 없습니다.");
        }

        try {
            String filePath = "data/patient/" + patientId + ".txt";
            List<String> lines = FileUtil.readLines(filePath);

            List<String> reservations = new ArrayList<>();
            for (int i = 3; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (!line.isEmpty()) {
                    reservations.add(line);
                }
            }

            System.out.println("======================================================================================");
            System.out.println("전체 예약 내역 (총 " + reservations.size() + "건)");
            System.out.println("======================================================================================");

            if (reservations.isEmpty()) {
                System.out.println("예약 내역이 없습니다.");
            } else {
                for (String reservation : reservations) {
                    String[] parts = reservation.split("\\s+");
                    // R00000001 | 2025-10-05 | 09:30-09:40 | 내과 | 김의사 | [예약중]
                    String status = getStatusString(parts[6]);
                    String deptName = getDepartmentName(parts[4]);
                    String doctorName = getDoctorName(parts[5]);
                    System.out.printf("%s | %s | %s-%s | %s | %s | [%s]%n",
                            parts[0], parts[1], parts[2], parts[3], deptName, doctorName, status);
                }
            }
        } catch (IOException e) {
            throw new SearchException("예약 내역을 조회하는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 6.3.2 진료과별 검색 (날짜 조건 없음)
     */
    public void searchByDepartment(String[] args) throws SearchException {
        if (!authContext.isLoggedIn()) {
            throw new SearchException("로그인이 필요합니다.");
        }

        if (args.length == 0) {
            throw new SearchException("인자가 부족합니다. (형식: dept <진료과 코드>)");
        }

        String deptCode = args[0].toUpperCase();

        // 진료과 코드 검증
        if (!isValidDepartment(deptCode)) {
            throw new SearchException("존재하지 않는 진료과입니다.\n사용 가능한 진료과: IM(내과), GS(일반외과), OB(산부인과), PED(소아과), PSY(정신과), DERM(피부과), ENT(이비인후과), ORTH(정형외과)");
        }

        LocalDate date = null;
        if (args.length == 2) {
            // 6.3.4 날짜 조건 검색
            date = parseAndValidateDate(args[1]);
        }

        // 해당 진료과 의사 찾기 및 예약 가능 시간 조회
        showAvailableSlotsByDepartment(deptCode, date);
    }

    /**
     * 6.3.3 의사별 검색
     */
    public void searchByDoctor(String[] args) throws SearchException {
        if (!authContext.isLoggedIn()) {
            throw new SearchException("로그인이 필요합니다.");
        }

        if (args.length == 0) {
            throw new SearchException("인자가 부족합니다. (형식: doctor <의사번호>)");
        }

        if (args.length > 1) {
            throw new SearchException("인자가 잘못된 형식입니다.");
        }

        String doctorId = args[0];

        if (!doctorId.matches("D\\d{5}")) {
            throw new SearchException("인자가 잘못된 형식입니다.");
        }

        if (!isDoctorExists(doctorId)) {
            throw new SearchException("의사번호가 존재하지 않습니다.");
        }

        showDoctorAvailableSlots(doctorId, null);
    }

    /**
     * 진료과별 예약 가능 시간 조회
     */
    private void showAvailableSlotsByDepartment(String deptCode, LocalDate date) throws SearchException {
        try {
            List<String> lines = FileUtil.readLines("data/doctor/doctorlist.txt");
            List<DoctorInfo> doctors = new ArrayList<>();

            // 해당 진료과 의사 찾기
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length >= 3 && parts[2].equals(deptCode)) {
                    doctors.add(new DoctorInfo(parts[0], parts[1], parts[2]));
                }
            }

            if (doctors.isEmpty()) {
                throw new SearchException("해당 진료과에 의사가 없습니다.");
            }

            String deptName = getDepartmentName(deptCode);

            if (date == null) {
                System.out.println("======================================================================================");
                System.out.println(deptName + "(" + deptCode + ") 예약 가능 일정");
                System.out.println("======================================================================================");
            } else {
                System.out.println("======================================================================================");
                System.out.println(deptName + "(" + deptCode + ") 예약 가능 일정 (" + date + ")");
                System.out.println("======================================================================================");
            }

            boolean hasAvailable = false;

            for (DoctorInfo doctor : doctors) {
                List<String> availableSlots = getAvailableSlots(doctor.id, date);

                if (!availableSlots.isEmpty()) {
                    hasAvailable = true;
                    System.out.println("의사: " + doctor.name + " (" + doctor.id + ")");
                    for (String slot : availableSlots) {
                        System.out.println("- " + slot);
                    }
                }
            }

            if (!hasAvailable) {
                System.out.println("예약 가능한 시간이 없습니다.");
            }

        } catch (IOException e) {
            throw new SearchException("진료과 정보를 조회하는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 의사별 예약 가능 시간 조회
     */
    private void showDoctorAvailableSlots(String doctorId, LocalDate date) throws SearchException {
        try {
            String doctorName = getDoctorName(doctorId);

            System.out.println("======================================================================================");
            System.out.println(doctorName + " 예약 가능 일정 ");
            System.out.println("======================================================================================");

            List<String> availableSlots = getAvailableSlots(doctorId, date);

            if (availableSlots.isEmpty()) {
                System.out.println("예약 가능한 시간이 없습니다.");
            } else {
                for (String slot : availableSlots) {
                    System.out.println("- " + slot);
                }
            }

        } catch (IOException e) {
            throw new SearchException("의사 정보를 조회하는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 예약 가능한 시간 슬롯 가져오기
     */
    private List<String> getAvailableSlots(String doctorId, LocalDate date) throws SearchException {
        List<String> availableSlots = new ArrayList<>();

        try {
            if (date == null) {
                // 날짜 지정 없음 - 의사 파일에서 모든 예약 가능 시간 가져오기
                String doctorFilePath = "data/doctor/" + doctorId + ".txt";
                List<String> lines = FileUtil.readLines(doctorFilePath);

                for (int i = 3; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    if (line.isEmpty()) continue;

                    String[] parts = line.split("\\s+");
                    String scheduleDate = parts[0];

                    // 각 슬롯 확인 (54개)
                    for (int j = 1; j < parts.length && j <= 54; j++) {
                        if ("0".equals(parts[j])) {
                            String time = getTimeFromSlotIndex(j - 1);
                            availableSlots.add(scheduleDate + " " + time);
                        }
                    }
                }
            } else {
                // 날짜 지정 있음 - AppointmentRepository 사용
                try {
                    List<String> slots = appointmentRepository.getAvailableTimeSlots(date, doctorId);
                    availableSlots.addAll(slots);
                } catch (AppointmentFileException e) {
                    // 파일이 없으면 빈 리스트 반환
                }
            }
        } catch (IOException e) {
            throw new SearchException("예약 가능 시간을 조회하는 중 오류가 발생했습니다.");
        }

        return availableSlots;
    }

    /**
     * 슬롯 인덱스에서 시간 계산 (0 -> 09:00, 1 -> 09:10, ...)
     */
    private String getTimeFromSlotIndex(int index) {
        int hour = 9 + (index / 6);
        int minute = (index % 6) * 10;
        return String.format("%02d:%02d", hour, minute);
    }

    /**
     * 날짜 파싱 및 검증
     */
    private LocalDate parseAndValidateDate(String dateStr) throws SearchException {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            if (date.isBefore(LocalDate.now())) {
                throw new SearchException("과거 날짜로는 조회할 수 없습니다.");
            }
            return date;
        } catch (DateTimeParseException e) {
            throw new SearchException("날짜 형식이 잘못되었습니다. (예: 2025-10-10)");
        }
    }

    /**
     * 의사 이름으로 의사번호 찾기
     */
    private String findDoctorIdByName(String doctorName) {
        try {
            List<String> lines = FileUtil.readLines("data/doctor/doctorlist.txt");
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length >= 2 && parts[1].equals(doctorName)) {
                    return parts[0];
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    /**
     * 예약 상태 문자열 반환
     */
    private String getStatusString(String code) {
        switch (code) {
            case "1": return "예약중";
            case "2": return "진료완료";
            case "3": return "취소";
            case "4": return "미방문";
            default: return "알 수 없음";
        }
    }

    /**
     * 진료과 이름 반환
     */
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

    /**
     * 의사 이름 가져오기
     */
    private String getDoctorName(String doctorId) throws IOException {
        List<String> lines = FileUtil.readLines("data/doctor/" + doctorId + ".txt");
        if (!lines.isEmpty()) {
            String[] parts = lines.get(0).split("\\s+");
            return parts[1];
        }
        return doctorId;
    }

    /**
     * 진료과 코드 유효성 검증
     */
    private boolean isValidDepartment(String code) {
        return code.matches("^(IM|GS|OB|PED|PSY|DERM|ENT|ORTH)$");
    }

    /**
     * 의사 존재 여부 확인
     */
    private boolean isDoctorExists(String doctorId) {
        return FileUtil.resourceExists("data/doctor/" + doctorId + ".txt");
    }

    /**
     * 의사 정보를 담는 내부 클래스
     */
    private static class DoctorInfo {
        String id;
        String name;
        String department;

        DoctorInfo(String id, String name, String department) {
            this.id = id;
            this.name = name;
            this.department = department;
        }
    }
}