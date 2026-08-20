-- Migrate users_details.phone_number from legacy E.164 (+919876543210) to storage format (+91-9876543210).
-- Skips rows that already contain a hyphen or are OAuth/admin placeholders.

UPDATE users_details
SET phone_number = CONCAT('+971-', SUBSTRING(phone_number, 5))
WHERE phone_number LIKE '+971%'
  AND phone_number NOT LIKE '%-%'
  AND phone_number NOT LIKE 'oauth:%'
  AND LENGTH(phone_number) > 5;

UPDATE users_details
SET phone_number = CONCAT('+91-', SUBSTRING(phone_number, 4))
WHERE phone_number LIKE '+91%'
  AND phone_number NOT LIKE '%-%'
  AND phone_number NOT LIKE 'oauth:%'
  AND LENGTH(phone_number) > 4;

UPDATE users_details
SET phone_number = CONCAT('+44-', SUBSTRING(phone_number, 4))
WHERE phone_number LIKE '+44%'
  AND phone_number NOT LIKE '%-%'
  AND phone_number NOT LIKE 'oauth:%'
  AND LENGTH(phone_number) > 4;

UPDATE users_details
SET phone_number = CONCAT('+1-', SUBSTRING(phone_number, 3))
WHERE phone_number LIKE '+1%'
  AND phone_number NOT LIKE '%-%'
  AND phone_number NOT LIKE 'oauth:%'
  AND LENGTH(phone_number) > 3;
