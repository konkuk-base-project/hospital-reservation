package service.admin;

import service.Command;
import util.exception.SearchException;

public class UserSearchCommand implements Command {
    private final AdminService adminService;

    public UserSearchCommand(AdminService adminService) {
        this.adminService = adminService;
    }

    @Override
    public void execute(String[] args) {
        try {
            adminService.searchUser(args);
        } catch (SearchException e) {
            System.out.println("[오류] " + e.getMessage());
        }
    }
}
