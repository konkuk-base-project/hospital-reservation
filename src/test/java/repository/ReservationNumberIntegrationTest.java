package repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 실제 시나리오를 시뮬레이션하는 통합 테스트
 * - 새로운 의사가 가입한 후 예약을 생성하는 시나리오
 */
public class ReservationNumberIntegrationTest {

    private Path testPatientFile;
    private static final String TEST_PATIENT_ID = "P999999";

    @BeforeEach
    public void setUp() throws Exception {
        // 테스트용 환자 파일 생성
        testPatientFile = util.file.FileUtil.getResourcePath("data/patient/" + TEST_PATIENT_ID + ".txt");
        List<String> lines = new ArrayList<>();
        lines.add(TEST_PATIENT_ID + " testuser 테스트환자 1990-01-01 010-0000-0000 0");
        lines.add("");
        lines.add("[예약번호] [예약날짜] [시작시간] [종료시간] [진료과] [의사번호] [상태]");
        Files.write(testPatientFile, lines);
    }

    @AfterEach
    public void tearDown() throws Exception {
        // 테스트 파일 삭제
        if (testPatientFile != null && Files.exists(testPatientFile)) {
            Files.delete(testPatientFile);
        }
    }

    @Test
    public void testNewDoctorReservationScenario() throws Exception {
        System.out.println("\n=== 새로운 의사 예약 생성 시나리오 테스트 ===");

        // 시나리오 1: 첫 번째 의사의 예약 서비스
        ReservationRepository repo1 = new ReservationRepository();
        String reservation1 = repo1.getNextReservationId();
        System.out.println("의사1의 예약번호: " + reservation1);

        // 예약을 파일에 기록 (실제 시나리오 시뮬레이션)
        List<String> lines = Files.readAllLines(testPatientFile);
        lines.add(reservation1 + " 2025-12-15 10:00 10:10 IM D00001 1");
        Files.write(testPatientFile, lines);

        // 시나리오 2: 새로운 의사가 가입하고 예약 서비스를 시작
        ReservationRepository repo2 = new ReservationRepository();
        String reservation2 = repo2.getNextReservationId();
        System.out.println("의사2의 예약번호 (새 인스턴스): " + reservation2);

        // 예약번호 검증
        assertNotEquals(reservation1, reservation2, "예약번호는 중복되면 안됩니다");

        int num1 = Integer.parseInt(reservation1.substring(1));
        int num2 = Integer.parseInt(reservation2.substring(1));

        assertTrue(num2 > num1,
            String.format("두 번째 예약번호(%s)는 첫 번째 예약번호(%s)보다 커야 합니다",
                reservation2, reservation1));

        assertEquals(num1 + 1, num2,
            String.format("예약번호는 순차적이어야 합니다. 예상: R%08d, 실제: %s",
                num1 + 1, reservation2));

        // 예약을 파일에 기록
        lines = Files.readAllLines(testPatientFile);
        lines.add(reservation2 + " 2025-12-15 11:00 11:10 IM D00002 1");
        Files.write(testPatientFile, lines);

        // 시나리오 3: 기존 인스턴스에서 추가 예약
        String reservation3 = repo1.getNextReservationId();
        System.out.println("의사1의 두 번째 예약번호 (기존 인스턴스): " + reservation3);

        int num3 = Integer.parseInt(reservation3.substring(1));
        assertTrue(num3 > num2,
            String.format("세 번째 예약번호(%s)는 두 번째 예약번호(%s)보다 커야 합니다",
                reservation3, reservation2));

        System.out.println("\n✅ 모든 시나리오 테스트 통과!");
        System.out.println("예약번호 순서: " + reservation1 + " → " + reservation2 + " → " + reservation3);
    }

    @Test
    public void testReservationNumberContinuity() throws Exception {
        System.out.println("\n=== 예약번호 연속성 테스트 ===");

        ReservationRepository repo = new ReservationRepository();

        // 5개의 연속된 예약 생성
        List<String> reservationIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String id = repo.getNextReservationId();
            reservationIds.add(id);

            // 파일에 기록
            List<String> lines = Files.readAllLines(testPatientFile);
            lines.add(id + " 2025-12-15 " + String.format("%02d:00", 10 + i) + " " +
                String.format("%02d:10", 10 + i) + " IM D00001 1");
            Files.write(testPatientFile, lines);

            System.out.println("예약 " + (i + 1) + ": " + id);
        }

        // 연속성 검증
        for (int i = 1; i < reservationIds.size(); i++) {
            int prevNum = Integer.parseInt(reservationIds.get(i - 1).substring(1));
            int currNum = Integer.parseInt(reservationIds.get(i).substring(1));

            assertEquals(prevNum + 1, currNum,
                String.format("예약번호가 연속되지 않습니다: %s → %s",
                    reservationIds.get(i - 1), reservationIds.get(i)));
        }

        System.out.println("✅ 예약번호 연속성 테스트 통과!");
    }
}
