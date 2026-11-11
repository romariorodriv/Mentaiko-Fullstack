-- SCRUM-26: acceso por usuario y rango temporal al historial de check-ins.
CREATE INDEX IF NOT EXISTS ix_checkins_user_created_at
  ON checkins(user_id, created_at DESC, id DESC);
