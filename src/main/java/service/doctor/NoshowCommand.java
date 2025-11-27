package service.doctor;

import service.Command;
import util.exception.DoctorScheduleException;

public class NoshowCommand implements Command {
    private final DoctorService doctorService;

    public NoshowCommand(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @Override
    public void execute(String[] args) {
        try {
            doctorService.noshowAppointment(args);
        } catch (DoctorScheduleException e) {
            System.out.println("[오류] " + e.getMessage());
        }
    }
}