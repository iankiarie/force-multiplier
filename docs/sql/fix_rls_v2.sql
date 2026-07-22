-- ============================================================
-- FIX: RLS for note_comments + note_collaborators
-- Run this ONCE in the Supabase SQL editor.
-- It drops all existing policies on both tables and creates
-- correct non-recursive ones.
-- ============================================================

-- ── 1. Enable RLS (in case it isn't on yet) ──────────────────
ALTER TABLE public.note_comments     ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.note_collaborators ENABLE ROW LEVEL SECURITY;

-- ── 2. Drop ALL existing policies on both tables ─────────────
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT policyname, tablename
        FROM   pg_policies
        WHERE  schemaname = 'public'
          AND  tablename  IN ('note_comments', 'note_collaborators')
    LOOP
        EXECUTE format('DROP POLICY IF EXISTS %I ON public.%I',
                       r.policyname, r.tablename);
    END LOOP;
END $$;

-- ── 3. SECURITY DEFINER helpers (break recursion) ─────────────
-- These run as the table owner and do NOT trigger RLS checks.

CREATE OR REPLACE FUNCTION public.fm_user_owns_note(p_note_id uuid)
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
STABLE
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.notes
        WHERE  id = p_note_id
          AND  user_id = auth.uid()
    );
$$;

CREATE OR REPLACE FUNCTION public.fm_user_is_collaborator(p_note_id uuid)
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
STABLE
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.note_collaborators
        WHERE  note_id = p_note_id
          AND  user_id = auth.uid()
    );
$$;

-- ── 4. note_collaborators policies ───────────────────────────

-- Owner can see all collaborators on their notes
CREATE POLICY "nc_owner_select" ON public.note_collaborators
    FOR SELECT USING (fm_user_owns_note(note_id));

-- Collaborator can see their own row
CREATE POLICY "nc_self_select" ON public.note_collaborators
    FOR SELECT USING (user_id = auth.uid());

-- Owner can invite people (insert)
CREATE POLICY "nc_owner_insert" ON public.note_collaborators
    FOR INSERT WITH CHECK (
        fm_user_owns_note(note_id)
        AND invited_by = auth.uid()
    );

-- Owner can update roles / acceptance
CREATE POLICY "nc_owner_update" ON public.note_collaborators
    FOR UPDATE USING (fm_user_owns_note(note_id));

-- Collaborator can accept their own invite (set accepted=true)
CREATE POLICY "nc_self_accept" ON public.note_collaborators
    FOR UPDATE USING (user_id = auth.uid());

-- Owner can remove collaborators
CREATE POLICY "nc_owner_delete" ON public.note_collaborators
    FOR DELETE USING (fm_user_owns_note(note_id));

-- ── 5. note_comments policies ─────────────────────────────────

-- Owner or collaborator can read comments
CREATE POLICY "ncomm_select" ON public.note_comments
    FOR SELECT USING (
        fm_user_owns_note(note_id)
        OR fm_user_is_collaborator(note_id)
    );

-- Owner or collaborator can post comments
CREATE POLICY "ncomm_insert" ON public.note_comments
    FOR INSERT WITH CHECK (
        user_id = auth.uid()
        AND (
            fm_user_owns_note(note_id)
            OR fm_user_is_collaborator(note_id)
        )
    );

-- Author can edit their own comment
CREATE POLICY "ncomm_update" ON public.note_comments
    FOR UPDATE USING (user_id = auth.uid());

-- Author or owner can delete
CREATE POLICY "ncomm_delete" ON public.note_comments
    FOR DELETE USING (
        user_id = auth.uid()
        OR fm_user_owns_note(note_id)
    );
