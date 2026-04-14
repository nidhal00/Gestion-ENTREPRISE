-- ============================================================
-- À exécuter dans MySQL Workbench / phpMyAdmin / HeidiSQL
-- sur la base : mindaudit_pidev
-- ============================================================

-- 1. Supprimer la contrainte Foreign Key owner_id → utilisateur
ALTER TABLE entreprise DROP FOREIGN KEY FK_D19FA6076ED395;

-- 2. Vérification (doit retourner 0 lignes pour cette FK)
SELECT CONSTRAINT_NAME
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_NAME = 'entreprise'
  AND CONSTRAINT_TYPE = 'FOREIGN KEY'
  AND TABLE_SCHEMA = 'mindaudit_pidev';
