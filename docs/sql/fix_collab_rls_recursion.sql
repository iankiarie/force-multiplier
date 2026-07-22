-- ============================================================
-- Fix: infinite recursion in RLS between notes ↔ note_collaborators
-- Error: 42P17 "infinite recursion detected in policy for relation note_collaborators"
--
-- Root cause (cycle):
--   nc_select (note_collaborators)  → subquery on notes
--   notes_collab_select (notes)     → subquery on note_collaborators
--   → PostgreSQL evaluates both RLS simultaneously → stack overflow
--
-- Fix: SECURITY DEFINER helper functions bypass RLS on their target table,
--      breaking the cycle without changing any business logic.
-- ============================================================

-- ── Step 1: Drop all policies that have cross-table references ──────────────

drop policy if exists "nc_select"            on note_collaborators;
drop policy if exists "nc_insert"            on note_collaborators;
drop policy if exists "nc_update"            on note_collaborators;
drop policy if exists "nc_delete"            on note_collaborators;

drop policy if exists "notes_collab_select"  on notes;
drop policy if exists "notes_collab_update"  on notes;

drop policy if exists "cmt_select"           on note_comments;
drop policy if exists "cmt_insert"           on note_comments;

-- ── Step 2: SECURITY DEFINER helpers (each bypasses RLS on its own table) ──

-- Returns true if auth.uid() owns the given note (reads notes WITHOUT RLS)
create or replace function public.fm_user_owns_note(p_note_id uuid)
returns boolean
language sql
security definer
stable
set search_path = public
as $$
  select exists (
    select 1 from public.notes
    where id = p_note_id
      and user_id = auth.uid()
  );
$$;

-- Returns true if auth.uid() is an accepted collaborator on the note
-- (reads note_collaborators WITHOUT RLS)
create or replace function public.fm_user_is_collaborator(p_note_id uuid)
returns boolean
language sql
security definer
stable
set search_path = public
as $$
  select exists (
    select 1 from public.note_collaborators
    where note_id = p_note_id
      and user_id = auth.uid()
  );
$$;

-- Returns true if auth.uid() is a collaborator with one of the given roles
create or replace function public.fm_user_has_collab_role(p_note_id uuid, p_roles text[])
returns boolean
language sql
security definer
stable
set search_path = public
as $$
  select exists (
    select 1 from public.note_collaborators
    where note_id = p_note_id
      and user_id = auth.uid()
      and role = any(p_roles)
  );
$$;

-- ── Step 3: Recreate note_collaborators policies (no direct notes subquery) ─

-- Owner, self, or invitee can see the row
-- NOTE: use auth.email() NOT (select email from auth.users where id = auth.uid())
--       auth.users is not readable by the authenticated role (42501).
create policy "nc_select" on note_collaborators for select
    using (
        fm_user_owns_note(note_id)
        or user_id = auth.uid()
        or invited_email = auth.email()
    );

-- Only note owner can add collaborators
create policy "nc_insert" on note_collaborators for insert
    with check (
        fm_user_owns_note(note_id)
    );

-- Owner can update roles; invitee can accept their own invite
create policy "nc_update" on note_collaborators for update
    using (
        fm_user_owns_note(note_id)
        or user_id = auth.uid()
    );

-- Owner can remove anyone; collaborator can remove themselves (leave)
create policy "nc_delete" on note_collaborators for delete
    using (
        fm_user_owns_note(note_id)
        or user_id = auth.uid()
    );

-- ── Step 4: Recreate notes policies (no direct note_collaborators subquery) ─

-- Owner or any collaborator can read
create policy "notes_collab_select" on notes for select
    using (
        user_id = auth.uid()
        or fm_user_is_collaborator(id)
    );

-- Owner or editor can update
create policy "notes_collab_update" on notes for update
    using (
        user_id = auth.uid()
        or fm_user_has_collab_role(id, array['editor'])
    );

-- ── Step 5: Recreate note_comments policies ──────────────────────────────────

-- Readable by note owner or any collaborator
create policy "cmt_select" on note_comments for select
    using (
        fm_user_owns_note(note_id)
        or fm_user_is_collaborator(note_id)
    );

-- Insertable by owner, editor, or commenter — must be posting as themselves
create policy "cmt_insert" on note_comments for insert
    with check (
        user_id = auth.uid()
        and (
            fm_user_owns_note(note_id)
            or fm_user_has_collab_role(note_id, array['editor', 'commenter'])
        )
    );

-- ============================================================
-- Verification query (run after applying — should return 0 rows on a good state)
-- ============================================================
-- select policyname, tablename from pg_policies
-- where tablename in ('notes', 'note_collaborators', 'note_comments')
-- order by tablename, policyname;
