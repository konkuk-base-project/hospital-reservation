package service.auth;

import service.Command;
import util.exception.LoginException;

public class LoginCommand implements Command {
    private final AuthService authService;

    public LoginCommand(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void execute(String[] args) {
        try {
            authService.login(args);
            System.out.println("로그인 성공! 주 메뉴로 이동합니다.");
        } catch (LoginException e) {
            System.out.println("[오류] " + e.getMessage());
        }
    }
}
