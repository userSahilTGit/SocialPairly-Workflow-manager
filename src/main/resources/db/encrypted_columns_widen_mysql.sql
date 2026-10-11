-- Defect 1584: widen columns that now hold AES-256-GCM ciphertext.
-- SensitiveDataEncryptionMigrator runs this automatically on startup; run manually only if the
-- application DB user lacks ALTER privileges. Safe to re-run.

ALTER TABLE user_identity_compliance MODIFY COLUMN ssn VARCHAR(255) NULL;
ALTER TABLE user_identity_compliance MODIFY COLUMN id_document_number VARCHAR(255) NULL;

ALTER TABLE bank_details MODIFY COLUMN account_number VARCHAR(255) NOT NULL;
ALTER TABLE bank_details MODIFY COLUMN aba_routing_number VARCHAR(255) NOT NULL;
ALTER TABLE bank_details MODIFY COLUMN account_holder_name VARCHAR(512) NOT NULL;
