package util.validation;

import util.file.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OrphanDataValidator {

    private static final Pattern RESERVATION_ID_PATTERN = Pattern.compile("^R\\d{8}$");

    public void validate() {
        try {
            validatePatientOrphans();
            validateDoctorOrphans();
            validateAppointmentOrphans();

        } catch (OrphanDataException e) {
            System.out.println();
            System.out.println(e.getMessage());
            System.exit(0);
        } catch (Exception e) {
            System.out.println();
            System.out.println("[오류] 고아 데이터 검사 중 예기치 못한 오류가 발생하였습니다. 프로그램을 종료합니다.");
            System.exit(0);
        }
    }

    private void validatePatientOrphans() throws IOException {
        // 1. patientlist.txt에서 참조된 환자 ID 수집
        Set<String> referencedPatientIds = new HashSet<>();
        List<String> lines = FileUtil.readLines("data/patient/patientlist.txt");

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            if (parts.length > 0) {
                referencedPatientIds.add(parts[0]); // 환자번호 (P000001 형식)
            }
        }

        // 2. 실제 존재하는 환자 데이터 파일 수집
        Path patientDir = FileUtil.getResourcePath("data/patient");
        Set<String> existingPatientIds = new HashSet<>();

        try (Stream<Path> files = Files.list(patientDir)) {
            existingPatientIds = files
                .filter(path -> path.toString().endsWith(".txt"))
                .filter(path -> !path.getFileName().toString().equals("patientlist.txt"))
                .map(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.replace(".txt", ""); // P000001.txt -> P000001
                })
                .filter(id -> id.matches("^P\\d{6}$")) // P000001 형식만 필터링
                .collect(Collectors.toSet());
        }

        // 3. 고아 파일 검출 (파일은 존재하지만 list에 없음)
        Set<String> orphanPatientIds = new HashSet<>(existingPatientIds);
        orphanPatientIds.removeAll(referencedPatientIds);

        if (!orphanPatientIds.isEmpty()) {
            String orphanFiles = orphanPatientIds.stream()
                .sorted()
                .map(id -> "/data/patient/" + id + ".txt")
                .collect(Collectors.joining(", "));

            throw new OrphanDataException(
                "[오류] 다음 환자 데이터 파일이 '/data/patient/patientlist.txt'에 등록되지 않았습니다: "
                + orphanFiles + " 프로그램을 종료합니다."
            );
        }
    }

    private void validateDoctorOrphans() throws IOException {
        // 1. doctorlist.txt에서 참조된 의사 ID 수집
        Set<String> referencedDoctorIds = new HashSet<>();
        List<String> lines = FileUtil.readLines("data/doctor/doctorlist.txt");

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            if (parts.length > 0) {
                referencedDoctorIds.add(parts[0]); // 의사번호 (D00001 형식)
            }
        }

        // 2. 실제 존재하는 의사 데이터 파일 수집
        Path doctorDir = FileUtil.getResourcePath("data/doctor");
        Set<String> existingDoctorIds = new HashSet<>();

        try (Stream<Path> files = Files.list(doctorDir)) {
            existingDoctorIds = files
                .filter(path -> path.toString().endsWith(".txt"))
                .filter(path -> !path.getFileName().toString().equals("doctorlist.txt"))
                .map(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.replace(".txt", ""); // D00001.txt -> D00001
                })
                .filter(id -> id.matches("^D\\d{5}$")) // D00001 형식만 필터링
                .collect(Collectors.toSet());
        }

        // 3. 고아 파일 검출 (파일은 존재하지만 list에 없음)
        Set<String> orphanDoctorIds = new HashSet<>(existingDoctorIds);
        orphanDoctorIds.removeAll(referencedDoctorIds);

        if (!orphanDoctorIds.isEmpty()) {
            String orphanFiles = orphanDoctorIds.stream()
                .sorted()
                .map(id -> "/data/doctor/" + id + ".txt")
                .collect(Collectors.joining(", "));

            throw new OrphanDataException(
                "[오류] 다음 의사 데이터 파일이 '/data/doctor/doctorlist.txt'에 등록되지 않았습니다: "
                + orphanFiles + " 프로그램을 종료합니다."
            );
        }
    }

    private void validateAppointmentOrphans() throws IOException {
        // 1. 모든 환자 파일에서 예약 ID 수집
        Set<String> reservationsInPatients = collectReservationsFromPatients();

        // 2. 모든 의사 파일에서 예약 ID 수집
        Set<String> reservationsInDoctors = collectReservationsFromDoctors();

        // 3. 모든 예약 파일에서 예약 ID 수집
        Set<String> reservationsInAppointments = collectReservationsFromAppointments();

        // 4. 교차 검증: 세 곳 모두에 일관되게 존재해야 함
        validateAppointmentConsistency(reservationsInPatients, reservationsInDoctors, reservationsInAppointments);
    }

    private Set<String> collectReservationsFromPatients() throws IOException {
        Set<String> reservations = new HashSet<>();
        List<String> patientList = FileUtil.readLines("data/patient/patientlist.txt");

        for (int i = 1; i < patientList.size(); i++) {
            String line = patientList.get(i).trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            if (parts.length > 0) {
                String patientId = parts[0];
                String patientFile = "data/patient/" + patientId + ".txt";

                if (FileUtil.resourceExists(patientFile)) {
                    List<String> patientLines = FileUtil.readLines(patientFile);

                    // 4행부터 예약 내역 (1행: 환자정보, 2행: 빈행, 3행: 헤더)
                    for (int j = 3; j < patientLines.size(); j++) {
                        String reservationLine = patientLines.get(j).trim();
                        if (reservationLine.isEmpty()) continue;

                        String[] reservationParts = reservationLine.split("\\s+");
                        if (reservationParts.length > 0 && RESERVATION_ID_PATTERN.matcher(reservationParts[0]).matches()) {
                            reservations.add(reservationParts[0]);
                        }
                    }
                }
            }
        }

        return reservations;
    }

    private Set<String> collectReservationsFromDoctors() throws IOException {
        Set<String> reservations = new HashSet<>();
        List<String> doctorList = FileUtil.readLines("data/doctor/doctorlist.txt");

        for (int i = 1; i < doctorList.size(); i++) {
            String line = doctorList.get(i).trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            if (parts.length > 0) {
                String doctorId = parts[0];
                String doctorFile = "data/doctor/" + doctorId + ".txt";

                if (FileUtil.resourceExists(doctorFile)) {
                    List<String> doctorLines = FileUtil.readLines(doctorFile);

                    // 4행부터 스케줄 (1행: 의사정보, 2행: 요일, 3행: 빈행)
                    for (int j = 3; j < doctorLines.size(); j++) {
                        String scheduleLine = doctorLines.get(j).trim();
                        if (scheduleLine.isEmpty()) continue;

                        String[] scheduleParts = scheduleLine.split("\\s+");
                        // 첫 번째는 날짜, 나머지는 슬롯 (0 또는 예약번호)
                        for (int k = 1; k < scheduleParts.length; k++) {
                            String slot = scheduleParts[k];
                            if (RESERVATION_ID_PATTERN.matcher(slot).matches()) {
                                reservations.add(slot);
                            }
                        }
                    }
                }
            }
        }

        return reservations;
    }

    private Set<String> collectReservationsFromAppointments() throws IOException {
        Set<String> reservations = new HashSet<>();
        Path appointmentDir = FileUtil.getResourcePath("data/appointment");

        if (!Files.exists(appointmentDir)) {
            return reservations; // 예약 디렉토리가 없으면 빈 세트 반환
        }

        try (Stream<Path> files = Files.list(appointmentDir)) {
            files.filter(path -> path.toString().endsWith(".txt"))
                 .filter(path -> !path.getFileName().toString().contains("backup"))
                 .filter(path -> path.getFileName().toString().matches("\\d{8}\\.txt"))
                 .forEach(path -> {
                     try {
                         List<String> lines = Files.readAllLines(path);

                         // 3행부터 시간표 (1행: 날짜, 2행: 헤더)
                         for (int i = 2; i < lines.size(); i++) {
                             String line = lines.get(i).trim();
                             if (line.isEmpty()) continue;

                             String[] parts = line.split("\\s+");
                             // 첫 번째는 시간, 나머지는 의사별 슬롯
                             for (int j = 1; j < parts.length; j++) {
                                 String slot = parts[j];
                                 if (RESERVATION_ID_PATTERN.matcher(slot).matches()) {
                                     reservations.add(slot);
                                 }
                             }
                         }
                     } catch (IOException e) {
                         // 파일 읽기 실패는 무시 (다른 검증에서 처리됨)
                     }
                 });
        }

        return reservations;
    }

    private void validateAppointmentConsistency(
            Set<String> inPatients,
            Set<String> inDoctors,
            Set<String> inAppointments) {

        // 환자 파일에만 있는 고아 예약
        Set<String> orphanInPatients = new HashSet<>(inPatients);
        orphanInPatients.removeAll(inDoctors);
        orphanInPatients.removeAll(inAppointments);

        if (!orphanInPatients.isEmpty()) {
            String orphans = orphanInPatients.stream().sorted().collect(Collectors.joining(", "));
            throw new OrphanDataException(
                "[오류] 다음 예약이 환자 파일에만 존재하고 의사 스케줄이나 예약 파일에는 없습니다: "
                + orphans + " 프로그램을 종료합니다."
            );
        }

        // 의사 파일에만 있는 고아 예약
        Set<String> orphanInDoctors = new HashSet<>(inDoctors);
        orphanInDoctors.removeAll(inPatients);
        orphanInDoctors.removeAll(inAppointments);

        if (!orphanInDoctors.isEmpty()) {
            String orphans = orphanInDoctors.stream().sorted().collect(Collectors.joining(", "));
            throw new OrphanDataException(
                "[오류] 다음 예약이 의사 스케줄에만 존재하고 환자 파일이나 예약 파일에는 없습니다: "
                + orphans + " 프로그램을 종료합니다."
            );
        }

        // 예약 파일에만 있는 고아 예약
        Set<String> orphanInAppointments = new HashSet<>(inAppointments);
        orphanInAppointments.removeAll(inPatients);
        orphanInAppointments.removeAll(inDoctors);

        if (!orphanInAppointments.isEmpty()) {
            String orphans = orphanInAppointments.stream().sorted().collect(Collectors.joining(", "));
            throw new OrphanDataException(
                "[오류] 다음 예약이 예약 파일에만 존재하고 환자 파일이나 의사 스케줄에는 없습니다: "
                + orphans + " 프로그램을 종료합니다."
            );
        }
    }

    private static class OrphanDataException extends RuntimeException {
        public OrphanDataException(String message) {
            super(message);
        }
    }
}
