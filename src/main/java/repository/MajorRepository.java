package repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import model.Major;
import util.file.FileUtil;

public class MajorRepository {
    private static final String MAJOR_LIST_FILE_PATH = "data/major/majorlist.txt";
    private final List<Major> majors = new ArrayList<>();

    public MajorRepository() {
        loadMajorsFromFile();
    }

    private void loadMajorsFromFile() {
        if (!FileUtil.resourceExists(MAJOR_LIST_FILE_PATH)) {
            initializeDefaultMajors();
            return;
        }

        try {
            List<String> lines = FileUtil.readLines(MAJOR_LIST_FILE_PATH);
            if (lines.isEmpty()) {
                initializeDefaultMajors();
                return;
            }

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.trim().isEmpty())
                    continue;
                String[] parts = line.split("\\s+", 2);
                if (parts.length == 2) {
                    Major major = new Major(parts[0], parts[1]);
                    majors.add(major);
                }
            }
        } catch (IOException e) {
            System.out.println("[오류] 진료과 목록 파일을 읽는 중 오류가 발생했습니다: " + e.getMessage());
            System.exit(1);
        }
    }

    private void initializeDefaultMajors() {
        majors.add(new Major("IM", "내과"));
        majors.add(new Major("GS", "일반외과"));
        majors.add(new Major("OB", "산부인과"));
        majors.add(new Major("PED", "소아과"));
        majors.add(new Major("PSY", "정신과"));
        majors.add(new Major("DERM", "피부과"));
        majors.add(new Major("ENT", "이비인후과"));
        majors.add(new Major("ORTH", "정형외과"));

        try {
            List<String> lines = new ArrayList<>();
            for (Major major : majors) {
                lines.add(major.toFileString());
            }
            FileUtil.createDirectoriesAndWrite(FileUtil.getResourcePath(MAJOR_LIST_FILE_PATH), lines);
        } catch (IOException e) {
            System.out.println("[오류] 기본 진료과 목록을 저장하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public Optional<Major> findByCode(String majorCode) {
        return majors.stream()
                .filter(m -> m.getMajorCode().equals(majorCode))
                .findFirst();
    }

    public List<Major> findAll() {
        return new ArrayList<>(majors);
    }

    public void save(Major major) throws IOException {
        FileUtil.appendLine(MAJOR_LIST_FILE_PATH, major.toFileString());
        majors.add(major);
    }

    public boolean isMajorExists(String majorCode) {
        return majors.stream().anyMatch(m -> m.getMajorCode().equals(majorCode));
    }
}
