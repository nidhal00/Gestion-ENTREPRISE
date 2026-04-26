package com.gestion.util;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class MailService {

    private static Session buildSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",             "true");
        props.put("mail.smtp.starttls.enable",  "true");
        props.put("mail.smtp.host",             MailConfig.SMTP_HOST);
        props.put("mail.smtp.port",             MailConfig.SMTP_PORT);
        props.put("mail.smtp.ssl.protocols",    "TLSv1.2");
        props.put("mail.smtp.ssl.trust",        MailConfig.SMTP_HOST);

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(MailConfig.EMAIL_SENDER, MailConfig.EMAIL_PASSWORD);
            }
        });
    }

    public static void sendEmail(String to, String subject, String body) {
        new Thread(() -> {
            try {
                Session session = buildSession();
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(MailConfig.EMAIL_SENDER, MailConfig.SENDER_NAME));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
                message.setSubject(subject);
                message.setText(body);
                Transport.send(message);
                System.out.println("[MailService] ✓ Email envoyé → " + to);
            } catch (Exception e) {
                System.err.println("[MailService] ✗ Erreur → " + to + " : " + e.getMessage());
            }
        }).start();
    }

    public static void sendHtmlEmail(String to, String subject, String htmlContent) {
        new Thread(() -> {
            try {
                Session session = buildSession();
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(MailConfig.EMAIL_SENDER, MailConfig.SENDER_NAME));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
                message.setSubject(subject);
                message.setContent(htmlContent, "text/html; charset=UTF-8");
                Transport.send(message);
                System.out.println("[MailService] ✓ HTML email envoyé → " + to);
            } catch (Exception e) {
                System.err.println("[MailService] ✗ Erreur → " + to + " : " + e.getMessage());
            }
        }).start();
    }

    // ═══════════════════════════════════════════════════════════════
    //  TEMPLATES HTML — EMAILS PROFESSIONNELS MINDAUDIT
    // ═══════════════════════════════════════════════════════════════

    public static void sendEntrepriseValidee(String to, String nomEntreprise, String matricule, String secteur) {
        String subject = "✅ [MindAudit] Entreprise validée — " + nomEntreprise;
        String html = buildTemplate(
            "#10b981",
            "✅",
            "Entreprise Validée",
            nomEntreprise,
            "<p style='color:#e2e8f0;font-size:15px;line-height:1.7;margin:0 0 18px;'>"
            + "Nous avons le plaisir de vous informer que votre entreprise <strong style='color:#10b981;'>"
            + nomEntreprise + "</strong> a été <strong style='color:#10b981;'>validée</strong> par l'équipe MindAudit.</p>"
            + infoRow("Matricule", matricule)
            + infoRow("Secteur", secteur)
            + "<div style='background:rgba(16,185,129,0.08);border:1px solid rgba(16,185,129,0.2);"
            + "border-radius:10px;padding:14px 18px;margin:18px 0;'>"
            + "<p style='color:#34d399;font-size:13px;margin:0;'>📋 <strong>Prochaine étape :</strong> "
            + "Vous pouvez maintenant déposer vos documents justificatifs pour compléter votre dossier d'audit.</p></div>"
        );
        sendHtmlEmail(to, subject, html);
    }

    public static void sendEntrepriseRejetee(String to, String nomEntreprise, String matricule, String secteur) {
        String subject = "❌ [MindAudit] Entreprise non validée — " + nomEntreprise;
        String html = buildTemplate(
            "#ef4444",
            "❌",
            "Entreprise Non Validée",
            nomEntreprise,
            "<p style='color:#e2e8f0;font-size:15px;line-height:1.7;margin:0 0 18px;'>"
            + "Nous vous informons que votre entreprise <strong style='color:#ef4444;'>"
            + nomEntreprise + "</strong> n'a pas été validée par l'administrateur MindAudit.</p>"
            + infoRow("Matricule", matricule)
            + infoRow("Secteur", secteur)
            + "<div style='background:rgba(239,68,68,0.08);border:1px solid rgba(239,68,68,0.2);"
            + "border-radius:10px;padding:14px 18px;margin:18px 0;'>"
            + "<p style='color:#f87171;font-size:13px;margin:0;'>💡 <strong>Action requise :</strong> "
            + "Veuillez vérifier vos informations et les corriger, puis re-soumettre votre dossier.</p></div>"
        );
        sendHtmlEmail(to, subject, html);
    }

    public static void sendDocumentValide(String to, String nomDoc, String typeDoc, String nomEntreprise) {
        String subject = "✅ [MindAudit] Document approuvé — " + nomDoc;
        String html = buildTemplate(
            "#10b981",
            "📄",
            "Document Approuvé",
            nomDoc,
            "<p style='color:#e2e8f0;font-size:15px;line-height:1.7;margin:0 0 18px;'>"
            + "Le document <strong style='color:#10b981;'>" + nomDoc
            + "</strong> de l'entreprise <strong>" + nomEntreprise + "</strong> a été "
            + "<strong style='color:#10b981;'>approuvé</strong> par l'administrateur.</p>"
            + infoRow("Document", nomDoc)
            + infoRow("Catégorie", typeDoc)
            + infoRow("Entreprise", nomEntreprise)
            + "<div style='background:rgba(16,185,129,0.08);border:1px solid rgba(16,185,129,0.2);"
            + "border-radius:10px;padding:14px 18px;margin:18px 0;'>"
            + "<p style='color:#34d399;font-size:13px;margin:0;'>✓ Ce document est maintenant "
            + "intégré à votre dossier de conformité.</p></div>"
        );
        sendHtmlEmail(to, subject, html);
    }

    public static void sendDocumentRejete(String to, String nomDoc, String typeDoc, String nomEntreprise, String motif) {
        String subject = "❌ [MindAudit] Document rejeté — " + nomDoc;
        String html = buildTemplate(
            "#ef4444",
            "📄",
            "Document Rejeté",
            nomDoc,
            "<p style='color:#e2e8f0;font-size:15px;line-height:1.7;margin:0 0 18px;'>"
            + "Le document <strong style='color:#ef4444;'>" + nomDoc
            + "</strong> de l'entreprise <strong>" + nomEntreprise + "</strong> a été "
            + "<strong style='color:#ef4444;'>rejeté</strong>.</p>"
            + infoRow("Document", nomDoc)
            + infoRow("Catégorie", typeDoc)
            + infoRow("Entreprise", nomEntreprise)
            + "<div style='background:rgba(239,68,68,0.08);border:1px solid rgba(239,68,68,0.2);"
            + "border-radius:10px;padding:14px 18px;margin:18px 0;'>"
            + "<p style='color:#f87171;font-size:13px;margin:0;'><strong>Motif du rejet :</strong><br>"
            + motif + "</p></div>"
            + "<div style='background:rgba(245,158,11,0.08);border:1px solid rgba(245,158,11,0.2);"
            + "border-radius:10px;padding:14px 18px;margin:10px 0;'>"
            + "<p style='color:#fbbf24;font-size:13px;margin:0;'>💡 <strong>Action requise :</strong> "
            + "Corrigez le document et re-déposez-le via la plateforme.</p></div>"
        );
        sendHtmlEmail(to, subject, html);
    }

    // ═══════════════════════════════════════════════════════════════
    //  HTML TEMPLATE BUILDER
    // ═══════════════════════════════════════════════════════════════

    private static String infoRow(String label, String value) {
        if (value == null || value.isEmpty()) value = "—";
        return "<div style='display:flex;justify-content:space-between;padding:8px 0;"
            + "border-bottom:1px solid rgba(255,255,255,0.06);'>"
            + "<span style='color:#64748b;font-size:13px;'>" + label + "</span>"
            + "<span style='color:#e2e8f0;font-size:13px;font-weight:600;'>" + value + "</span></div>";
    }

    private static String buildTemplate(String accentColor, String icon, String title, String entityName, String bodyContent) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
            + "<body style='margin:0;padding:0;background-color:#0a0f1e;font-family:Segoe UI,Roboto,Helvetica,Arial,sans-serif;'>"
            + "<div style='max-width:560px;margin:0 auto;background-color:#0f172a;border-radius:16px;"
            + "overflow:hidden;border:1px solid rgba(255,255,255,0.06);'>"

            // Header gradient bar
            + "<div style='height:5px;background:linear-gradient(to right," + accentColor + ",#6366f1);'></div>"

            // Logo area
            + "<div style='padding:30px 32px 20px;text-align:center;'>"
            + "<div style='display:inline-block;width:42px;height:42px;background:linear-gradient(135deg,#6366f1,#8b5cf6);"
            + "border-radius:10px;line-height:42px;font-size:18px;margin-bottom:10px;'>"
            + "<span style='color:white;font-weight:bold;'>M</span></div>"
            + "<h1 style='color:white;font-size:20px;font-weight:700;margin:8px 0 2px;'>MindAudit</h1>"
            + "<p style='color:#475569;font-size:11px;margin:0;letter-spacing:1px;'>PLATEFORME DE CONFORMITÉ</p>"
            + "</div>"

            // Status badge
            + "<div style='text-align:center;padding:0 32px 24px;'>"
            + "<div style='display:inline-block;background:rgba(255,255,255,0.04);"
            + "border:1px solid " + accentColor + "33;border-radius:40px;padding:10px 28px;'>"
            + "<span style='font-size:22px;vertical-align:middle;'>" + icon + "</span>"
            + "<span style='color:" + accentColor + ";font-size:16px;font-weight:700;"
            + "margin-left:10px;vertical-align:middle;'>" + title + "</span></div></div>"

            // Separator
            + "<div style='margin:0 32px;height:1px;background:rgba(255,255,255,0.06);'></div>"

            // Body content
            + "<div style='padding:28px 32px;'>"
            + "<p style='color:#94a3b8;font-size:13px;margin:0 0 16px;'>Bonjour,</p>"
            + bodyContent
            + "</div>"

            // Footer
            + "<div style='margin:0 32px;height:1px;background:rgba(255,255,255,0.06);'></div>"
            + "<div style='padding:20px 32px 28px;text-align:center;'>"
            + "<p style='color:#334155;font-size:11px;margin:0 0 6px;'>Cet email a été envoyé automatiquement par MindAudit.</p>"
            + "<p style='color:#1e293b;font-size:10px;margin:0;'>© 2025 MindAudit — Plateforme de gestion et conformité d'entreprises</p>"
            + "</div>"

            + "</div></body></html>";
    }
}
