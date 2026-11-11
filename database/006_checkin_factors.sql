-- SCRUM-27: factores seleccionables, sin inferirlos de notas privadas.
CREATE TABLE IF NOT EXISTS emotional_factors (
 id SMALLINT PRIMARY KEY,
 code VARCHAR(40) NOT NULL UNIQUE,
 label VARCHAR(80) NOT NULL
);
INSERT INTO emotional_factors(id,code,label) VALUES
(1,'ACADEMICO','Estudios'),(2,'TRABAJO','Trabajo'),(3,'FAMILIA','Familia'),
(4,'AMISTADES','Amistades'),(5,'SALUD','Salud'),(6,'DESCANSO','Descanso'),
(7,'FINANZAS','Finanzas'),(8,'OTRO','Otro')
ON CONFLICT (id) DO NOTHING;
CREATE TABLE IF NOT EXISTS checkin_factors (
 checkin_id BIGINT NOT NULL REFERENCES checkins(id) ON DELETE CASCADE,
 factor_id SMALLINT NOT NULL REFERENCES emotional_factors(id),
 PRIMARY KEY(checkin_id,factor_id)
);
CREATE INDEX IF NOT EXISTS ix_checkin_factors_factor ON checkin_factors(factor_id,checkin_id);
