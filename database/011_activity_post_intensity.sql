-- SCRUM-32 / HU-28: intensidad posterior opcional (1 a 5) al completar una microactividad.
-- La intensidad "antes" es la del check-in asociado (checkins.intensity).
ALTER TABLE activity_recommendations
  ADD COLUMN IF NOT EXISTS post_intensity INTEGER;
DO $$
BEGIN
 IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_activity_recommendations_post_intensity'
                AND conrelid = 'activity_recommendations'::regclass) THEN
  ALTER TABLE activity_recommendations ADD CONSTRAINT ck_activity_recommendations_post_intensity
   CHECK(post_intensity IS NULL OR post_intensity BETWEEN 1 AND 5);
 END IF;
END $$;
