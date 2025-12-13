package repository;

import util.file.FileUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class ReservationRepository {
    private static final String PATIENT_DIR = "data/patient/";
    private static int lastReservationNumber = 0;
    private static final Object lock = new Object();

    /**
     * 모든 환자 파일을 검색하여 마지막 예약번호 찾기
     */
    private int getCurrentMaxReservationNumber() {
        int maxReservationNumber = 0;

        try {
            Path patientPath = FileUtil.getResourcePath(PATIENT_DIR);

            if (!Files.exists(patientPath)) {
                return 0;
            }

            try (Stream<Path> files = Files.list(patientPath)) {
                maxReservationNumber = files
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
                        .filter(line -> line.trim().startsWith("R")) // 예약번호로 시작하는 줄
                        .map(line -> line.trim().split("\\s+")[0]) // 첫 번째 컬럼 (예약번호)
                        .filter(id -> id.matches("R\\d{8}"))
                        .map(id -> id.substring(1)) // R 제거
                        .mapToInt(Integer::parseInt)
                        .max()
                        .orElse(0);
            }
        } catch (IOException | NumberFormatException e) {
            // 오류 발생 시 현재까지의 최대값 반환
        }

        return maxReservationNumber;
    }

    /**
     * 새로운 예약번호 생성 (파일 최대값과 메모리 카운터 중 큰 값 사용)
     */
    public String getNextReservationId() {
        synchronized (lock) {
            // 파일에서 현재 최대 예약번호 조회
            int fileMax = getCurrentMaxReservationNumber();

            // 파일 최대값과 메모리 카운터 중 큰 값 선택
            lastReservationNumber = Math.max(lastReservationNumber, fileMax);

            // 다음 예약번호 생성
            lastReservationNumber++;

            return String.format("R%08d", lastReservationNumber);
        }
    }
}