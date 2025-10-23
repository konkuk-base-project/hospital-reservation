package util.validation;

import util.file.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OrphanDataValidator {

    public void validate() {
        try {
            validatePatientOrphans();
            validateDoctorOrphans();
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

    private static class OrphanDataException extends RuntimeException {
        public OrphanDataException(String message) {
            super(message);
        }
    }
}
