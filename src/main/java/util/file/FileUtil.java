package util.file;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FileUtil {

    public static List<String> readLines(String filePath) throws IOException {
        InputStream is = FileUtil.class.getClassLoader().getResourceAsStream(filePath);

        if (is == null) {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return Collections.emptyList();
            }
            return Files.readAllLines(path);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.toList());
        }
    }

    public static boolean resourceExists(String filePath) {
        InputStream is = FileUtil.class.getClassLoader().getResourceAsStream(filePath);
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                // ignore
            }
            return true;
        }
        return Files.exists(Paths.get(filePath));
    }

    public static void appendLine(String filePath, String line) throws IOException {
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, System.lineSeparator() + line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public static void createDirectoriesAndWrite(Path filePath, List<String> lines) throws IOException {
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, lines);
    }

    /**
     * 리소스 디렉토리의 경로를 반환합니다
     *
     * @param relativePath resources 디렉토리 기준 상대 경로
     * @return 리소스 파일의 절대 경로
     */
    public static Path getResourcePath(String relativePath) {
        // resources 디렉토리를 기준으로 경로 생성 (한글 경로 문제 해결)
        String userDir = System.getProperty("user.dir");
        return Paths.get(userDir, "src", "main", "resources", relativePath);
    }
}