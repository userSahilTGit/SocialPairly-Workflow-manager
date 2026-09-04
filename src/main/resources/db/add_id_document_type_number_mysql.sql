-- Identity document type + number for Phase 1 Identity Documents section.
-- Images already live in user_identity_compliance_blobs.storage_blob (LONGBLOB).

ALTER TABLE user_identity_compliance
  ADD COLUMN id_document_type VARCHAR(40) NULL AFTER ssn,
  ADD COLUMN id_document_number VARCHAR(100) NULL AFTER id_document_type;
