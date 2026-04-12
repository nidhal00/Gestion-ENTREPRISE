package com.gestion.entity;

import java.util.Date;

public class Entreprise {
    private int id;
    private String nom;
    private String matriculeFiscale;
    private String secteur;
    private String taille;
    private String pays;
    private String email;
    private String telephone;
    private String adresse;
    private Date dateCreation;
    private String statut;
    private Double latitude;
    private Double longitude;
    private Integer rating;
    private String accessCode;
    private Date dateAuditDebut;
    private Date dateAuditFin;
    private Integer ownerId;
    private Integer complianceScore;

    public Entreprise() {}

    public Entreprise(int id, String nom, String matriculeFiscale, String email, String telephone, String accessCode) {
        this.id = id;
        this.nom = nom;
        this.matriculeFiscale = matriculeFiscale;
        this.email = email;
        this.telephone = telephone;
        this.accessCode = accessCode;
    }

    public Entreprise(int id, String nom, String matriculeFiscale, String secteur, String taille, String pays, String email, String telephone, String adresse, Date dateCreation, String statut, Double latitude, Double longitude, Integer rating, String accessCode, Date dateAuditDebut, Date dateAuditFin, Integer ownerId) {
        this.id = id;
        this.nom = nom;
        this.matriculeFiscale = matriculeFiscale;
        this.secteur = secteur;
        this.taille = taille;
        this.pays = pays;
        this.email = email;
        this.telephone = telephone;
        this.adresse = adresse;
        this.dateCreation = dateCreation;
        this.statut = statut;
        this.latitude = latitude;
        this.longitude = longitude;
        this.rating = rating;
        this.accessCode = accessCode;
        this.dateAuditDebut = dateAuditDebut;
        this.dateAuditFin = dateAuditFin;
        this.ownerId = ownerId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getMatriculeFiscale() { return matriculeFiscale; }
    public void setMatriculeFiscale(String matriculeFiscale) { this.matriculeFiscale = matriculeFiscale; }
    public String getSecteur() { return secteur; }
    public void setSecteur(String secteur) { this.secteur = secteur; }
    public String getTaille() { return taille; }
    public void setTaille(String taille) { this.taille = taille; }
    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public Date getDateCreation() { return dateCreation; }
    public void setDateCreation(Date dateCreation) { this.dateCreation = dateCreation; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getAccessCode() { return accessCode; }
    public void setAccessCode(String accessCode) { this.accessCode = accessCode; }
    public Date getDateAuditDebut() { return dateAuditDebut; }
    public void setDateAuditDebut(Date dateAuditDebut) { this.dateAuditDebut = dateAuditDebut; }
    public Date getDateAuditFin() { return dateAuditFin; }
    public void setDateAuditFin(Date dateAuditFin) { this.dateAuditFin = dateAuditFin; }
    public Integer getOwnerId() { return ownerId; }
    public void setOwnerId(Integer ownerId) { this.ownerId = ownerId; }
    public Integer getComplianceScore() { return complianceScore; }
    public void setComplianceScore(Integer complianceScore) { this.complianceScore = complianceScore; }

    @Override
    public String toString() {
        return nom + " (" + matriculeFiscale + ")";
    }
}
