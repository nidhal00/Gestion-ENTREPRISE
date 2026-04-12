package com.gestion.util;

public class SessionManager {

    private static SessionManager instance;

    private int userId = 1;
    private String role = "ADMIN";
    private String name = "Admin";

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void login(int userId, String role, String name) {
        this.userId = userId;
        this.role = role;
        this.name = name;
    }

    public void logout() {
        this.userId = 1;
        this.role = "ADMIN";
        this.name = "Admin";
    }

    public int getUserId()   { return userId; }
    public String getRole()  { return role; }
    public String getName()  { return name; }
    public boolean isAdmin() { return "ADMIN".equals(role); }
    public boolean isUser()  { return "USER".equals(role); }

    public void toggleRole() {
        if (isAdmin()) {
            this.userId = 101;
            this.role = "USER";
            this.name = "Entreprise Demo";
        } else {
            this.userId = 1;
            this.role = "ADMIN";
            this.name = "Admin";
        }
    }
}
