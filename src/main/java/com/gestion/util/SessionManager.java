package com.gestion.util;

public class SessionManager {

    private static SessionManager instance;

    private int    userId = 1;
    private String role   = "ADMIN";
    private String name   = "Admin";

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    // ── Getters ────────────────────────────────────────────────
    public int    getUserId() { return userId; }
    public String getRole()   { return role; }
    public String getName()   { return name; }
    public boolean isAdmin()  { return "ADMIN".equals(role); }
    public boolean isUser()   { return "USER".equals(role); }

    // ── Demo toggle (bascule ADMIN ↔ USER pour les tests) ─────
    public void toggleRole() {
        if (isAdmin()) {
            userId = 101;
            role   = "USER";
            name   = "Entreprise Démo";
        } else {
            userId = 1;
            role   = "ADMIN";
            name   = "Admin";
        }
    }
}
