-- ============================================================
-- fix_all_rls.sql  v2
-- Run ONCE in Supabase SQL Editor (safe to re-run).
--
-- Fixes in this version:
--   1. SECURITY DEFINER helper functions — break circular RLS
--      between notes ↔ note_collaborators (PostgreSQL 42P17)
--   2. accepted = TRUE  (boolean column, not a timestamp)
--   3. users SELECT policy → USING (true) so leaderboard sees all users
--   4. user_profiles auto-create trigger
--   5. bets RLS + trigger column fix (stake→amount, chosen_outcome→option_id)
-- ============================================================

-- ============================================================
-- PART 0: SECURITY DEFINER helper functions
--   These run as the table owner, bypassing RLS on inner queries.
--   This is the correct way to break circular RLS loops in Postgres.
-- ============================================================

-- Returns TRUE when the given user is an accepted collaborator on the note.
-- Called inside notes policies — does NOT re-enter notes RLS.
CREATE OR REPLACE FUNCTION public.is_note_collaborator(
    p_note_id  uuid,
    p_user_id  uuid
)
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
STABLE
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM   public.note_collaborators
        WHERE  note_id     = p_note_id
          AND  user_id     = p_user_id
          AND  accepted = TRUE
    );
$$;

-- Returns TRUE when the given user owns the given note.
-- Called inside note_collaborators policies — does NOT re-enter note_collaborators RLS.
CREATE OR REPLACE FUNCTION public.is_note_owner(
    p_note_id  uuid,
    p_user_id  uuid
)
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
STABLE
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM   public.notes
        WHERE  id      = p_note_id
          AND  user_id = p_user_id
    );
$$;

-- ============================================================
-- PART 1: public.users  RLS
-- ============================================================
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;

-- INSERT: user can only create their own row
DROP POLICY IF EXISTS "users can insert own row" ON public.users;
CREATE POLICY "users can insert own row"
  ON public.users
  FOR INSERT
  TO authenticated
  WITH CHECK (id = auth.uid());

-- SELECT: ALL authenticated users can read ALL rows (required for leaderboard).
-- Without this, each user only sees themselves and the leaderboard
-- returns a single row per session.
DROP POLICY IF EXISTS "users can read own row"   ON public.users;
DROP POLICY IF EXISTS "users can read all rows"  ON public.users;
CREATE POLICY "users can read all rows"
  ON public.users
  FOR SELECT
  TO authenticated
  USING (true);

-- UPDATE: user can only update their own row
DROP POLICY IF EXISTS "users can update own row" ON public.users;
CREATE POLICY "users can update own row"
  ON public.users
  FOR UPDATE
  TO authenticated
  USING     (id = auth.uid())
  WITH CHECK (id = auth.uid());

-- ============================================================
-- PART 2: public.user_profiles  RLS  (coin_balance lives here)
-- ============================================================
ALTER TABLE public.user_profiles ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "user_profiles insert own" ON public.user_profiles;
CREATE POLICY "user_profiles insert own"
  ON public.user_profiles
  FOR INSERT
  TO authenticated
  WITH CHECK (id = auth.uid());

DROP POLICY IF EXISTS "user_profiles select own" ON public.user_profiles;
CREATE POLICY "user_profiles select own"
  ON public.user_profiles
  FOR SELECT
  TO authenticated
  USING (id = auth.uid());

DROP POLICY IF EXISTS "user_profiles update own" ON public.user_profiles;
CREATE POLICY "user_profiles update own"
  ON public.user_profiles
  FOR UPDATE
  TO authenticated
  USING     (id = auth.uid())
  WITH CHECK (id = auth.uid());

-- Auto-create user_profiles row whenever a new users row is inserted
CREATE OR REPLACE FUNCTION public.create_user_profile()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
  INSERT INTO public.user_profiles (id)
  VALUES (NEW.id)
  ON CONFLICT (id) DO NOTHING;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_create_user_profile ON public.users;
CREATE TRIGGER trg_create_user_profile
  AFTER INSERT ON public.users
  FOR EACH ROW EXECUTE FUNCTION public.create_user_profile();

-- ============================================================
-- PART 3: public.notes  RLS
--   Uses is_note_collaborator() — no direct subquery into
--   note_collaborators — breaks the recursion loop.
-- ============================================================
ALTER TABLE public.notes ENABLE ROW LEVEL SECURITY;

-- Owner reads their notes
DROP POLICY IF EXISTS "notes owner select" ON public.notes;
CREATE POLICY "notes owner select"
  ON public.notes
  FOR SELECT
  TO authenticated
  USING (user_id = auth.uid());

-- Accepted collaborator reads shared notes
-- Uses SECURITY DEFINER helper — safe, no recursion
DROP POLICY IF EXISTS "notes collaborator select" ON public.notes;
CREATE POLICY "notes collaborator select"
  ON public.notes
  FOR SELECT
  TO authenticated
  USING (public.is_note_collaborator(id, auth.uid()));

