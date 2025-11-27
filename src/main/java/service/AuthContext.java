package service;

import model.User;

public class AuthContext {
    private User currentUser;

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public void login(User user) {
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public String getPrompt() {
        if (!isLoggedIn()) {
            return "Main";
        }
        if ("ADMIN".equals(currentUser.getRole())) {
            return "Admin";
        }
        if ("DOCTOR".equals(currentUser.getRole())) {
            return "Doctor";
        }
        return "User";
    }
}