-- SCRUM-19: recordatorios internos, sin notificaciones push ni envíos externos.
CREATE TABLE IF NOT EXISTS reminder_preferences (
  user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  enabled BOOLEAN NOT NULL DEFAULT FALSE,
  local_time TIME NOT NULL DEFAULT TIME '20:00',
  days_of_week VARCHAR(7) NOT NULL DEFAULT '1111111',
  last_acknowledged_date DATE,
  CONSTRAINT ck_reminder_days CHECK (days_of_week ~ '^[01]{7}$')
);
