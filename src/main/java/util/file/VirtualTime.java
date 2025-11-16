package util.file;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

public class VirtualTime {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern DATETIME_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$");
    private static final String FILE_PATH = "data/time/virtualtime.txt";

    public static LocalDate currentDate() {
        try {
            List<String> lines = FileUtil.readLines(FILE_PATH);
            if (lines == null || lines.isEmpty()) return LocalDate.now();

            for (String line : lines) {
                if (line == null) continue;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("=", 2);
                if (parts.length != 2) continue;

                String key = parts[0].trim();
                String value = parts[1].trim();

                if ("BASE_TIME".equals(key) && DATETIME_PATTERN.matcher(value).matches()) {
                    LocalDateTime ldt = LocalDateTime.parse(value, FORMATTER);
                    return ldt.toLocalDate();
                }
            }
        } catch (Exception ignored) {
            // 파싱 실패 시 시스템 날짜 사용
        }
        return LocalDate.now();
    }
}
