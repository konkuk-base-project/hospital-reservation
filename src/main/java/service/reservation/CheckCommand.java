// src/main/java/service/reservation/CheckCommand.java
package service.reservation;

import service.Command;
import util.exception.ReservationException;

public class CheckCommand implements Command {
    private final ReservationService reservationService;

    public CheckCommand(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Override
    public void execute(String[] args) {
        try {
            reservationService.checkReservation(args);
        } catch (ReservationException e) {
            System.out.println("[오류] " + e.getMessage());
        }
    }
}