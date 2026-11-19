-- SCRUM-16: Favoritos individuales. Ejecutar una vez en PostgreSQL si no se usa DDL automático.
CREATE TABLE IF NOT EXISTS favorite_micro_activities (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    micro_activity_id BIGINT NOT NULL REFERENCES micro_activities(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_favorite_micro_activity_user_activity UNIQUE (user_id, micro_activity_id)
);

CREATE INDEX IF NOT EXISTS idx_favorites_user_created
    ON favorite_micro_activities (user_id, created_at DESC);
