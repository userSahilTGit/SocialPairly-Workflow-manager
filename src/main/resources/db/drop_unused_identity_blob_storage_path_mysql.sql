-- ADO #1560: identity documents are DB LONGBLOB only (no filesystem path).
-- storage_path was a legacy unused column (always NULL after consolidate v2).
-- Run once. Ignore error 1091 if the column is already gone.

ALTER TABLE user_identity_compliance_blobs
  DROP COLUMN storage_path;
