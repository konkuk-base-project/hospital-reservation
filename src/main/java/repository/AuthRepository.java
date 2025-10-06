package repository;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import model.User;
import util.file.FileUtil;

public class AuthRepository {
    private static final String CREDENTIALS_FILE_PATH = "data/auth/credentials.txt";
    private final Map<String, User> users = new HashMap<>();

    public AuthRepository() {
        loadUsersFromFile();
    }

    private void loadUsersFromFile() {
        try {
            List<String> lines = FileUtil.readLines(CREDENTIALS_FILE_PATH);
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\s+");
                if (parts.length == 4) {
                    User user = new User(parts[0], parts[1], parts[2], parts[3]);
                    users.put(user.getUsername(), user);
                }
            }
        } catch (IOException e) {
            System.out.println("[오류] 인증 파일을 읽는 중 오류가 발생했습니다: " + e.getMessage());
            System.exit(1);
        }
    }

    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    public void save(User user) throws IOException {
        FileUtil.appendLine(CREDENTIALS_FILE_PATH, user.toFileString());
        users.put(user.getUsername(), user);
    }
}