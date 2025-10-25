package service.auth;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

import model.Patient;
import model.User;
import repository.AuthRepository;
import repository.PatientRepository;
import service.AuthContext;
import util.exception.LoginException;
import util.exception.SignupException;
import util.hash.PasswordHasher;

public class AuthService {
    private final PatientRepository patientRepository;
    private final AuthRepository authRepository;
    private final AuthContext authContext;

    public AuthService(PatientRepository patientRepository, AuthRepository authRepository, AuthContext authContext) {
        this.patientRepository = patientRepository;
        this.authRepository = authRepository;
        this.authContext = authContext;
    }

    public String signup(String[] args) throws SignupException {
        if (args.length != 6) throw new SignupException("인자가 부족합니다. (형식: signup <아이디> <비밀번호> <비밀번호확인> <이름> <생년월일> <전화번호>)");

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
            User newUser = new User(username, hashedPassword, "USER", newPatientId);
            authRepository.save(newUser);

            return "회원가입이 완료되었습니다. [환자번호: " + newPatientId + "] 로그인 화면으로 이동합니다.";
        } catch (IOException e) {
            throw new SignupException("회원 정보를 파일에 저장하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public void login(String[] args) throws LoginException {
        if (authContext.isLoggedIn()) throw new LoginException("이미 로그인되어 있습니다.");
        if (args.length != 2) throw new LoginException("인자가 부족합니다. (형식: login <아이디> <비밀번호>)");

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

        // 로그인 메시지 출력
        System.out.println("관리자(Admin)로 로그인되었습니다.");
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

    public void logout(String[] args) throws LoginException {
        if (!authContext.isLoggedIn()) throw new LoginException("현재 로그인된 사용자가 없습니다.");
        if (args.length > 0) throw new LoginException("불필요한 인자가 입력되었습니다. (형식: logout)");
        authContext.logout();
    }

    private void validateUsername(String username) throws SignupException {
        if (username.length() < 4 || username.length() > 20) throw new SignupException("아이디는 최소 4자 이상, 최대 20자 이내여야 합니다.");
        if (!Pattern.matches("^[a-zA-Z0-9]+$", username)) throw new SignupException("아이디는 영문, 숫자 조합이어야 합니다.");
        if (authRepository.findByUsername(username).isPresent()) throw new SignupException("이미 사용 중인 아이디입니다.");
    }

    private void validatePassword(String password, String passwordConfirm) throws SignupException {
        if (!password.equals(passwordConfirm)) throw new SignupException("비밀번호가 일치하지 않습니다.");
        if (password.length() < 8 || !Pattern.matches("^(?=.*[A-Za-z])(?=.*\\d).+$", password)) throw new SignupException("비밀번호는 최소 8자 이상, 영문+숫자를 포함해야 합니다.");
    }

    private void validateName(String name) throws SignupException {
        if (name.length() < 2 || name.length() > 20) throw new SignupException("이름은 길이가 2 이상 20 이하입니다.");
        if (!Pattern.matches("^[a-zA-Z가-힣]+$", name)) throw new SignupException("이름은 한글(완성형) 또는 로마자(a-z, A-Z)로만 구성되며 공백은 허용되지 않습니다.");
    }

    private void validateBirthDate(String birthDateStr) throws SignupException {
        try {
            LocalDate birthDate = LocalDate.parse(birthDateStr);
            if (birthDate.isBefore(LocalDate.of(1900, 1, 1)) || birthDate.isAfter(LocalDate.now())) {
                throw new SignupException("생년월일이 유효하지 않습니다. (1900-01-01부터 현재 날짜까지만 유효)");
            }
        } catch (DateTimeParseException e) {
            throw new SignupException("생년월일 형식이 잘못되었습니다. (예: YYYY-MM-DD)");
        }
    }

    private void validatePhoneNumber(String phoneNumber) throws SignupException {
        if (!Pattern.matches("^010-\\d{4}-\\d{4}$", phoneNumber)) throw new SignupException("전화번호 형식이 잘못되었습니다. (예: 010-1234-5678).");
    }
}