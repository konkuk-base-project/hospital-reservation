package service.doctor;

import service.Command;
import util.exception.DoctorScheduleException;

public class SetScheduleCommand implements Command {
    private final DoctorService doctorService;

    public SetScheduleCommand(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @Override
    public void execute(String[] args) {
        try {
            doctorService.setSchedule(args);
        } catch (DoctorScheduleException e) {
            System.out.println("[오류] " + e.getMessage());
        }
    }
}