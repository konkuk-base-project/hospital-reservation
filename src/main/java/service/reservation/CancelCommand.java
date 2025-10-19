// src/main/java/service/reservation/CancelCommand.java
package service.reservation;

import service.Command;
import util.exception.ReservationException;

public class CancelCommand implements Command {
    private final ReservationService reservationService;

    public CancelCommand(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Override
    public void execute(String[] args) {
        try {
            reservationService.cancelReservation(args);
        } catch (ReservationException e) {
            System.out.println("[오류] " + e.getMessage());
        }
    }
}