package service.doctor;

import service.Command;
import util.exception.DoctorScheduleException;

public class CompleteCommand implements Command {
    private final DoctorService doctorService;

    public CompleteCommand(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @Override
    public void execute(String[] args) {
        try {
            doctorService.completeAppointment(args);
        } catch (DoctorScheduleException e) {
            System.out.println("[오류] " + e.getMessage());
        }
    }
}