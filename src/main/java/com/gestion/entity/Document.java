package com.gestion.entity;

import java.util.Date;

public class Document {
    private int id;
    private int entrepriseId;
    private String nom;
    private String type;
    private String url;
    private String statut;
    private Date dateUpload;
    private Integer uploadedBy;
    private Integer noteIa;
    private Integer rating;
    private String analysisReport;

    public Document() {}

    public Document(int id, int entrepriseId, String nom, String type, String url, String statut, Date dateUpload) {
        this.id = id;
        this.entrepriseId = entrepriseId;
        this.nom = nom;
        this.type = type;
        this.url = url;
        this.statut = statut;
        this.dateUpload = dateUpload;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(int entrepriseId) { this.entrepriseId = entrepriseId; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public Date getDateUpload() { return dateUpload; }
    public void setDateUpload(Date dateUpload) { this.dateUpload = dateUpload; }
    public Integer getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(Integer uploadedBy) { this.uploadedBy = uploadedBy; }
    public Integer getNoteIa() { return noteIa; }
    public void setNoteIa(Integer noteIa) { this.noteIa = noteIa; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getAnalysisReport() { return analysisReport; }
    public void setAnalysisReport(String analysisReport) { this.analysisReport = analysisReport; }

    @Override
    public String toString() {
        return nom + " (" + type + ")";
    }
}
