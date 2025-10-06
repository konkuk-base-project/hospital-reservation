package repository;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import model.Patient;
import util.file.FileUtil;

public class PatientRepository {
    private static final String PATIENT_LIST_FILE_PATH = "data/patient/patientlist.txt";
    private static final String PATIENT_DIR_PATH = "data/patient/";
    private final List<Patient> patients = new ArrayList<>();
    private int lastPatientNumber = 0;

    public PatientRepository() {
        loadPatientsFromFile();
    }

    private void loadPatientsFromFile() {
        try {
            List<String> lines = FileUtil.readLines(PATIENT_LIST_FILE_PATH);
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\s+");
                if (parts.length == 5) {
                    Patient patient = new Patient(parts[0], parts[1], parts[2], parts[3], parts[4]);
                    patients.add(patient);
                    int currentNum = Integer.parseInt(parts[0].substring(1));
                    if (currentNum > lastPatientNumber) {
                        lastPatientNumber = currentNum;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("[오류] 환자 목록 파일을 읽는 중 오류가 발생했습니다: " + e.getMessage());
            System.exit(1);
        }
    }

    public void save(Patient patient) throws IOException {
        FileUtil.appendLine(PATIENT_LIST_FILE_PATH, patient.toPatientListString());
        createPatientDetailFile(patient);
        patients.add(patient);
    }

    private void createPatientDetailFile(Patient patient) throws IOException {
        Path filePath = Paths.get(PATIENT_DIR_PATH, patient.getPatientId() + ".txt");
        String header = patient.toDetailFileHeaderString();
        String reservationHeader = "[예약번호] [예약날짜] [시작시간] [종료시간] [진료과] [의사번호] [상태]";
        List<String> content = List.of(header, "", reservationHeader);
        FileUtil.createDirectoriesAndWrite(filePath, content);
    }

    public String getNextPatientId() {
        lastPatientNumber++;
        return String.format("P%06d", lastPatientNumber);
    }
}