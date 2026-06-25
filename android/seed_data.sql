-- Supabase SQL Seeding Script
-- ALIGNED WITH YOUR PROVIDED SCHEMA
-- User UID: a57eaf21-df96-493e-8a37-e971306272af

-- 1. Ensure User Profile exists in public.users
INSERT INTO public.users (id, email, username, full_name, points, is_active)
VALUES
    ('a57eaf21-df96-493e-8a37-e971306272af', 'ian@forcemultiplier.co', 'iankiarie', 'Ian Kiarie', 3240, TRUE)
ON CONFLICT (id) DO UPDATE SET
    points = EXCLUDED.points,
    full_name = EXCLUDED.full_name,
    username = EXCLUDED.username;

-- 2. Seed Predictions and Options
-- We use Common Table Expressions (CTE) to link options to predictions correctly
DELETE FROM public.prediction_options;
DELETE FROM public.predictions;

WITH inserted_predictions AS (
    INSERT INTO public.predictions (title, description, ends_at, created_by, category, status)
    VALUES
        ('Atlas Q4 Growth', 'Will Atlas achieve > 20% growth in Q4?', '2024-12-31 23:59:59+00', 'a57eaf21-df96-493e-8a37-e971306272af', 'Strategy', 'ACTIVE'),
        ('Zero Bug Sprint', 'Will the team achieve zero bugs this sprint?', '2024-11-15 23:59:59+00', 'a57eaf21-df96-493e-8a37-e971306272af', 'Engineering', 'ACTIVE')
    RETURNING id, title
)
INSERT INTO public.prediction_options (option_text, prediction_id)
SELECT 'Yes', id FROM inserted_predictions
UNION ALL
SELECT 'No', id FROM inserted_predictions;

-- 3. Seed Knowledge Vault Notes
DELETE FROM public.note_tags;
DELETE FROM public.notes;
DELETE FROM public.tags;

-- Insert Tags
INSERT INTO public.tags (id, name) VALUES
    (gen_random_uuid(), 'Strategy'),
    (gen_random_uuid(), 'AI'),
    (gen_random_uuid(), 'Meetings'),
    (gen_random_uuid(), 'Design'),
    (gen_random_uuid(), 'Learning'),
    (gen_random_uuid(), 'Android'),
    (gen_random_uuid(), 'Ideas'),
    (gen_random_uuid(), 'Gamification');

-- Insert Notes and link them to tags
WITH note1 AS (
    INSERT INTO public.notes (title, content, preview, user_id)
    VALUES ('Product Strategy 2024', 'Key focus areas for the upcoming year include AI integration and community-driven roadmaps...', 'AI integration and community roadmaps', 'a57eaf21-df96-493e-8a37-e971306272af')
    RETURNING id
),
note2 AS (
    INSERT INTO public.notes (title, content, preview, user_id)
    VALUES ('Meeting Notes: Sprint 45', 'Discussed the new design system implementation and resolved the navigation bottleneck...', 'Design system and navigation fixes', 'a57eaf21-df96-493e-8a37-e971306272af')
    RETURNING id
)
INSERT INTO public.note_tags (note_id, tag_id)
SELECT note1.id, tags.id FROM note1, public.tags WHERE tags.name IN ('Strategy', 'AI')
UNION ALL
SELECT note2.id, tags.id FROM note2, public.tags WHERE tags.name IN ('Meetings', 'Design');
