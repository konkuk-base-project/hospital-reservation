package util.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

public class FileUtil {

    public static List<String> readLines(String filePath) throws IOException {
        Path resourcePath = getResourcePath(filePath);

        if (!Files.exists(resourcePath)) {
            return Collections.emptyList();
        }

        return Files.readAllLines(resourcePath);
    }

    public static boolean resourceExists(String filePath) {
        return Files.exists(getResourcePath(filePath));
    }

    public static void appendLine(String filePath, String line) throws IOException {
        Path path = getResourcePath(filePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, System.lineSeparator() + line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public static void createDirectoriesAndWrite(Path filePath, List<String> lines) throws IOException {
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, lines);
    }

    /**
     * 프로젝트 루트 기준 경로를 반환합니다
     *
     * @param relativePath 프로젝트 루트 기준 상대 경로
     * @return 파일의 절대 경로
     */
    public static Path getResourcePath(String relativePath) {
        // 프로젝트 루트를 기준으로 경로 생성 (한글 경로 문제 해결)
        String userDir = System.getProperty("user.dir");
        return Paths.get(userDir, relativePath);
    }
}