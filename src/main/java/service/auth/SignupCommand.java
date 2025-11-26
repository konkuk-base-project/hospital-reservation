package service.auth;

import service.Command;
import util.exception.SignupException;

public class SignupCommand implements Command {
    private final AuthService authService;
    private final boolean isDoctor;

    public SignupCommand(AuthService authService) {
        this(authService, false);
    }

    public SignupCommand(AuthService authService, boolean isDoctor) {
        this.authService = authService;
        this.isDoctor = isDoctor;
    }

    @Override
    public void execute(String[] args) {
        try {
            String result = isDoctor ? authService.signupDoctor(args) : authService.signup(args);
            System.out.println(result);
        } catch (SignupException e) {
            System.out.println("[오류] " + e.getMessage());
        }
    }
}