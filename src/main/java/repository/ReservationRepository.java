package repository;

import util.file.FileUtil;
import java.io.IOException;
import java.util.List;

public class ReservationRepository {
    private static final String RESERVATION_COUNTER_FILE = "data/reservation/counter.txt";
    private int lastReservationNumber = 0;

    public ReservationRepository() {
        loadLastReservationNumber();
    }

    /**
     * 마지막 예약번호를 파일에서 로드
     */
    private void loadLastReservationNumber() {
        try {
            if (FileUtil.resourceExists(RESERVATION_COUNTER_FILE)) {
                List<String> lines = FileUtil.readLines(RESERVATION_COUNTER_FILE);
                if (!lines.isEmpty()) {
                    String lastId = lines.get(0).trim();
                    if (lastId.startsWith("R")) {
                        lastReservationNumber = Integer.parseInt(lastId.substring(1));
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            lastReservationNumber = 0;
        }
    }

    /**
     * 새로운 예약번호 생성 및 저장
     */
    public String getNextReservationId() throws IOException {
        lastReservationNumber++;
        String newId = String.format("R%08d", lastReservationNumber);

        // 파일에 저장
        FileUtil.createDirectoriesAndWrite(
                FileUtil.getResourcePath(RESERVATION_COUNTER_FILE),
                List.of(newId)
        );

        return newId;
    }
}