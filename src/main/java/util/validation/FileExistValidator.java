package util.validation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class FileExistValidator {
    private final List<String> requiredPaths = Arrays.asList(
            "data/patient/patientlist.txt",
            "data/doctor/doctorlist.txt",
            "resource/virtualtime.txt",
            "data/auth/credentials.txt"
    );

    public void validate() {
        System.out.print("필수 파일을 확인합니다...");

        for (String pathString : requiredPaths) {
            Path path = Paths.get(pathString);

            if (!Files.exists(path)) {
                String formattedPath = "/" + pathString.replace("\\", "/");
                System.out.println("[오류] '" + formattedPath + "'이(가) 존재하지 않습니다. 프로그램을 종료합니다.");
                System.exit(0);
            }
        }

        validateDetailFiles("data/patient/patientlist.txt", "data/patient");
        validateDetailFiles("data/doctor/doctorlist.txt", "data/doctor");

        System.out.println("성공");
    }

    private void validateDetailFiles(String listFilePath, String detailFileDir) {
        Path listFile = Paths.get(listFilePath);
        Path detailDir = Paths.get(detailFileDir);

        try {
            List<String> lines = Files.readAllLines(listFile);

            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();

                String[] parts = line.split("\\s+");
                if (parts.length > 0) {
                    String id = parts[0];
                    Path detailFilePath = detailDir.resolve(id + ".txt");

                    if (!Files.exists(detailFilePath)) {
                        String formattedPath = "/" + detailFileDir.replace("\\", "/") + "/" + id + ".txt";
                        System.out.println("\n[오류] '" + formattedPath + "'이(가) 존재하지 않습니다. 프로그램을 종료합니다.");
                        System.exit(0);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("\n[오류] '" + listFilePath + "' 파일을 읽는 중 오류가 발생했습니다. 프로그램을 종료합니다.");
            System.exit(0);
        }
    }
}
