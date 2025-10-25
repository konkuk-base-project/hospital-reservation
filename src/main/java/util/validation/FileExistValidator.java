package util.validation;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import util.file.FileUtil;

public class FileExistValidator {
    private final List<String> requiredPaths = Arrays.asList(
            "data/patient/patientlist.txt",
            "data/doctor/doctorlist.txt",
            "data/time/virtualtime.txt",
            "data/auth/credentials.txt"
    );

    public void validate() {
        System.out.print("필수 파일을 확인합니다...");

        for (String pathString : requiredPaths) {
            if (!FileUtil.resourceExists(pathString)) {
                String formattedPath = "/" + pathString.replace("\\", "/");
                System.out.println();
                System.out.println("[오류] '" + formattedPath + "'이(가) 존재하지 않습니다. 프로그램을 종료합니다.");
                System.exit(0);
            }
        }

        System.out.println(" 성공");
    }
}
