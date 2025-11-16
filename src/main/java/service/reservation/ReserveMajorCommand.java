package service.reservation;

import service.Command;
import util.exception.ReservationException;

public class ReserveMajorCommand implements Command {
    private final ReservationService reservationService;

    public ReserveMajorCommand(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Override
    public void execute(String[] args) {
        try {
            reservationService.reserveMajor(args);
        } catch (ReservationException e) {
            System.out.println("[오류] " + e.getMessage());
        }
    }
}
