-- =============================================================================
-- Fold collection child tables into JSON columns
-- Target DB: socialpairly_dev (or prod)
-- Order: 1 ALTER → 2 MIGRATE → 3 DROP (you run DROP after verifying counts)
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1) ALTER — add JSON columns
-- ---------------------------------------------------------------------------

ALTER TABLE user_profiles_details
  ADD COLUMN interests JSON NULL;

ALTER TABLE user_personality_profile
  ADD COLUMN friend_descriptors JSON NULL,
  ADD COLUMN personality_traits JSON NULL;

-- ---------------------------------------------------------------------------
-- 2) MIGRATE — copy from child tables
-- ---------------------------------------------------------------------------

-- Profile interests (chip list)
UPDATE user_profiles_details p
SET interests = (
  SELECT JSON_ARRAYAGG(ui.interest)
  FROM user_interests ui
  WHERE ui.profile_id = p.id
)
WHERE EXISTS (
  SELECT 1 FROM user_interests ui2 WHERE ui2.profile_id = p.id
);

-- Friend descriptors (preserve sort_order; compatible without JSON_ARRAYAGG ORDER BY)
UPDATE user_personality_profile p
SET friend_descriptors = (
  SELECT CAST(
    CONCAT(
      '[',
      GROUP_CONCAT(JSON_QUOTE(ufd.descriptor) ORDER BY ufd.sort_order SEPARATOR ','),
      ']'
    ) AS JSON
  )
  FROM user_friend_descriptors ufd
  WHERE ufd.personality_profile_id = p.id
)
WHERE EXISTS (
  SELECT 1 FROM user_friend_descriptors ufd2 WHERE ufd2.personality_profile_id = p.id
);

-- Personality traits
UPDATE user_personality_profile p
SET personality_traits = (
  SELECT JSON_ARRAYAGG(upt.trait_code)
  FROM user_personality_traits upt
  WHERE upt.personality_profile_id = p.id
)
WHERE EXISTS (
  SELECT 1 FROM user_personality_traits upt2 WHERE upt2.personality_profile_id = p.id
);

-- ---------------------------------------------------------------------------
-- 3) VERIFY (run these, then DROP)
-- ---------------------------------------------------------------------------
-- SELECT id, interests FROM user_profiles_details WHERE interests IS NOT NULL;
-- SELECT id, friend_descriptors, personality_traits FROM user_personality_profile
--   WHERE friend_descriptors IS NOT NULL OR personality_traits IS NOT NULL;
-- SELECT COUNT(*) FROM user_interests;
-- SELECT COUNT(*) FROM user_friend_descriptors;
-- SELECT COUNT(*) FROM user_personality_traits;

-- ---------------------------------------------------------------------------
-- 4) DROP — tables become useless after migrate (run manually when ready)
-- ---------------------------------------------------------------------------
-- DROP TABLE IF EXISTS user_interests;
-- DROP TABLE IF EXISTS user_friend_descriptors;
-- DROP TABLE IF EXISTS user_personality_traits;
