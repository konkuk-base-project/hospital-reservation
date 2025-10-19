// src/main/java/service/reservation/ModifyCommand.java
package service.reservation;

import service.Command;
import util.exception.ReservationException;

public class ModifyCommand implements Command {
    private final ReservationService reservationService;

    public ModifyCommand(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Override
    public void execute(String[] args) {
        try {
            reservationService.modifyReservation(args);
        } catch (ReservationException e) {
            System.out.println("[오류] " + e.getMessage());
        }
    }
}