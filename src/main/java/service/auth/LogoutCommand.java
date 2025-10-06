package service.auth;

import service.Command;
import util.exception.LoginException;

public class LogoutCommand implements Command {
    private final AuthService authService;

    public LogoutCommand(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void execute(String[] args) {
        try {
            authService.logout(args);
            System.out.println("로그아웃 되었습니다. 로그인 화면으로 돌아갑니다.");
        } catch (LoginException e) {
            System.out.println("[오류] " + e.getMessage());
        }
    }
}