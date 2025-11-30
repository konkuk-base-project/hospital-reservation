package util.validation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import repository.AppointmentRepository;
import util.exception.AppointmentFileException;
import util.exception.FileFormatException;
import util.file.FileUtil;

public class FileFormatValidator {

    private static final Pattern PATIENT_ID_PATTERN = Pattern.compile("^P\\d{6}$");
    private static final Pattern DOCTOR_ID_PATTERN = Pattern.compile("^D\\d{5}$");
    private static final Pattern RESERVATION_ID_PATTERN = Pattern.compile("^R\\d{8}$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern TIME_PATTERN = Pattern.compile("^\\d{2}:\\d{2}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{3}-\\d{4}-\\d{4}$");
    private static final Pattern MAJOR_CODE_PATTERN = Pattern.compile("^[A-Z]{2,6}$");
    private static final Pattern DAY_OF_WEEK_PATTERN = Pattern.compile("^(MON|TUE|WED|THU|FRI|SAT|SUN)$");

    private final repository.MajorRepository majorRepository;

    public FileFormatValidator(repository.MajorRepository majorRepository) {
        this.majorRepository = majorRepository;
    }

    public void validate() {
        System.out.print("파일 형식을 검증합니다...");

        try {
            validatePatientListFile();
            validateMajorListFile();
            validateDoctorListFile();
            validateVirtualTimeFile();
            validateCredentialsFile();
            validatePatientDetailFiles();
            validateDoctorDetailFiles();
            validateDoctorMajorCodeConsistency();
            validateDoctorMasterScheduleFiles();
            validateAppointmentFiles();
            validateCredentialAccountTypeConsistency();

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
            if (!header.equals("[환자 번호] [아이디] [환자 이름] [생년월일] [전화번호] [노쇼 횟수]")) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            // 데이터 행 검증
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty())
                    continue;

                String[] parts = line.split("\\s+");
                if (parts.length != 6) {
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

                // 노쇼 횟수 형식 검증 (정수)
                if (!parts[5].matches("^\\d+$")) {
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
            if (!header.equals("[의사번호] [의사이름] [진료과 코드] [전화번호] [등록일]")) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            // 데이터 행 검증
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty())
                    continue;

                String[] parts = line.split("\\s+");
                if (parts.length != 5) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // 의사번호 형식 검증
                if (!DOCTOR_ID_PATTERN.matcher(parts[0]).matches()) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // 전화번호 형식 검증
                if (!PHONE_PATTERN.matcher(parts[3]).matches()) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // 등록일 형식 검증
                if (!DATE_PATTERN.matcher(parts[4]).matches()) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }
            }
        } catch (IOException e) {
            throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
        }
    }

    private void validateVirtualTimeFile() {
        String filePath = "data/time/virtualtime.txt";
        try {
            List<String> lines = FileUtil.readLines(filePath);

            // --------------------------------------------
            // ★ 기획서 4.6.1: 파일이 없거나 비어있으면 기본값 생성이 정상 동작
            // --------------------------------------------
            if (lines.isEmpty()) {
                // 기본 BASE_TIME 파일 생성하도록 VirtualTime.load() 로 위임
                util.file.VirtualTime.setTime(LocalDateTime.of(2025, 10, 1, 9, 0, 0));
                return; // 검증 통과
            }

            // BASE_TIME 한 줄만 존재해야 함
            if (lines.size() != 1 || !lines.get(0).startsWith("BASE_TIME=")) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            // 값 검증
            String[] parts = lines.get(0).split("=");
            if (parts.length != 2) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            String value = parts[1].trim();
            Pattern datetimePattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$");
            if (!datetimePattern.matcher(value).matches()) {
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
                if (line.isEmpty())
                    continue;

                String[] parts = line.split("\\s+");
                if (parts.length != 4) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // 계정 타입 검증
                if (!parts[2].equals("PATIENT") && !parts[2].equals("DOCTOR") && !parts[2].equals("ADMIN")) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // 식별번호 형식 검증
                if (parts[2].equals("PATIENT")) {
                    if (!PATIENT_ID_PATTERN.matcher(parts[3]).matches()) {
                        throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                    }
                } else if (parts[2].equals("DOCTOR")) {
                    if (!DOCTOR_ID_PATTERN.matcher(parts[3]).matches()) {
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
                if (line.isEmpty())
                    continue;

                String[] parts = line.split("\\s+");
                if (parts.length > 0) {
                    String patientId = parts[0];
                    String detailFilePath = "data/patient/" + patientId + ".txt";

                    if (!FileUtil.resourceExists(detailFilePath)) {
                        throw new FileFormatException(
                                "[오류] '/data/patient/patientlist.txt'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
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

            // 1행: 환자 기본 정보 (환자번호 이름 생년월일 전화번호 노쇼횟수)
            String[] basicInfo = lines.get(0).trim().split("\\s+");
            if (basicInfo.length != 5) {
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
            // 노쇼 횟수 형식 검증 (음이 아닌 정수)
            if (!basicInfo[4].matches("^\\d+$")) {
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
                if (line.isEmpty())
                    continue;

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

                // 진료과 검증 (형식만 확인, 실제 존재 여부는 validateDoctorMajorCodeConsistency에서 확인)
                if (!MAJOR_CODE_PATTERN.matcher(parts[4]).matches()) {
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
                if (line.isEmpty())
                    continue;

                String[] parts = line.split("\\s+");
                if (parts.length > 0) {
                    String doctorId = parts[0];
                    String detailFilePath = "data/doctor/" + doctorId + ".txt";

                    if (!FileUtil.resourceExists(detailFilePath)) {
                        throw new FileFormatException(
                                "[오류] '/data/doctor/doctorlist.txt'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
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
                throw new FileFormatException("[오류] /" + filePath + "의 스케줄 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            // 1행: 의사 정보 (doctorlist.txt와 동일한 형식)
            String[] doctorInfo = lines.get(0).trim().split("\\s+");
            if (doctorInfo.length != 5) {
                throw new FileFormatException("[오류] /" + filePath + "의 스케줄 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }
            if (!DOCTOR_ID_PATTERN.matcher(doctorInfo[0]).matches()) {
                throw new FileFormatException("[오류] /" + filePath + "의 스케줄 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }
            if (!PHONE_PATTERN.matcher(doctorInfo[3]).matches()) {
                throw new FileFormatException("[오류] /" + filePath + "의 스케줄 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }
            if (!DATE_PATTERN.matcher(doctorInfo[4]).matches()) {
                throw new FileFormatException("[오류] /" + filePath + "의 스케줄 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            // 2행: 요일별 진료 여부 (5개 값: 월화수목금)
            String[] weekdaySchedule = lines.get(1).trim().split("\\s+");
            if (weekdaySchedule.length != 5) {
                throw new FileFormatException("[오류] /" + filePath + "의 스케줄 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }
            for (String day : weekdaySchedule) {
                if (!day.matches("^[01]$")) {
                    throw new FileFormatException("[오류] /" + filePath + "의 스케줄 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }
            }

            // 3행: 빈 행
            if (!lines.get(2).trim().isEmpty()) {
                throw new FileFormatException("[오류] /" + filePath + "의 스케줄 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            // 4행부터: 날짜별 예약 정보 (날짜 + 54개 슬롯)
            for (int i = 3; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty())
                    continue;

                String[] parts = line.split("\\s+");
                if (parts.length != 55) {
                    throw new FileFormatException("[오류] /" + filePath + "의 스케줄 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // 날짜 형식 검증
                if (!DATE_PATTERN.matcher(parts[0]).matches()) {
                    throw new FileFormatException("[오류] /" + filePath + "의 스케줄 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // 슬롯 값 검증 (0 또는 R로 시작하는 예약번호)
                for (int j = 1; j < parts.length; j++) {
                    if (!parts[j].equals("0") && !RESERVATION_ID_PATTERN.matcher(parts[j]).matches()) {
                        throw new FileFormatException("[오류] /" + filePath + "의 스케줄 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                    }
                }
            }
        } catch (IOException e) {
            throw new FileFormatException("[오류] /" + filePath + "의 스케줄 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
        } catch (NumberFormatException e) {
            throw new FileFormatException("[오류] /" + filePath + "의 스케줄 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
        }
    }

    private void validateAppointmentFiles() {
        try {
            Path appointmentDir = FileUtil.getResourcePath("data/appointment");

            if (!Files.exists(appointmentDir)) {
                // 예약 디렉토리가 없으면 건너뜀 (선택적 기능)
                return;
            }

            AppointmentRepository repository = new AppointmentRepository();

            try (Stream<Path> files = Files.list(appointmentDir)) {
                files.filter(path -> path.toString().endsWith(".txt"))
                        .filter(path -> !path.getFileName().toString().contains("backup"))
                        .forEach(path -> {
                            String fileName = path.getFileName().toString();
                            // YYYYMMDD.txt 형식 파일만 검증
                            if (fileName.matches("\\d{8}\\.txt")) {
                                try {
                                    // 파일명에서 날짜 추출
                                    String dateStr = fileName.replace(".txt", "");
                                    int year = Integer.parseInt(dateStr.substring(0, 4));
                                    int month = Integer.parseInt(dateStr.substring(4, 6));
                                    int day = Integer.parseInt(dateStr.substring(6, 8));
                                    LocalDate date = LocalDate.of(year, month, day);

                                    // AppointmentRepository를 통해 검증
                                    repository.getAppointmentsByDate(date);

                                } catch (AppointmentFileException e) {
                                    String filePath = "data/appointment/" + fileName;
                                    String errorMsg = String.format("[오류] '/%s'의 형식이 올바르지 않습니다. %s 프로그램을 종료합니다.",
                                            filePath, e.getMessage());
                                    throw new FileFormatException(errorMsg);
                                } catch (Exception e) {
                                    String filePath = "data/appointment/" + fileName;
                                    throw new FileFormatException(
                                            "[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                                }
                            }
                        });
            }
        } catch (IOException e) {
            throw new FileFormatException("[오류] 예약 파일 검증 중 오류가 발생했습니다. 프로그램을 종료합니다.");
        }
    }

    private void validateMajorListFile() {
        String filePath = "data/major/majorlist.txt";
        Pattern majorCodePattern = Pattern.compile("^[A-Z]{2,6}$");

        try {
            List<String> lines = FileUtil.readLines(filePath);

            if (lines.isEmpty()) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            // 헤더 검증
            String header = lines.get(0).trim();
            if (!header.equals("[진료과코드] [진료과명]")) {
                throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            // 데이터 행 검증
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty())
                    continue;

                String[] parts = line.split("\\s+", 2);
                if (parts.length != 2) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // 진료과 코드 형식 검증 (대문자 로마자 2-4자)
                if (!majorCodePattern.matcher(parts[0]).matches()) {
                    throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }
            }
        } catch (IOException e) {
            throw new FileFormatException("[오류] '/" + filePath + "'의 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
        }
    }

    private void validateDoctorMajorCodeConsistency() {
        try {
            // doctorlist.txt에서 의사들의 진료과 코드 검증
            List<String> doctorLines = FileUtil.readLines("data/doctor/doctorlist.txt");

            for (int i = 1; i < doctorLines.size(); i++) {
                String line = doctorLines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    String majorCode = parts[2];

                    // 진료과 코드가 majorlist.txt에 존재하는지 확인
                    if (!majorRepository.isMajorExists(majorCode)) {
                        throw new FileFormatException(
                                "[오류] 진료과 코드 불일치: " + majorCode + "은(는) '/data/major/majorlist.txt'에 존재하지 않습니다. 프로그램을 종료합니다.");
                    }
                }
            }
        } catch (IOException e) {
            throw new FileFormatException("[오류] 진료과 코드 불일치 검증 중 오류가 발생했습니다. 프로그램을 종료합니다.");
        }
    }

    private void validateDoctorMasterScheduleFiles() {
        try {
            List<String> doctorLines = FileUtil.readLines("data/doctor/doctorlist.txt");

            for (int i = 1; i < doctorLines.size(); i++) {
                String line = doctorLines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length > 0) {
                    String doctorId = parts[0];
                    String masterFilePath = "data/doctor/" + doctorId + "-master.txt";

                    // Check if master file exists
                    if (!FileUtil.resourceExists(masterFilePath)) {
                        continue; // Master file is optional
                    }

                    // Validate master file format
                    validateDoctorMasterScheduleFile(doctorId);
                }
            }
        } catch (IOException e) {
            throw new FileFormatException("[오류] 의사 마스터 스케줄 파일 검증 중 오류가 발생했습니다. 프로그램을 종료합니다.");
        }
    }

    private void validateDoctorMasterScheduleFile(String doctorId) {
        String filePath = "data/doctor/" + doctorId + "-master.txt";

        try {
            List<String> lines = FileUtil.readLines(filePath);

            if (lines.isEmpty()) {
                throw new FileFormatException("[오류] /" + filePath + "의 스케줄 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

            // Expected format: 7 lines (MON to SUN)
            String[] expectedDays = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
            int dayIndex = 0;
            int validLineCount = 0;

            for (String line : lines) {
                String trimmed = line.trim();

                // Skip empty lines
                if (trimmed.isEmpty()) {
                    continue;
                }

                validLineCount++;

                String[] parts = trimmed.split("\\s+");

                // Must be exactly 3 parts: [DAY] [START_TIME] [END_TIME]
                if (parts.length != 3) {
                    throw new FileFormatException("[오류] /" + filePath + "의 스케줄 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                String day = parts[0];
                String startTime = parts[1];
                String endTime = parts[2];

                // Validate day of week
                if (!DAY_OF_WEEK_PATTERN.matcher(day).matches()) {
                    throw new FileFormatException("[오류] /" + filePath + "의 스케줄 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // Check if "0 0" (no schedule for this day)
                if (startTime.equals("0") && endTime.equals("0")) {
                    continue;
                }

                // Validate time format (HH:MM)
                if (!TIME_PATTERN.matcher(startTime).matches() || !TIME_PATTERN.matcher(endTime).matches()) {
                    throw new FileFormatException("[오류] /" + filePath + "의 스케줄 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // Validate time values
                String[] startParts = startTime.split(":");
                String[] endParts = endTime.split(":");

                int startHour = Integer.parseInt(startParts[0]);
                int startMinute = Integer.parseInt(startParts[1]);
                int endHour = Integer.parseInt(endParts[0]);
                int endMinute = Integer.parseInt(endParts[1]);

                if (startHour < 0 || startHour > 23 || startMinute < 0 || startMinute > 59 ||
                        endHour < 0 || endHour > 23 || endMinute < 0 || endMinute > 59) {
                    throw new FileFormatException("[오류] /" + filePath + "의 스케줄 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }

                // Start time must be before end time
                int startTotalMinutes = startHour * 60 + startMinute;
                int endTotalMinutes = endHour * 60 + endMinute;

                if (startTotalMinutes >= endTotalMinutes) {
                    throw new FileFormatException("[오류] /" + filePath + "의 스케줄 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
                }
            }

            // Must have exactly 7 valid lines (one for each day of week)
            if (validLineCount != 7) {
                throw new FileFormatException("[오류] /" + filePath + "의 스케줄 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
            }

        } catch (IOException e) {
            throw new FileFormatException("[오류] /" + filePath + "의 스케줄 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
        } catch (NumberFormatException e) {
            throw new FileFormatException("[오류] /" + filePath + "의 스케줄 형식이 올바르지 않습니다. 프로그램을 종료합니다.");
        }
    }

    private void validateCredentialAccountTypeConsistency() {
        try {
            // patientlist.txt에서 환자 ID 목록 읽기
            List<String> patientLines = FileUtil.readLines("data/patient/patientlist.txt");
            java.util.Set<String> patientIds = new java.util.HashSet<>();
            java.util.Map<String, String> patientIdToUsername = new java.util.HashMap<>();

            for (int i = 1; i < patientLines.size(); i++) {
                String line = patientLines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    patientIds.add(parts[0]); // 환자번호
                    patientIdToUsername.put(parts[0], parts[1]); // 환자번호 -> 아이디
                }
            }

            // doctorlist.txt에서 의사 ID 목록 읽기
            List<String> doctorLines = FileUtil.readLines("data/doctor/doctorlist.txt");
            java.util.Set<String> doctorIds = new java.util.HashSet<>();
            java.util.Map<String, String> doctorIdToUsername = new java.util.HashMap<>();

            for (int i = 1; i < doctorLines.size(); i++) {
                String line = doctorLines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length >= 1) {
                    doctorIds.add(parts[0]); // 의사번호
                }
            }

            // credentials.txt에서 계정 정보 읽기 및 검증
            List<String> credentialLines = FileUtil.readLines("data/auth/credentials.txt");

            for (int i = 1; i < credentialLines.size(); i++) {
                String line = credentialLines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length >= 4) {
                    String username = parts[0];
                    String accountType = parts[2];
                    String identifier = parts[3];

                    // PATIENT 타입인데 환자 목록에 없는 경우
                    if (accountType.equals("PATIENT")) {
                        if (!patientIds.contains(identifier)) {
                            throw new FileFormatException(
                                    "[오류] 계정 타입 불일치: " + username + "은 PATIENT로 등록되어 있으나 환자 목록에 존재하지 않습니다. 프로그램을 종료합니다."
                            );
                        }
                    }
                    // DOCTOR 타입인데 의사 목록에 없는 경우
                    else if (accountType.equals("DOCTOR")) {
                        if (!doctorIds.contains(identifier)) {
                            throw new FileFormatException(
                                    "[오류] 계정 타입 불일치: " + username + "은 DOCTOR로 등록되어 있으나 의사 목록에 존재하지 않습니다. 프로그램을 종료합니다."
                            );
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new FileFormatException("[오류] 계정 타입 불일치 검증 중 오류가 발생했습니다. 프로그램을 종료합니다.");
        }
    }
}
