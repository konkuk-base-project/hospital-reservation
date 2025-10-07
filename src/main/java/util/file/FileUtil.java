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
}