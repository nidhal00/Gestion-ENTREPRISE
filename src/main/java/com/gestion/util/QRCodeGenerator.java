package com.gestion.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.gestion.entity.Entreprise;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

public class QRCodeGenerator {

    private static String buildContent(Entreprise e) {
        String date = (e.getDateCreation() != null)
            ? new SimpleDateFormat("dd/MM/yyyy").format(e.getDateCreation())
            : "N/A";
        return "=== MINDAUDIT - AUDIT CARD ===\n"
            + "Entreprise : " + e.getNom() + "\n"
            + "Matricule  : " + e.getMatriculeFiscale() + "\n"
            + "Secteur    : " + (e.getSecteur()  != null ? e.getSecteur()  : "-") + "\n"
            + "Taille     : " + (e.getTaille()   != null ? e.getTaille()   : "-") + "\n"
            + "Statut     : " + (e.getStatut()   != null ? e.getStatut()   : "-") + "\n"
            + "Conformite : " + (e.getComplianceScore() != null ? e.getComplianceScore() : 0) + "%\n"
            + "Email      : " + (e.getEmail()    != null ? e.getEmail()    : "-") + "\n"
            + "Tel        : " + (e.getTelephone()!= null ? e.getTelephone(): "-") + "\n"
            + "Cree le    : " + date + "\n"
            + "==============================\n"
            + "Certifie MindAudit 2025";
    }

    public static BufferedImage generateImage(Entreprise e) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(buildContent(e), BarcodeFormat.QR_CODE, 300, 300);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    public static void saveToFile(BufferedImage image, String filePath) throws Exception {
        ImageIO.write(image, "PNG", Paths.get(filePath).toFile());
    }

    public static String generate(Entreprise e, String outputDir) throws Exception {
        BufferedImage image = generateImage(e);
        String filePath = outputDir + "/mindaudit_qr_" + e.getId() + "_"
            + e.getNom().replaceAll("[^a-zA-Z0-9]", "_") + ".png";
        saveToFile(image, filePath);
        return filePath;
    }
}
