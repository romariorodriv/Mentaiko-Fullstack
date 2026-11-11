-- SCRUM-30: catálogo de tipos y preferencias elegidas por usuario.
ALTER TABLE micro_activities
  ADD COLUMN IF NOT EXISTS activity_type VARCHAR(30) NOT NULL DEFAULT 'GENERAL';
ALTER TABLE micro_activities
  ADD CONSTRAINT ck_micro_activity_type
  CHECK(activity_type IN ('GENERAL','RESPIRACION','MOVIMIENTO','RELAJACION','ESCRITURA','ATENCION_PLENA'));
CREATE TABLE IF NOT EXISTS activity_preferences (
 user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 activity_type VARCHAR(30) NOT NULL
   CHECK(activity_type IN ('GENERAL','RESPIRACION','MOVIMIENTO','RELAJACION','ESCRITURA','ATENCION_PLENA')),
 PRIMARY KEY(user_id,activity_type)
);
CREATE INDEX IF NOT EXISTS ix_activity_preferences_type ON activity_preferences(activity_type);
