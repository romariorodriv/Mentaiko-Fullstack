-- HU-22: agrega la categoría DESCANSO_OCULAR al catálogo de tipos de actividad.
-- Equivalencia con Jira: respiración -> RESPIRACION, reflexión escrita -> ESCRITURA,
-- estiramiento -> MOVIMIENTO, descanso ocular -> DESCANSO_OCULAR (nuevo).
-- Idempotente: puede ejecutarse varias veces sobre una base que ya tenga la 009.
-- Ejemplo: psql -h 127.0.0.1 -p 5433 -U mentaiko_user -d mentaiko_db -f 012_activity_type_descanso_ocular.sql
DO $$
DECLARE
 c RECORD;
BEGIN
 ALTER TABLE micro_activities DROP CONSTRAINT IF EXISTS ck_micro_activity_type;
 ALTER TABLE micro_activities ADD CONSTRAINT ck_micro_activity_type
  CHECK(activity_type IN ('GENERAL','RESPIRACION','MOVIMIENTO','RELAJACION','ESCRITURA','ATENCION_PLENA','DESCANSO_OCULAR'));

 -- En la 009 el CHECK de activity_preferences es inline y sin nombre: se eliminan los CHECK
 -- existentes sobre activity_type (sea cual sea su nombre generado) y se crea uno con nombre.
 FOR c IN SELECT conname FROM pg_constraint
          WHERE conrelid = 'activity_preferences'::regclass AND contype = 'c'
            AND pg_get_constraintdef(oid) ILIKE '%activity_type%'
 LOOP
  EXECUTE format('ALTER TABLE activity_preferences DROP CONSTRAINT %I', c.conname);
 END LOOP;
 ALTER TABLE activity_preferences ADD CONSTRAINT ck_activity_preference_type
  CHECK(activity_type IN ('GENERAL','RESPIRACION','MOVIMIENTO','RELAJACION','ESCRITURA','ATENCION_PLENA','DESCANSO_OCULAR'));
END $$;

-- Actividad de ejemplo para que la categoría no quede vacía.
INSERT INTO micro_activities(title, normalized_title, description, duration_minutes, active,
                             activity_type, created_at, updated_at)
SELECT 'Regla 20-20-20 para tus ojos', 'regla 20-20-20 para tus ojos',
       'Cada 20 minutos de pantalla, mira algo a unos 6 metros (20 pies) durante 20 segundos. Parpadea lentamente y relaja los hombros antes de volver a tu tarea.',
       2, TRUE, 'DESCANSO_OCULAR', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM micro_activities WHERE activity_type = 'DESCANSO_OCULAR')
  AND NOT EXISTS (SELECT 1 FROM micro_activities WHERE normalized_title = 'regla 20-20-20 para tus ojos');