-- Owner inserts their own notes
DROP POLICY IF EXISTS "notes owner insert" ON public.notes;
CREATE POLICY "notes owner insert"
  ON public.notes
  FOR INSERT
  TO authenticated
  WITH CHECK (user_id = auth.uid());

-- Owner + accepted editors can update
DROP POLICY IF EXISTS "notes owner update" ON public.notes;
CREATE POLICY "notes owner update"
  ON public.notes
  FOR UPDATE
  TO authenticated
  USING (
    user_id = auth.uid()
    OR (
      public.is_note_collaborator(id, auth.uid())
      -- role check done in app layer; policy just gates write access
    )
  );

-- Owner can delete
DROP POLICY IF EXISTS "notes owner delete" ON public.notes;
CREATE POLICY "notes owner delete"
  ON public.notes
  FOR DELETE
  TO authenticated
  USING (user_id = auth.uid());

-- ============================================================
-- PART 4: public.note_collaborators  RLS
--   Uses is_note_owner() — no direct subquery into notes — breaks loop.
-- ============================================================
ALTER TABLE public.note_collaborators ENABLE ROW LEVEL SECURITY;

-- Note owner OR the invited user can see collaborator rows
DROP POLICY IF EXISTS "collab owner select" ON public.note_collaborators;
CREATE POLICY "collab owner select"
  ON public.note_collaborators
  FOR SELECT
  TO authenticated
  USING (
    user_id    = auth.uid()
    OR invited_by = auth.uid()
    OR public.is_note_owner(note_id, auth.uid())
  );

-- Only the note owner can invite (insert collaborator rows)
-- Uses is_note_owner() SECURITY DEFINER — no recursion
DROP POLICY IF EXISTS "collab owner insert" ON public.note_collaborators;
CREATE POLICY "collab owner insert"
  ON public.note_collaborators
  FOR INSERT
  TO authenticated
  WITH CHECK (public.is_note_owner(note_id, auth.uid()));

-- Invited user can accept (set accepted = true); note owner can update too.
-- Also allows claiming a pending invite where user_id IS NULL but invited_email matches.
DROP POLICY IF EXISTS "collab accept or owner update" ON public.note_collaborators;
CREATE POLICY "collab accept or owner update"
  ON public.note_collaborators
  FOR UPDATE
  TO authenticated
  USING (
    user_id = auth.uid()
    OR public.is_note_owner(note_id, auth.uid())
    OR (
      user_id IS NULL
      AND invited_email = (SELECT email FROM public.users WHERE id = auth.uid())
    )
  );

-- Invited user can leave; note owner can revoke
DROP POLICY IF EXISTS "collab owner delete" ON public.note_collaborators;
CREATE POLICY "collab owner delete"
  ON public.note_collaborators
  FOR DELETE
  TO authenticated
  USING (
    user_id = auth.uid()
    OR public.is_note_owner(note_id, auth.uid())
  );

-- ============================================================
-- PART 5: public.bets  RLS
-- ============================================================
ALTER TABLE public.bets ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "bets user insert" ON public.bets;
CREATE POLICY "bets user insert"
  ON public.bets
  FOR INSERT
  TO authenticated
  WITH CHECK (user_id = auth.uid());

DROP POLICY IF EXISTS "bets user select" ON public.bets;
CREATE POLICY "bets user select"
  ON public.bets
  FOR SELECT
  TO authenticated
  USING (user_id = auth.uid());

-- ============================================================
-- PART 6: Fix stale trigger column names
--   Old trigger used:  NEW.stake, NEW.chosen_outcome
--   Actual columns are: amount, option_id
-- ============================================================
CREATE OR REPLACE FUNCTION public.update_option_stake()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
    UPDATE public.prediction_options
    SET total_stake = total_stake + NEW.amount
    WHERE id = NEW.option_id;
  ELSIF TG_OP = 'DELETE' THEN
    UPDATE public.prediction_options
    SET total_stake = GREATEST(0, total_stake - OLD.amount)
    WHERE id = OLD.option_id;
  ELSIF TG_OP = 'UPDATE' THEN
    UPDATE public.prediction_options
    SET total_stake = GREATEST(0, total_stake - OLD.amount) + NEW.amount
    WHERE id = NEW.option_id;
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_update_option_stake ON public.bets;
CREATE TRIGGER trg_update_option_stake
  AFTER INSERT OR UPDATE OR DELETE ON public.bets
  FOR EACH ROW EXECUTE FUNCTION public.update_option_stake();

-- ============================================================
-- DONE. Verify with:
--
-- SELECT tablename, policyname, cmd, qual, with_check
-- FROM   pg_policies
-- WHERE  tablename IN ('users','user_profiles','notes','note_collaborators','bets')
-- ORDER  BY tablename, cmd;
--
-- SELECT routine_name, security_type
-- FROM   information_schema.routines
-- WHERE  routine_name IN ('is_note_collaborator','is_note_owner',
--                         'create_user_profile','update_option_stake');
-- ============================================================
