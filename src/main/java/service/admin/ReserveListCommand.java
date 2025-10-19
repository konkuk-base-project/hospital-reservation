package service.admin;

import service.Command;
import util.exception.SearchException;

public class ReserveListCommand implements Command {
    private final AdminService adminService;

    public ReserveListCommand(AdminService adminService) {
        this.adminService = adminService;
    }

    @Override
    public void execute(String[] args) {
        try {
            adminService.showReserveList(args);
        } catch (SearchException e) {
            System.out.println("[오류] " + e.getMessage());
        }
    }
}
