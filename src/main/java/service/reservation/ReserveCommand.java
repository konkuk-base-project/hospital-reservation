// src/main/java/service/reservation/ReserveCommand.java
package service.reservation;

import service.Command;
import util.exception.ReservationException;

public class ReserveCommand implements Command {
    private final ReservationService reservationService;

    public ReserveCommand(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Override
    public void execute(String[] args) {
        try {
            reservationService.createReservation(args);
        } catch (ReservationException e) {
            System.out.println("[오류] " + e.getMessage());
        }
    }
}