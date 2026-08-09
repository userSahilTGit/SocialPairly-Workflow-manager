-- Run once on existing DBs where user_identity_verification already exists
-- without ssn / dl_* columns. Skip if columns already present (error 1060).

ALTER TABLE user_identity_verification
  ADD COLUMN ssn VARCHAR(11) NULL AFTER photo_status,
  ADD COLUMN dl_front_document_id BIGINT NULL AFTER ssn,
  ADD COLUMN dl_back_document_id BIGINT NULL AFTER dl_front_document_id;

ALTER TABLE user_identity_verification
  ADD CONSTRAINT fk_uiv_dl_front
    FOREIGN KEY (dl_front_document_id) REFERENCES user_private_documents (id)
    ON DELETE SET NULL,
  ADD CONSTRAINT fk_uiv_dl_back
    FOREIGN KEY (dl_back_document_id) REFERENCES user_private_documents (id)
    ON DELETE SET NULL;
