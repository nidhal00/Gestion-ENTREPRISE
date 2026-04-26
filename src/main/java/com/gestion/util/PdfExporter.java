package com.gestion.util;

import com.gestion.entity.Document;
import com.gestion.entity.Entreprise;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PdfExporter {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public static void exportEntreprise(Entreprise e, List<Document> docs, String outputPath) throws Exception {
        PdfWriter writer = new PdfWriter(new FileOutputStream(outputPath));
        PdfDocument pdf = new PdfDocument(writer);
        com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf);

        // Header
        document.add(new Paragraph("RAPPORT DE CONFORMITÉ - MINDAUDIT")
                .setBold().setFontSize(18).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

        document.add(new Paragraph("Informations de l'entreprise").setBold().setFontSize(14).setMarginBottom(10));
        
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth();
        infoTable.addCell(new Cell().add(new Paragraph("Nom :")).setBold());
        infoTable.addCell(new Cell().add(new Paragraph(e.getNom())));
        infoTable.addCell(new Cell().add(new Paragraph("Matricule :")).setBold());
        infoTable.addCell(new Cell().add(new Paragraph(e.getMatriculeFiscale())));
        infoTable.addCell(new Cell().add(new Paragraph("Statut :")).setBold());
        infoTable.addCell(new Cell().add(new Paragraph(e.getStatut())));
        infoTable.addCell(new Cell().add(new Paragraph("Score Conformité :")).setBold());
        infoTable.addCell(new Cell().add(new Paragraph((e.getComplianceScore() != null ? e.getComplianceScore() : 0) + " %")));
        document.add(infoTable.setMarginBottom(20));

        // Progress Bar textuelle
        int score = e.getComplianceScore() != null ? e.getComplianceScore() : 0;
        String bar = "[";
        int filled = score / 10;
        for (int i = 0; i < 10; i++) bar += (i < filled ? "■" : "□");
        bar += "] " + score + "%";
        document.add(new Paragraph("Niveau de conformité : " + bar).setBold().setMarginBottom(20));

        // Table des documents
        document.add(new Paragraph("Liste des documents déposés").setBold().setFontSize(14).setMarginBottom(10));
        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 2})).useAllAvailableWidth();
        table.addHeaderCell(new Cell().add(new Paragraph("Nom du document")).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(new Cell().add(new Paragraph("Type")).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(new Cell().add(new Paragraph("Statut")).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(new Cell().add(new Paragraph("Date de dépôt")).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY));

        for (Document d : docs) {
            table.addCell(new Cell().add(new Paragraph(d.getNom())));
            table.addCell(new Cell().add(new Paragraph(d.getType())));
            table.addCell(new Cell().add(new Paragraph(d.getStatut())));
            table.addCell(new Cell().add(new Paragraph(d.getDateUpload() != null ? DATE_FORMAT.format(d.getDateUpload()) : "-")));
        }
        document.add(table);

        // Footer
        document.add(new Paragraph("\nGénéré le : " + DATE_FORMAT.format(new Date()))
                .setItalic().setFontSize(10).setTextAlignment(TextAlignment.RIGHT));

        document.close();
    }

    public static void exportListeEntreprises(List<Entreprise> list, String outputPath) throws Exception {
        PdfWriter writer = new PdfWriter(new FileOutputStream(outputPath));
        PdfDocument pdf = new PdfDocument(writer);
        com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf);

        document.add(new Paragraph("LISTE DES ENTREPRISES - MINDAUDIT")
                .setBold().setFontSize(18).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 3, 2, 2, 1.5f})).useAllAvailableWidth();
        table.addHeaderCell(new Cell().add(new Paragraph("Nom")).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(new Cell().add(new Paragraph("Matricule")).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(new Cell().add(new Paragraph("Secteur")).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(new Cell().add(new Paragraph("Statut")).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(new Cell().add(new Paragraph("Score")).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY));

        for (Entreprise e : list) {
            table.addCell(new Cell().add(new Paragraph(e.getNom())));
            table.addCell(new Cell().add(new Paragraph(e.getMatriculeFiscale())));
            table.addCell(new Cell().add(new Paragraph(e.getSecteur() != null ? e.getSecteur() : "-")));
            table.addCell(new Cell().add(new Paragraph(e.getStatut())));
            table.addCell(new Cell().add(new Paragraph((e.getComplianceScore() != null ? e.getComplianceScore() : 0) + "%")));
        }
        document.add(table);

        document.add(new Paragraph("\nTotal : " + list.size() + " entreprises")
                .setBold().setFontSize(10));
        document.add(new Paragraph("Généré le : " + DATE_FORMAT.format(new Date()))
                .setItalic().setFontSize(10).setTextAlignment(TextAlignment.RIGHT));

        document.close();
    }
}
