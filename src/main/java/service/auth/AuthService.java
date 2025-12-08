package service.auth;

import java.io.IOException;
import java.nio.file.Files; // 추가
import java.nio.file.Path; // 추가
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream; // 추가

import model.Doctor;
import model.Patient;
import model.User;
import repository.AuthRepository;
import repository.DoctorRepository;
import repository.PatientRepository;
import repository.AppointmentRepository;
import service.AuthContext;
import util.exception.LoginException;
import util.exception.SignupException;
import util.file.FileUtil;
import util.file.VirtualTime;
import util.hash.PasswordHasher;

public class AuthService {
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AuthRepository authRepository;
    private final AppointmentRepository appointmentRepository;
    private final AuthContext authContext;

    public AuthService(PatientRepository patientRepository, DoctorRepository doctorRepository,
            AuthRepository authRepository, AppointmentRepository appointmentRepository, AuthContext authContext) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.authRepository = authRepository;
        this.appointmentRepository = appointmentRepository;
        this.authContext = authContext;
    }

    /**
     * 6.1.1 환자 회원가입
     */
    public String signup(String[] args) throws SignupException {
        if (args.length != 6) {
            throw new SignupException("인자의 개수가 올바르지 않습니다. (형식: signup <아이디> <비밀번호> <비밀번호확인> <이름> <생년월일> <전화번호>)");
        }

        String username = args[0];
        String password = args[1];
        String passwordConfirm = args[2];
        String name = args[3];
        String birthDateStr = args[4];
        String phoneNumber = args[5];

        validateUsername(username);
        validatePassword(password, passwordConfirm);
        validateName(name);
        validateBirthDate(birthDateStr);
        validatePhoneNumber(phoneNumber);

        try {
            String newPatientId = patientRepository.getNextPatientId();
            Patient newPatient = new Patient(newPatientId, username, name, birthDateStr, phoneNumber);
            patientRepository.save(newPatient);

            String hashedPassword = PasswordHasher.hash(password);
            User newUser = new User(username, hashedPassword, "PATIENT", newPatientId);
            authRepository.save(newUser);

            return "회원가입이 완료되었습니다. [환자번호: " + newPatientId + "]";
        } catch (IOException e) {
            throw new SignupException("회원 정보를 파일에 저장하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 6.1.2 의사 회원가입
     */
    public String signupDoctor(String[] args) throws SignupException {
        if (args.length != 6) {
            throw new SignupException(
                    "인자의 개수가 올바르지 않습니다. (형식: signup-doctor <아이디> <비밀번호> <비밀번호확인> <이름> <진료과코드> <전화번호>)");
        }

        String username = args[0];
        String password = args[1];
        String passwordConfirm = args[2];
        String name = args[3];
        String deptCode = args[4].toUpperCase();
        String phoneNumber = args[5];

        validateUsername(username);
        validatePassword(password, passwordConfirm);
        validateName(name);
        validateDepartmentCode(deptCode);
        validatePhoneNumber(phoneNumber);

        try {
            String newDoctorId = doctorRepository.getNextDoctorId();

            String registrationDate = LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            Doctor newDoctor = new Doctor(
                    newDoctorId,
                    username,
                    name,
                    deptCode,
                    phoneNumber,
                    registrationDate);

            doctorRepository.save(newDoctor);

            String hashedPassword = PasswordHasher.hash(password);
            User newUser = new User(username, hashedPassword, "DOCTOR", newDoctorId);
            authRepository.save(newUser);

            // 모든 기존 예약 파일에 새 의사 열 추가
            updateAllAppointmentFilesWithNewDoctor(newDoctorId);

            return "의사 회원가입이 완료되었습니다. [의사번호: " + newDoctorId + "]";
        } catch (IOException e) {
            throw new SignupException("회원 정보를 파일에 저장하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private void updateAllAppointmentFilesWithNewDoctor(String newDoctorId) throws IOException {
        Path appointmentDir = FileUtil.getResourcePath("data/appointment");

        if (!Files.exists(appointmentDir)) {
            return; // 예약 파일 없으면 스킵
        }

        try (Stream<Path> files = Files.list(appointmentDir)) {
            files.filter(path -> path.toString().endsWith(".txt"))
                    .filter(path -> path.getFileName().toString().matches("\\d{8}\\.txt"))
                    .forEach(path -> {
                        try {
                            List<String> lines = Files.readAllLines(path);
                            List<String> updatedLines = new ArrayList<>();

                            for (int i = 0; i < lines.size(); i++) {
                                String line = lines.get(i);

                                if (i == 0) {
                                    // 1행: 날짜 - 그대로
                                    updatedLines.add(line);
                                } else if (i == 1) {
                                    // 2행: 헤더 - 새 의사 추가
                                    updatedLines.add(line + " " + newDoctorId);
                                } else {
                                    // 3행~: 시간 슬롯 - 0 추가
                                    updatedLines.add(line + " 0");
                                }
                            }

                            Files.write(path, updatedLines);
                        } catch (IOException e) {
                            throw new RuntimeException("예약 파일 업데이트 실패: " + path, e);
                        }
                    });
        }
    }

    /**
     * 6.1.3 로그인
     */
    public void login(String[] args) throws LoginException {
        if (authContext.isLoggedIn()) {
            throw new LoginException("이미 로그인되어 있습니다.");
        }
        if (args.length != 2) {
            throw new LoginException("인자의 개수가 올바르지 않습니다. (형식: login <아이디> <비밀번호>)");
        }

        String username = args[0];
        String password = args[1];

        // 관리자 로그인 처리 (login 000 000)
        if (username.equals("000") && password.equals("000")) {
            User admin = new User("000", PasswordHasher.hash("000"), "ADMIN", "A000000");
            authContext.login(admin);

            // 콘솔 잔여 문자 강제 제거 (Gradle run 입력 버퍼 대응)
            try {
                System.in.skip(System.in.available());
            } catch (IOException ignored) {}

            System.out.flush();
            return;
        }

        User user = authRepository.findByUsername(username)
                .orElseThrow(() -> new LoginException("존재하지 않는 아이디입니다."));

        String hashedInputPassword = PasswordHasher.hash(password);
        if (!user.getHashedPassword().equals(hashedInputPassword)) {
            throw new LoginException("비밀번호가 일치하지 않습니다.");
        }
        authContext.login(user);
    }

    /**
     * 6.1.4 로그아웃
     */
    public void logout(String[] args) throws LoginException {
        if (!authContext.isLoggedIn()) {
            throw new LoginException("현재 로그인된 사용자가 없습니다.");
        }
        if (args.length > 0) {
            throw new LoginException("불필요한 인자가 입력되었습니다. (형식: logout)");
        }
        authContext.logout();
    }

    /**
     * 6.1.5 회원 탈퇴
     */
    public List<String> getFutureReservations(String password) throws LoginException {
        if (!authContext.isLoggedIn()) {
            throw new LoginException("로그인이 필요한 서비스입니다.");
        }
        User user = authContext.getCurrentUser();

        if (!user.getRole().equals("PATIENT")) {
            throw new LoginException("환자만 탈퇴할 수 있습니다.");
        }

        String hashedInputPassword = PasswordHasher.hash(password);
        if (!user.getHashedPassword().equals(hashedInputPassword)) {
            throw new LoginException("비밀번호가 일치하지 않습니다.");
        }

        List<String> allReservations = patientRepository.getPatientReservations(user.getId());
        List<String> futureReservations = new ArrayList<>();

        LocalDate today = VirtualTime.currentDate();
        LocalTime now = VirtualTime.currentDateTime().toLocalTime();

        for (String res : allReservations) {
            String[] parts = res.split("\\s+");
            if (parts.length < 7)
                continue;

            // 예약 상태가 취소(3)인 경우 제외
            if (parts[6].contains("(3)"))
                continue;

            LocalDate resDate = LocalDate.parse(parts[1]);
            LocalTime resTime = LocalTime.parse(parts[2]);

            if (resDate.isAfter(today) || (resDate.equals(today) && resTime.isAfter(now))) {
                futureReservations.add(res);
            }
        }

        return futureReservations;
    }

    public void withdraw(String password) throws LoginException, IOException {
        // 1. 검증 및 미래 예약 확인
        List<String> futureReservations = getFutureReservations(password);
        User user = authContext.getCurrentUser();

        // 2. 미래 예약 취소
        try {
            for (String res : futureReservations) {
                String[] parts = res.split("\\s+");
                String resId = parts[0];
                LocalDate resDate = LocalDate.parse(parts[1]);

                String resTime = parts[2];
                String doctorId = parts[5];

                // AppointmentRepository를 통해 예약 삭제 (상태 0으로 초기화)
                appointmentRepository.deleteAppointment(resDate, resId);

                // DoctorRepository를 통해 의사 스케줄 업데이트 (상태 0으로 초기화)
                doctorRepository.updateSchedule(doctorId, resDate, resTime, "0");
            }
        } catch (Exception e) {
            System.out.println("[경고] 예약 취소 중 오류 발생: " + e.getMessage());
        }

        // 3. 환자 정보 삭제
        patientRepository.delete(user.getId());

        // 4. 인증 정보 삭제
        authRepository.delete(user.getUsername());

        // 5. 로그아웃
        authContext.logout();
    }

    // ========== 검증 메서드들 ==========

    private void validateUsername(String username) throws SignupException {
        if (username.length() < 4 || username.length() > 20) {
            throw new SignupException("아이디는 최소 4자 이상, 최대 20자 이내여야 합니다.");
        }
        if (!Pattern.matches("^[a-zA-Z0-9]+$", username)) {
            throw new SignupException("아이디는 영문, 숫자 조합이어야 합니다.");
        }
        if (authRepository.findByUsername(username).isPresent()) {
            throw new SignupException("이미 사용 중인 아이디입니다.");
        }
    }

    private void validatePassword(String password, String passwordConfirm) throws SignupException {
        if (!password.equals(passwordConfirm)) {
            throw new SignupException("비밀번호가 일치하지 않습니다.");
        }
        if (password.length() < 8 || !Pattern.matches("^(?=.*[A-Za-z])(?=.*\\d).+$", password)) {
            throw new SignupException("비밀번호는 최소 8자 이상, 영문+숫자를 포함해야 합니다.");
        }
    }

    private void validateName(String name) throws SignupException {
        if (name.length() < 2 || name.length() > 20) {
            throw new SignupException("이름은 최소 2자 이상 입력해야 합니다.");
        }
        if (!Pattern.matches("^[a-zA-Z가-힣]+$", name)) {
            throw new SignupException("이름은 한글(완성형) 또는 로마자(a-z, A-Z)로만 구성되며 공백은 허용되지 않습니다.");
        }
    }

    private void validateBirthDate(String birthDateStr) throws SignupException {
        try {
            LocalDate birthDate = LocalDate.parse(birthDateStr);
            LocalDate baseDate = VirtualTime.currentDate();
            if (birthDate.isBefore(LocalDate.of(1900, 1, 1)) || birthDate.isAfter(baseDate)) {
                throw new SignupException("생년월일이 유효하지 않습니다. (1900-01-01부터 " + baseDate + "까지 유효)");
            }
        } catch (DateTimeParseException e) {
            throw new SignupException("생년월일 형식이 잘못되었습니다. (예: YYYY-MM-DD)");
        }
    }

    private void validatePhoneNumber(String phoneNumber) throws SignupException {
        if (!Pattern.matches("^010-\\d{4}-\\d{4}$", phoneNumber)) {
            throw new SignupException("전화번호 형식이 잘못되었습니다. (예: 010-1234-5678).");
        }
    }

    private void validateDepartmentCode(String deptCode) throws SignupException {
        try {
            List<String> lines = FileUtil.readLines("data/major/majorlist.txt");
            boolean found = false;
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty())
                    continue;
                String[] parts = line.split("\\s+");
                if (parts.length >= 1 && parts[0].equals(deptCode)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new SignupException("존재하지 않는 진료과입니다.");
            }
        } catch (IOException e) {
            throw new SignupException("진료과 목록을 확인하는 중 오류가 발생했습니다.");
        }
    }
}