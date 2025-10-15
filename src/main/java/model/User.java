package model;

public class User {
    private final String username;
    private final String hashedPassword;
    private final String role;
    private final String id;

    public User(String username, String hashedPassword, String role, String id) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.role = role;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getUsername() { return username; }
    public String getHashedPassword() { return hashedPassword; }
    public String getRole() { return role; }

    public String toFileString() {
        return String.join(" ", username, hashedPassword, role, id);
    }
}
