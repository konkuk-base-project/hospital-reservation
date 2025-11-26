package service.doctor;

import service.Command;
import util.exception.DoctorScheduleException;

public class ModifyScheduleCommand implements Command {
    private final DoctorService doctorService;

    public ModifyScheduleCommand(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @Override
    public void execute(String[] args) {
        try {
            doctorService.modifySchedule(args);
        } catch (DoctorScheduleException e) {
            System.out.println("[오류] " + e.getMessage());
        }
    }
}