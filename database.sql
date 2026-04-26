-- Base de données MindAudit PIDEV
-- Schéma propre sans FK vers table utilisateur (auth intégrée plus tard)

CREATE DATABASE IF NOT EXISTS mindaudit_pidev
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mindaudit_pidev;

-- Supprimer la contrainte FK owner_id → utilisateur si elle existe
-- (exécuter si la base existante la contient encore)
SET FOREIGN_KEY_CHECKS = 0;

-- ────────────────────────────────────────────────────────────
-- Table : entreprise
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS entreprise (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    nom              VARCHAR(255)    NOT NULL,
    matricule_fiscale VARCHAR(100)   NOT NULL UNIQUE,
    secteur          VARCHAR(100),
    taille           ENUM('small','medium','large'),
    pays             VARCHAR(100),
    email            VARCHAR(255),
    telephone        VARCHAR(20),
    adresse          VARCHAR(255),
    date_creation    TIMESTAMP       DEFAULT NOW(),
    statut           VARCHAR(50)     DEFAULT 'en_attente',
    latitude         DOUBLE,
    longitude        DOUBLE,
    rating           INT,
    access_code      VARCHAR(255),
    date_audit_debut DATE,
    date_audit_fin   DATE,
    owner_id         INT             -- Pas de FK pour l'instant (auth à intégrer)
);

-- ────────────────────────────────────────────────────────────
-- Table : document
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS document (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    entreprise_id   INT,
    nom             VARCHAR(255)    NOT NULL,
    type            VARCHAR(50),
    url             VARCHAR(500)    NOT NULL,
    statut          VARCHAR(50)     DEFAULT 'en_attente',
    date_upload     TIMESTAMP       DEFAULT NOW(),
    uploaded_by     INT,
    note_ia         INT,
    rating          INT,
    analysis_report TEXT,
    FOREIGN KEY (entreprise_id) REFERENCES entreprise(id) ON DELETE CASCADE
);

-- ────────────────────────────────────────────────────────────
-- Table : audit_log
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_log (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT,
    user_name    VARCHAR(100),
    action       VARCHAR(100) NOT NULL,
    entity_type  VARCHAR(50),
    entity_id    INT,
    detail       TEXT,
    created_at   TIMESTAMP DEFAULT NOW()
);

SET FOREIGN_KEY_CHECKS = 1;
