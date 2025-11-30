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

    // 변경된 범위
    private static final LocalDateTime MIN_TIME = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
    private static final LocalDateTime MAX_TIME = LocalDateTime.of(2025, 12, 31, 23, 59, 59);

    private static LocalDateTime baseTime; // BASE_TIME


    // 1) 프로그램 시작 시 virtualtime.txt에서 BASE_TIME을 읽어옴
    public static void load() {
        try {
            List<String> lines = FileUtil.readLines(FILE_PATH);

            if (lines == null || lines.isEmpty()) {
                initDefaultBaseTime();
                return;
            }

            boolean found = false;

            for (String line : lines) {
                if (line == null) continue;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("=", 2);
                if (parts.length != 2) continue;

                String key = parts[0].trim();
                String value = parts[1].trim();

                if ("BASE_TIME".equals(key) && DATETIME_PATTERN.matcher(value).matches()) {
                    LocalDateTime parsed = LocalDateTime.parse(value, FORMATTER);
                    baseTime = parsed;
                    found = true;
                    break;
                }
            }

            if (!found) {
                initDefaultBaseTime();
            }

        } catch (Exception e) {
            initDefaultBaseTime();
        }
    }

    static { load(); }

    // 2) 기본 BASE_TIME → 2025-10-01 09:00:00 로 변경
    private static void initDefaultBaseTime() {
        baseTime = LocalDateTime.of(2025, 10, 1, 9, 0, 0);

        String line = "BASE_TIME=" + FORMATTER.format(baseTime);
        FileUtil.writeLines(FILE_PATH, List.of(line));
    }

    public static LocalDate currentDate() {
        return currentDateTime().toLocalDate();
    }

    public static LocalDateTime currentDateTime() {
        if (baseTime == null) load();
        return baseTime;
    }

    // setTime에 검증
    public static void setTime(LocalDateTime newTime) {
        // 범위 검증
        if (newTime.isBefore(MIN_TIME)) {
            System.out.println("[오류] 설정 가능한 시간 범위를 벗어났습니다. (2025-01-01 ~ 2025-12-31)");
            return;
        }
        if (newTime.isAfter(MAX_TIME)) {
            System.out.println("[오류] 설정 가능한 시간 범위를 벗어났습니다. (2025-01-01 ~ 2025-12-31)");
            return;
        }

        // 범위만 맞으면 과거로 이동도 허용됨 
        baseTime = newTime;

        String line = "BASE_TIME=" + FORMATTER.format(newTime);
        FileUtil.writeLines(FILE_PATH, List.of(line)); 
    }

}
