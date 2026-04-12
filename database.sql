CREATE DATABASE IF NOT EXISTS gestion_entreprise_db;
USE gestion_entreprise_db;

CREATE TABLE IF NOT EXISTS entreprise (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    matricule VARCHAR(50) NOT NULL UNIQUE,
    adresse VARCHAR(255),
    email VARCHAR(255),
    telephone VARCHAR(20),
    code_acces VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS document (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    url VARCHAR(500) NOT NULL,
    type VARCHAR(50),
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    entreprise_id INT,
    FOREIGN KEY (entreprise_id) REFERENCES entreprise(id) ON DELETE CASCADE
);
