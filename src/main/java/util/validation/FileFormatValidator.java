package util.validation;

import util.exception.FileFormatException;
import util.file.FileUtil;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class FileFormatValidator {

    private static final Pattern PATIENT_ID_PATTERN = Pattern.compile("^P\\d{6}$");
    private static final Pattern DOCTOR_ID_PATTERN = Pattern.compile("^D\\d{5}$");
    private static final Pattern RESERVATION_ID_PATTERN = Pattern.compile("^R\\d{8}$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern TIME_PATTERN = Pattern.compile("^\\d{2}:\\d{2}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{3}-\\d{4}-\\d{4}$");
    private static final Pattern DEPARTMENT_PATTERN = Pattern.compile("^(IM|GS|OB|PED|PSY|DERM|ENT|ORTH)$");

    public void validate() {
        System.out.print("파일 형식을 검증합니다...");

        try {
            validatePatientListFile();
            validateDoctorListFile();
            validateVirtualTimeFile();
            validateCredentialsFile();
            validatePatientDetailFiles();
            validateDoctorDetailFiles();

            System.out.println(" 성공");
        } catch (FileFormatException e) {
            System.out.println();
            System.out.println(e.getMessage());
            System.exit(0);
        } catch (Exception e) {
            System.out.println();
            System.out.println("[오류] 예기치 못한 오류가 발생하였습니다. 프로그램을 종료합니다.");
            System.exit(0);
        }
    }

    private void validatePatientListFile() {
        String filePath = "data/patient/patientlist.txt";
        try {
            List<String> lines = FileUtil.readLines(filePath);

            if (lines.isEmpty()) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            // 헤더 검증
            String header = lines.get(0).trim();
            if (!header.equals("[환자 번호] [아이디] [환자 이름] [생년월일] [전화번호]")) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            // 데이터 행 검증
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length != 5) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // 환자번호 형식 검증
                if (!PATIENT_ID_PATTERN.matcher(parts[0]).matches()) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // 생년월일 형식 검증
                if (!DATE_PATTERN.matcher(parts[3]).matches()) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // 전화번호 형식 검증
                if (!PHONE_PATTERN.matcher(parts[4]).matches()) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }
            }
        } catch (IOException e) {
            throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
        }
    }

    private void validateDoctorListFile() {
        String filePath = "data/doctor/doctorlist.txt";
        try {
            List<String> lines = FileUtil.readLines(filePath);

            if (lines.isEmpty()) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            // 헤더 검증
            String header = lines.get(0).trim();
            if (!header.equals("[의사번호] [의사이름] [진료과 코드]")) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            // 데이터 행 검증
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length != 3) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // 의사번호 형식 검증
                if (!DOCTOR_ID_PATTERN.matcher(parts[0]).matches()) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // 진료과 코드 형식 검증
                if (!DEPARTMENT_PATTERN.matcher(parts[2]).matches()) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }
            }
        } catch (IOException e) {
            throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
        }
    }

    private void validateVirtualTimeFile() {
        String filePath = "resource/virtualtime.txt";
        try {
            List<String> lines = FileUtil.readLines(filePath);

            if (lines.isEmpty()) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            boolean hasBaseTime = false;
            boolean hasLastSave = false;
            boolean hasProgramStart = false;

            Pattern datetimePattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$");

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("=");
                if (parts.length != 2) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                String key = parts[0].trim();
                String value = parts[1].trim();

                if (key.equals("BASE_TIME")) {
                    hasBaseTime = true;
                    if (!datetimePattern.matcher(value).matches()) {
                        throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                    }
                } else if (key.equals("LAST_SAVE")) {
                    hasLastSave = true;
                    if (!datetimePattern.matcher(value).matches()) {
                        throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                    }
                } else if (key.equals("PROGRAM_START")) {
                    hasProgramStart = true;
                    if (!datetimePattern.matcher(value).matches()) {
                        throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                    }
                }
            }

            if (!hasBaseTime || !hasLastSave || !hasProgramStart) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }
        } catch (IOException e) {
            throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
        }
    }

    private void validateCredentialsFile() {
        String filePath = "data/auth/credentials.txt";
        try {
            List<String> lines = FileUtil.readLines(filePath);

            if (lines.isEmpty()) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            // 헤더 검증
            String header = lines.get(0).trim();
            if (!header.equals("[아이디] [비밀번호] [계정타입] [식별번호]")) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            // 데이터 행 검증
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length != 4) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // 계정 타입 검증
                if (!parts[2].equals("USER") && !parts[2].equals("ADMIN")) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // 식별번호 형식 검증
                if (parts[2].equals("USER")) {
                    if (!PATIENT_ID_PATTERN.matcher(parts[3]).matches()) {
                        throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                    }
                } else if (parts[2].equals("ADMIN")) {
                    if (!parts[3].matches("^A\\d{6}$")) {
                        throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                    }
                }
            }
        } catch (IOException e) {
            throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
        }
    }

    private void validatePatientDetailFiles() {
        try {
            List<String> lines = FileUtil.readLines("data/patient/patientlist.txt");

            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length > 0) {
                    String patientId = parts[0];
                    String detailFilePath = "data/patient/" + patientId + ".txt";

                    if (!FileUtil.resourceExists(detailFilePath)) {
                        String formattedPath = "/" + detailFilePath.replace("\\", "/");
                        throw new FileFormatException("[오류] '" + formattedPath + "'이(가) 존재하지 않습니다. 프로그램을 종료합니다.");
                    }

                    validatePatientDetailFile(patientId);
                }
            }
        } catch (IOException e) {
            throw new FileFormatException("[오류] 환자 개인 데이터 파일의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
        }
    }

    private void validatePatientDetailFile(String patientId) {
        String filePath = "data/patient/" + patientId + ".txt";
        try {
            List<String> lines = FileUtil.readLines(filePath);

            if (lines.size() < 3) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            // 1행: 환자 기본 정보
            String[] basicInfo = lines.get(0).trim().split("\\s+");
            if (basicInfo.length != 4) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }
            if (!PATIENT_ID_PATTERN.matcher(basicInfo[0]).matches()) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }
            if (!DATE_PATTERN.matcher(basicInfo[2]).matches()) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }
            if (!PHONE_PATTERN.matcher(basicInfo[3]).matches()) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            // 2행: 빈 행
            if (!lines.get(1).trim().isEmpty()) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            // 3행: 예약 이력 헤더
            String header = lines.get(2).trim();
            if (!header.equals("[예약번호] [예약날짜] [시작시간] [종료시간] [진료과] [의사번호] [상태]")) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            // 4행부터: 예약 내역 검증
            for (int i = 3; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length != 7) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // 예약번호 검증
                if (!RESERVATION_ID_PATTERN.matcher(parts[0]).matches()) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // 예약날짜 검증
                if (!DATE_PATTERN.matcher(parts[1]).matches()) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // 시작시간/종료시간 검증
                if (!TIME_PATTERN.matcher(parts[2]).matches() || !TIME_PATTERN.matcher(parts[3]).matches()) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // 진료과 검증
                if (!DEPARTMENT_PATTERN.matcher(parts[4]).matches()) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // 의사번호 검증
                if (!DOCTOR_ID_PATTERN.matcher(parts[5]).matches()) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // 상태 검증 (1, 2, 3, 4 중 하나)
                if (!parts[6].matches("^[1234]$")) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }
            }
        } catch (IOException e) {
            throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
        }
    }

    private void validateDoctorDetailFiles() {
        try {
            List<String> lines = FileUtil.readLines("data/doctor/doctorlist.txt");

            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length > 0) {
                    String doctorId = parts[0];
                    String detailFilePath = "data/doctor/" + doctorId + ".txt";

                    if (!FileUtil.resourceExists(detailFilePath)) {
                        String formattedPath = "/" + detailFilePath.replace("\\", "/");
                        throw new FileFormatException("[오류] '" + formattedPath + "'이(가) 존재하지 않습니다. 프로그램을 종료합니다.");
                    }

                    validateDoctorDetailFile(doctorId);
                }
            }
        } catch (IOException e) {
            throw new FileFormatException("[오류] 의사 스케줄 파일의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
        }
    }

    private void validateDoctorDetailFile(String doctorId) {
        String filePath = "data/doctor/" + doctorId + ".txt";
        try {
            List<String> lines = FileUtil.readLines(filePath);

            if (lines.size() < 3) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            // 1행: 의사 정보
            String[] doctorInfo = lines.get(0).trim().split("\\s+");
            if (doctorInfo.length != 3) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }
            if (!DOCTOR_ID_PATTERN.matcher(doctorInfo[0]).matches()) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }
            if (!DEPARTMENT_PATTERN.matcher(doctorInfo[2]).matches()) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            // 2행: 진료 가능 요일
            String[] weekdays = lines.get(1).trim().split("\\s+");
            if (weekdays.length != 5) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }
            for (String day : weekdays) {
                if (!day.equals("0") && !day.equals("1")) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }
            }

            // 3행: 빈 행
            if (!lines.get(2).trim().isEmpty()) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            // 4행부터: 스케줄 검증
            for (int i = 3; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length != 55) { // 날짜 1개 + 슬롯 54개
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // 날짜 검증
                if (!DATE_PATTERN.matcher(parts[0]).matches()) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // 슬롯 검증 (0 또는 예약번호)
                for (int j = 1; j < parts.length; j++) {
                    if (!parts[j].equals("0") && !RESERVATION_ID_PATTERN.matcher(parts[j]).matches()) {
                        throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                    }
                }
            }
        } catch (IOException e) {
            throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
        }
    }
}
