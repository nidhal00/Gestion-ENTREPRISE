package com.gestion.util;

public class MailConfig {
    // ─── Gmail SMTP avec App Password ───────────────────────────
    // 1. Activer la 2FA sur votre compte Google
    // 2. Aller à https://myaccount.google.com/apppasswords
    // 3. Créer un mot de passe d'application pour « Mail »
    // 4. Coller le mot de passe de 16 caractères ci-dessous
    public static final String SMTP_HOST     = "smtp.gmail.com";
    public static final String SMTP_PORT     = "587";
    public static final String EMAIL_SENDER  = "mindaudit.plateform@gmail.com";
    public static final String EMAIL_PASSWORD = "ytkrffkesnvtxewa";
    public static final String SENDER_NAME   = "MindAudit Platform";
}
