-- Strict national mobile digit length per country (e.g. India = 10).
ALTER TABLE ref_countries
  ADD COLUMN phone_national_length INT NULL AFTER phone_code;
