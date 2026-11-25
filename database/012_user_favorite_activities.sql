-- SCRUM-16 / T53: migrate existing favorites without dropping associations.
-- Apply after database/010_favorite_micro_activities.sql when upgrading an older environment.
-- Review a backup and run inside a maintenance window. Never automatically merge conflicting tables.
DO $$
BEGIN
    IF to_regclass('public.favorite_micro_activities') IS NOT NULL
       AND to_regclass('public.user_favorite_activities') IS NOT NULL THEN
        RAISE EXCEPTION 'Both favorite tables exist; inspect and reconcile manually before migration';
    END IF;

    IF to_regclass('public.favorite_micro_activities') IS NOT NULL THEN
        ALTER TABLE public.favorite_micro_activities RENAME TO user_favorite_activities;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.user_favorite_activities (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    activity_id BIGINT NOT NULL REFERENCES public.micro_activities(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_favorite_activities_user_activity UNIQUE (user_id, activity_id)
);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema='public' AND table_name='user_favorite_activities'
          AND column_name='micro_activity_id'
    ) THEN
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema='public' AND table_name='user_favorite_activities'
              AND column_name='activity_id'
        ) THEN
            RAISE EXCEPTION 'Both activity columns exist; resolve manually before migration';
        END IF;
        ALTER TABLE public.user_favorite_activities
            RENAME COLUMN micro_activity_id TO activity_id;
    END IF;

    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname='uk_favorite_micro_activity_user_activity'
          AND conrelid='public.user_favorite_activities'::regclass
    ) THEN
        ALTER TABLE public.user_favorite_activities
            RENAME CONSTRAINT uk_favorite_micro_activity_user_activity
            TO uk_user_favorite_activities_user_activity;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_favorite_activities_user_activity
    ON public.user_favorite_activities (user_id, activity_id);

CREATE INDEX IF NOT EXISTS idx_user_favorites_user_created
    ON public.user_favorite_activities (user_id, created_at DESC);

-- Verify retained rows with:
-- SELECT count(*) FROM public.user_favorite_activities;
-- SELECT user_id, activity_id, count(*) FROM public.user_favorite_activities
--   GROUP BY user_id, activity_id HAVING count(*) > 1;
