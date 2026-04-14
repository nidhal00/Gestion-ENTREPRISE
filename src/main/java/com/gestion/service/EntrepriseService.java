package com.gestion.service;

import com.gestion.entity.Entreprise;
import com.gestion.util.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EntrepriseService {
    private Connection connection;

    public EntrepriseService() {
        connection = DataSource.getInstance().getConnection();
    }

    public void add(Entreprise e) throws SQLException {
        String query = "INSERT INTO entreprise (nom, matricule_fiscale, secteur, taille, pays, email, telephone, adresse, date_creation, statut, owner_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, e.getNom());
            pst.setString(2, e.getMatriculeFiscale());
            pst.setString(3, e.getSecteur());
            pst.setString(4, e.getTaille());
            pst.setString(5, e.getPays());
            pst.setString(6, e.getEmail());
            pst.setString(7, e.getTelephone());
            pst.setString(8, e.getAdresse());
            long ts = (e.getDateCreation() != null)
                ? e.getDateCreation().getTime()
                : System.currentTimeMillis();
            pst.setTimestamp(9, new Timestamp(ts));
            // Utilise le statut défini (ex: validé par score auto), sinon "en_attente" par défaut
            pst.setString(10, e.getStatut() != null ? e.getStatut() : "en_attente");
            // owner_id stocké pour permettre au User de retrouver son entreprise
            if (e.getOwnerId() != null) pst.setInt(11, e.getOwnerId());
            else pst.setNull(11, Types.INTEGER);
            pst.executeUpdate();
        }
    }

    public void update(Entreprise e) throws SQLException {
        String query = "UPDATE entreprise SET nom=?, matricule_fiscale=?, secteur=?, taille=?, pays=?, email=?, telephone=?, adresse=?, statut=? WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, e.getNom());
            pst.setString(2, e.getMatriculeFiscale());
            pst.setString(3, e.getSecteur());
            pst.setString(4, e.getTaille());
            pst.setString(5, e.getPays());
            pst.setString(6, e.getEmail());
            pst.setString(7, e.getTelephone());
            pst.setString(8, e.getAdresse());
            pst.setString(9, e.getStatut());
            pst.setInt(10, e.getId());
            pst.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String query = "DELETE FROM entreprise WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }

    public List<Entreprise> findAll() throws SQLException {
        List<Entreprise> list = new ArrayList<>();
        String query = "SELECT * FROM entreprise ORDER BY date_creation DESC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapResultSetToEntreprise(rs));
            }
        }
        return list;
    }

    public List<Entreprise> findByOwnerId(int ownerId) throws SQLException {
        List<Entreprise> list = new ArrayList<>();
        String query = "SELECT * FROM entreprise WHERE owner_id = ? ORDER BY date_creation DESC";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, ownerId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToEntreprise(rs));
                }
            }
        }
        return list;
    }

    private Entreprise mapResultSetToEntreprise(ResultSet rs) throws SQLException {
        Entreprise e = new Entreprise();
        e.setId(rs.getInt("id"));
        e.setNom(rs.getString("nom"));
        e.setMatriculeFiscale(rs.getString("matricule_fiscale"));
        e.setSecteur(rs.getString("secteur"));
        e.setTaille(rs.getString("taille"));
        e.setPays(rs.getString("pays"));
        e.setEmail(rs.getString("email"));
        e.setTelephone(rs.getString("telephone"));
        e.setAdresse(rs.getString("adresse"));
        e.setDateCreation(rs.getDate("date_creation"));
        e.setStatut(rs.getString("statut"));
        e.setLatitude(rs.getObject("latitude") != null ? rs.getDouble("latitude") : null);
        e.setLongitude(rs.getObject("longitude") != null ? rs.getDouble("longitude") : null);
        e.setRating(rs.getObject("rating") != null ? rs.getInt("rating") : null);
        e.setOwnerId(rs.getObject("owner_id") != null ? rs.getInt("owner_id") : null);
        return e;
    }
}
