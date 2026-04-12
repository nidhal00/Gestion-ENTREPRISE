package com.gestion.service;

import com.gestion.entity.Document;
import com.gestion.util.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentService {
    private Connection connection;

    public DocumentService() {
        connection = DataSource.getInstance().getConnection();
    }

    public void add(Document d) throws SQLException {
        String query = "INSERT INTO document (entreprise_id, nom, type, url, statut, date_upload) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, d.getEntrepriseId());
            pst.setString(2, d.getNom());
            pst.setString(3, d.getType());
            pst.setString(4, d.getUrl());
            pst.setString(5, d.getStatut() != null ? d.getStatut() : "en_attente");
            pst.setTimestamp(6, new Timestamp(d.getDateUpload() != null ? d.getDateUpload().getTime() : System.currentTimeMillis()));
            pst.executeUpdate();
        }
    }

    public void update(Document d) throws SQLException {
        String query = "UPDATE document SET entreprise_id=?, nom=?, type=?, url=?, statut=? WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, d.getEntrepriseId());
            pst.setString(2, d.getNom());
            pst.setString(3, d.getType());
            pst.setString(4, d.getUrl());
            pst.setString(5, d.getStatut());
            pst.setInt(6, d.getId());
            pst.executeUpdate();
        }
    }

    public void updateStatut(int id, String statut) throws SQLException {
        String query = "UPDATE document SET statut=? WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, statut);
            pst.setInt(2, id);
            pst.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String query = "DELETE FROM document WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }

    public List<Document> findAll() throws SQLException {
        List<Document> list = new ArrayList<>();
        String query = "SELECT * FROM document ORDER BY date_upload DESC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapResultSetToDocument(rs));
            }
        }
        return list;
    }

    public List<Document> findByEntrepriseId(int entrepriseId) throws SQLException {
        List<Document> list = new ArrayList<>();
        String query = "SELECT * FROM document WHERE entreprise_id = ? ORDER BY date_upload DESC";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, entrepriseId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDocument(rs));
                }
            }
        }
        return list;
    }

    public Map<Integer, List<String>> getCategoriesPerEntreprise() throws SQLException {
        Map<Integer, List<String>> map = new HashMap<>();
        String query = "SELECT entreprise_id, type FROM document WHERE statut = 'validé'";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                int entId = rs.getInt("entreprise_id");
                String type = rs.getString("type");
                map.computeIfAbsent(entId, k -> new ArrayList<>()).add(type);
            }
        }
        return map;
    }

    private Document mapResultSetToDocument(ResultSet rs) throws SQLException {
        Document d = new Document();
        d.setId(rs.getInt("id"));
        d.setEntrepriseId(rs.getInt("entreprise_id"));
        d.setNom(rs.getString("nom"));
        d.setType(rs.getString("type"));
        d.setUrl(rs.getString("url"));
        d.setStatut(rs.getString("statut"));
        d.setDateUpload(rs.getTimestamp("date_upload"));
        d.setUploadedBy(rs.getObject("uploaded_by") != null ? rs.getInt("uploaded_by") : null);
        d.setNoteIa(rs.getObject("note_ia") != null ? rs.getInt("note_ia") : null);
        d.setRating(rs.getObject("rating") != null ? rs.getInt("rating") : null);
        d.setAnalysisReport(rs.getString("analysis_report"));
        return d;
    }
}
