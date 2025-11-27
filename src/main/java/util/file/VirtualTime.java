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
    // 프로그램 시작 시 realTime 저장
    private static LocalDateTime baseTime; // BASE_TIME

    // 1) 프로그램 시작 시 virtualtime.txt에서 BASE_TIME을 읽어옴
    public static void load() {
        try {
            List<String> lines = FileUtil.readLines(FILE_PATH);

            // 파일이 없거나 비어있으면 기본값 사용
            if (lines == null || lines.isEmpty()) {
                initDefaultBaseTime();
            } else {
                boolean found = false;

                for (String line : lines) {
                    if (line == null)
                        continue;
                    line = line.trim();
                    if (line.isEmpty())
                        continue;

                    // 예: BASE_TIME=2025-10-01 09:00:00
                    String[] parts = line.split("=", 2);
                    if (parts.length != 2)
                        continue;

                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    if ("BASE_TIME".equals(key) && DATETIME_PATTERN.matcher(value).matches()) {
                        baseTime = LocalDateTime.parse(value, FORMATTER);
                        found = true;
                        break;
                    }
                }

                // BASE_TIME 줄을 못 찾았거나 형식이 이상하면 기본값으로 초기화
                if (!found) {
                    initDefaultBaseTime();
                }
            }
        } catch (Exception e) {
            // 파일 읽기/파싱 중 에러가 나면 기본값으로 초기화
            initDefaultBaseTime();
        }

    }

    // 클래스가 처음 사용될 때 자동으로 호출되는 static 블록
    static {
        load();
    }

    // 2) 기본 BASE_TIME(2025-10-01 09:00:00)으로 초기화하고 파일에 쓰는 함수
    private static void initDefaultBaseTime() {
        baseTime = LocalDateTime.of(2025, 10, 1, 9, 0, 0);

        String line = "BASE_TIME=" + FORMATTER.format(baseTime);
        FileUtil.writeLines(FILE_PATH, List.of(line));
    }

    // 현재 가상 "날짜"만 필요할 때 사용
    public static LocalDate currentDate() {
        return currentDateTime().toLocalDate();
    }

    // 현재 가상 "날짜+시간" 반환 (BASE_TIME)
    public static LocalDateTime currentDateTime() {
        // 아직 안 초기화되어 있으면 한 번 초기화
        if (baseTime == null) {
            load();
        }
        return baseTime;
    }

    public static void setTime(LocalDateTime newTime) {
        baseTime = newTime; // 메모리 반영

        String line = "BASE_TIME=" + FORMATTER.format(newTime);
        FileUtil.writeLines(FILE_PATH, List.of(line)); // 파일 저장
    }

}
