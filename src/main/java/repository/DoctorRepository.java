package repository;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import model.Doctor;
import util.file.FileUtil;

public class DoctorRepository {
    private static final String DOCTOR_LIST_FILE_PATH = "data/doctor/doctorlist.txt";
    private static final String DOCTOR_DIR_PATH = "data/doctor/";
    private final List<Doctor> doctors = new ArrayList<>();
    private int lastDoctorNumber = 0;

    public DoctorRepository() {
        loadDoctorsFromFile();
    }

    private void loadDoctorsFromFile() {
        try {
            List<String> lines = FileUtil.readLines(DOCTOR_LIST_FILE_PATH);
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\s+");
                if (parts.length == 5) {
                    // 새 형식: doctorId name deptCode phoneNumber registrationDate
                    Doctor doctor = new Doctor(parts[0], null, parts[1], parts[2], parts[3], parts[4]);
                    doctors.add(doctor);
                    int currentNum = Integer.parseInt(parts[0].substring(1));
                    if (currentNum > lastDoctorNumber) {
                        lastDoctorNumber = currentNum;
                    }
                } else if (parts.length == 3) {
                    // 하위 호환성: 기존 형식 지원 (username과 phoneNumber, registrationDate는 null)
                    Doctor doctor = new Doctor(parts[0], null, parts[1], parts[2], null, null);
                    doctors.add(doctor);
                    int currentNum = Integer.parseInt(parts[0].substring(1));
                    if (currentNum > lastDoctorNumber) {
                        lastDoctorNumber = currentNum;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("[오류] 의사 목록 파일을 읽는 중 오류가 발생했습니다: " + e.getMessage());
            System.exit(1);
        }
    }

    public void save(Doctor doctor) throws IOException {
        FileUtil.appendLine(DOCTOR_LIST_FILE_PATH, doctor.toDoctorListString());
        createDoctorDetailFiles(doctor);
        doctors.add(doctor);
    }

    private void createDoctorDetailFiles(Doctor doctor) throws IOException {
        // D00001.txt 파일 생성
        Path filePath = Paths.get(DOCTOR_DIR_PATH, doctor.getDoctorId() + ".txt");
        String header = doctor.toDetailFileHeaderString();
        String weekdaySchedule = "0 0 0 0 0"; // 초기에는 모든 요일 진료 불가

        // 파일 내용: 헤더, 요일 정보, 빈 행, 날짜별 스케줄 섹션 빈 행, 요일별 정기 일정 섹션
        List<String> content = List.of(
                header,
                weekdaySchedule,
                ""
                // 날짜별 스케줄은 예약 생성 시 추가됨
                // 요일별 정기 일정은 의사가 설정할 때 추가됨
        );
        FileUtil.createDirectoriesAndWrite(filePath, content);

        // D00001-master.txt 파일 생성 (하위 호환성 유지)
        Path masterFilePath = Paths.get(DOCTOR_DIR_PATH, doctor.getDoctorId() + "-master.txt");
        List<String> masterContent = List.of(
                "MON 0 0",
                "TUE 0 0",
                "WED 0 0",
                "THU 0 0",
                "FRI 0 0",
                "SAT 0 0",
                "SUN 0 0"
        );
        FileUtil.createDirectoriesAndWrite(masterFilePath, masterContent);
    }

    public String getNextDoctorId() {
        lastDoctorNumber++;
        return String.format("D%05d", lastDoctorNumber);
    }

    public boolean isDoctorExists(String doctorId) {
        return doctors.stream().anyMatch(d -> d.getDoctorId().equals(doctorId));
    }
}