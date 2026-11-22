-- SCRUM-17 / HU-27: valoración de utilidad (1 a 5) y comentario opcional
-- sobre una microactividad completada.
ALTER TABLE activity_recommendations
  ADD COLUMN IF NOT EXISTS helpfulness_rating INTEGER,
  ADD COLUMN IF NOT EXISTS feedback_comment VARCHAR(300);
DO $$
BEGIN
 IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_activity_recommendations_helpfulness'
                AND conrelid = 'activity_recommendations'::regclass) THEN
  ALTER TABLE activity_recommendations ADD CONSTRAINT ck_activity_recommendations_helpfulness
   CHECK(helpfulness_rating IS NULL OR helpfulness_rating BETWEEN 1 AND 5);
 END IF;
END $$;
