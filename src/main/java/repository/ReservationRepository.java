package repository;

import util.file.FileUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class ReservationRepository {
    private static final String PATIENT_DIR = "data/patient/";
    private int lastReservationNumber = 0;

    public ReservationRepository() {
        loadLastReservationNumber();
    }

    /**
     * 모든 환자 파일을 검색하여 마지막 예약번호 찾기
     */
    private void loadLastReservationNumber() {
        try {
            Path patientPath = FileUtil.getResourcePath(PATIENT_DIR);

            if (!Files.exists(patientPath)) {
                lastReservationNumber = 0;
                return;
            }

            try (Stream<Path> files = Files.list(patientPath)) {
                lastReservationNumber = files
                        .filter(path -> path.getFileName().toString().matches("P\\d{6}\\.txt"))
                        .flatMap(path -> {
                            try {
                                String relativePath = PATIENT_DIR + path.getFileName().toString();
                                List<String> lines = FileUtil.readLines(relativePath);
                                return lines.stream();
                            } catch (IOException e) {
                                return Stream.empty();
                            }
                        })
                        .filter(line -> line.startsWith("R")) // 예약번호로 시작하는 줄
                        .map(line -> line.split("\\s+")[0]) // 첫 번째 컬럼 (예약번호)
                        .filter(id -> id.matches("R\\d{8}"))
                        .map(id -> id.substring(1)) // R 제거
                        .mapToInt(Integer::parseInt)
                        .max()
                        .orElse(0);
            }
        } catch (IOException | NumberFormatException e) {
            lastReservationNumber = 0;
        }
    }

    /**
     * 새로운 예약번호 생성
     */
    public String getNextReservationId() {
        lastReservationNumber++;
        return String.format("R%08d", lastReservationNumber);
    }
}