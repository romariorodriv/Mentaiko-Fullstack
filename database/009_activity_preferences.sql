-- SCRUM-30: catálogo de tipos y preferencias elegidas por usuario.
ALTER TABLE micro_activities
  ADD COLUMN IF NOT EXISTS activity_type VARCHAR(30) NOT NULL DEFAULT 'GENERAL';
DO $$
BEGIN
 IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_micro_activity_type'
                AND conrelid = 'micro_activities'::regclass) THEN
  ALTER TABLE micro_activities ADD CONSTRAINT ck_micro_activity_type
   CHECK(activity_type IN ('GENERAL','RESPIRACION','MOVIMIENTO','RELAJACION','ESCRITURA','ATENCION_PLENA'));
 END IF;
END $$;
CREATE TABLE IF NOT EXISTS activity_preferences (
 user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 activity_type VARCHAR(30) NOT NULL
   CHECK(activity_type IN ('GENERAL','RESPIRACION','MOVIMIENTO','RELAJACION','ESCRITURA','ATENCION_PLENA')),
 PRIMARY KEY(user_id,activity_type)
);
CREATE INDEX IF NOT EXISTS ix_activity_preferences_type ON activity_preferences(activity_type);
