package service.doctor;

import service.Command;
import util.exception.DoctorScheduleException;

public class ViewScheduleCommand implements Command {
    private final DoctorService doctorService;

    public ViewScheduleCommand(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @Override
    public void execute(String[] args) {
        try {
            doctorService.viewSchedule(args);
        } catch (DoctorScheduleException e) {
            System.out.println("[오류] " + e.getMessage());
        }
    }
}