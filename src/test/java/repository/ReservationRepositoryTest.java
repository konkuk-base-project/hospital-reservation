package repository;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ReservationRepositoryTest {

    @Test
    public void testNextReservationIdIsUnique() {
        ReservationRepository repo = new ReservationRepository();

        String firstId = repo.getNextReservationId();
        String secondId = repo.getNextReservationId();

        assertNotEquals(firstId, secondId, "예약번호는 중복되면 안됩니다");

        // 첫 번째 ID가 R00000057보다 크거나 같아야 함 (기존 데이터 기준)
        int firstNum = Integer.parseInt(firstId.substring(1));
        assertTrue(firstNum >= 57, "예약번호는 기존 최대값 이후부터 시작해야 합니다. 실제값: " + firstId);

        // 두 번째 ID는 첫 번째보다 1 커야 함
        int secondNum = Integer.parseInt(secondId.substring(1));
        assertEquals(firstNum + 1, secondNum, "예약번호는 순차적으로 증가해야 합니다");

        System.out.println("첫 번째 예약번호: " + firstId);
        System.out.println("두 번째 예약번호: " + secondId);
    }

    @Test
    public void testMultipleRepositoryInstances() throws Exception {
        ReservationRepository repo1 = new ReservationRepository();
        String id1 = repo1.getNextReservationId();

        // 첫 번째 예약을 테스트 환자 파일에 기록 (실제 시나리오 시뮬레이션)
        String testPatientFile = "data/patient/P000999.txt";
        java.nio.file.Path testFile = util.file.FileUtil.getResourcePath(testPatientFile);
        java.util.List<String> testLines = new java.util.ArrayList<>();
        testLines.add("P000999 테스트 1990-01-01 010-0000-0000 0");
        testLines.add("");
        testLines.add("[예약번호] [예약날짜] [시작시간] [종료시간] [진료과] [의사번호] [상태]");
        testLines.add(id1 + " 2025-12-15 10:00 10:10 IM D00001 1");
        java.nio.file.Files.write(testFile, testLines);

        try {
            // 새로운 인스턴스 생성 (새로운 의사 가입 시나리오)
            ReservationRepository repo2 = new ReservationRepository();
            String id2 = repo2.getNextReservationId();

            assertNotEquals(id1, id2, "다른 인스턴스에서도 예약번호는 중복되면 안됩니다");

            int num1 = Integer.parseInt(id1.substring(1));
            int num2 = Integer.parseInt(id2.substring(1));

            assertTrue(num2 > num1,
                "새 인스턴스의 예약번호는 이전 예약번호보다 커야 합니다. repo1: " + id1 + ", repo2: " + id2);

            System.out.println("Repository 1 예약번호: " + id1);
            System.out.println("Repository 2 예약번호: " + id2);
        } finally {
            // 테스트 파일 삭제
            java.nio.file.Files.deleteIfExists(testFile);
        }
    }
}
