-- SCRUM-18 / HU-25: conserve los descartes y una recomendación vigente por check-in.
BEGIN;

ALTER TABLE activity_recommendations
    ADD COLUMN IF NOT EXISTS discarded_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS discard_reason VARCHAR(30);

DO $$
DECLARE
    checkin_column SMALLINT;
    old_unique RECORD;
BEGIN
    SELECT attnum INTO STRICT checkin_column
    FROM pg_attribute
    WHERE attrelid = 'activity_recommendations'::regclass AND attname = 'checkin_id';

    FOR old_unique IN
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'activity_recommendations'::regclass
          AND contype = 'u' AND conkey = ARRAY[checkin_column]
    LOOP
        EXECUTE format('ALTER TABLE activity_recommendations DROP CONSTRAINT %I', old_unique.conname);
    END LOOP;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conrelid = 'activity_recommendations'::regclass
                     AND conname = 'ck_activity_recommendations_discarded') THEN
        ALTER TABLE activity_recommendations ADD CONSTRAINT ck_activity_recommendations_discarded
            CHECK ((discard_reason IS NULL OR (discarded_at IS NOT NULL AND discard_reason IN
                    ('TRANSPORTE', 'BIBLIOTECA', 'POCO_ESPACIO', 'POCO_TIEMPO', 'OTRO')))
                   AND (discarded_at IS NULL OR (NOT completed AND completed_at IS NULL)));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_activity_recommendations_checkin ON activity_recommendations(checkin_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_activity_recommendations_current_checkin
    ON activity_recommendations(checkin_id) WHERE discarded_at IS NULL;

COMMIT;
