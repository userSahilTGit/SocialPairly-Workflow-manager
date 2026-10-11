-- BUG-1570: store worldwide phone dialing codes on ref_countries
-- Safe to skip if column already exists (Hibernate ddl-auto=update also adds it).
ALTER TABLE ref_countries
  ADD COLUMN phone_code VARCHAR(8) NULL AFTER postal_regex;
