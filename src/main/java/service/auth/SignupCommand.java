package service.auth;

import service.Command;
import util.exception.SignupException;

public class SignupCommand implements Command {
    private final AuthService authService;

    public SignupCommand(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void execute(String[] args) {
        try {
            String result = authService.signup(args);
            System.out.println(result);
        } catch (SignupException e) {
            System.out.println("[오류] " + e.getMessage());
        }
    }
}