-- Education start/graduation month support (Identity Documents education section).
ALTER TABLE educations
  ADD COLUMN start_month TINYINT NULL AFTER start_year,
  ADD COLUMN graduation_month TINYINT NULL AFTER end_year;
