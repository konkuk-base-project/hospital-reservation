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
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return Collections.emptyList();
        }
        return Files.readAllLines(path);
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