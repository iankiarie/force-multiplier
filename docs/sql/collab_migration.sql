-- ============================================================
-- ForceMultiplier – Note Collaboration Migration
-- Run this in Supabase SQL Editor
-- ============================================================

-- 1. note_collaborators
-- ------------------------------------------------------------
create table if not exists note_collaborators (
    id          uuid primary key default gen_random_uuid(),
    note_id     uuid not null references notes(id) on delete cascade,
    user_id     uuid references auth.users(id) on delete cascade,
    invited_email text,                              -- set when invite is pending
    role        text not null default 'viewer'
                    check (role in ('owner', 'editor', 'commenter', 'viewer')),
    invited_by  uuid references auth.users(id),
    accepted    boolean not null default false,
    created_at  timestamptz not null default now(),
    unique (note_id, user_id)
);

create index if not exists idx_nc_note_id   on note_collaborators (note_id);
create index if not exists idx_nc_user_id   on note_collaborators (user_id);
create index if not exists idx_nc_inv_email on note_collaborators (invited_email);

-- 2. note_comments
-- ------------------------------------------------------------
create table if not exists note_comments (
    id          uuid primary key default gen_random_uuid(),
    note_id     uuid not null references notes(id) on delete cascade,
    user_id     uuid not null references auth.users(id) on delete cascade,
    content     text not null,
    resolved    boolean not null default false,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

create index if not exists idx_cmt_note_id on note_comments (note_id);
create index if not exists idx_cmt_user_id on note_comments (user_id);

-- auto-bump updated_at on edit
create or replace function update_comment_timestamp()
returns trigger language plpgsql as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

create trigger trg_comment_updated_at
    before update on note_comments
    for each row execute function update_comment_timestamp();

-- ============================================================
-- 3. Row Level Security
-- ============================================================

-- note_collaborators
alter table note_collaborators enable row level security;

-- Note owner or existing collaborator can read collaborator list
create policy "nc_select" on note_collaborators for select
    using (
        note_id in (select id from notes where user_id = auth.uid())
        or user_id = auth.uid()
        or invited_email = (select email from auth.users where id = auth.uid())
    );

-- Only note owner can add collaborators
create policy "nc_insert" on note_collaborators for insert
    with check (
        note_id in (select id from notes where user_id = auth.uid())
    );

-- Owner can update roles; invitee can accept their own invite
create policy "nc_update" on note_collaborators for update
    using (
        note_id in (select id from notes where user_id = auth.uid())
        or user_id = auth.uid()
    );

-- Owner can remove anyone; collaborator can remove themselves (leave)
create policy "nc_delete" on note_collaborators for delete
    using (
        note_id in (select id from notes where user_id = auth.uid())
        or user_id = auth.uid()
    );

-- note_comments
alter table note_comments enable row level security;

-- Readable by note owner + any collaborator
create policy "cmt_select" on note_comments for select
    using (
        note_id in (select id from notes where user_id = auth.uid())
        or note_id in (select note_id from note_collaborators where user_id = auth.uid())
    );

-- Insertable by owner, editor, or commenter
create policy "cmt_insert" on note_comments for insert
    with check (
        user_id = auth.uid()
        and (
            note_id in (select id from notes where user_id = auth.uid())
            or note_id in (
                select note_id from note_collaborators
                where user_id = auth.uid()
                and role in ('editor', 'commenter')
            )
        )
    );

-- Author can edit their own comments
create policy "cmt_update" on note_comments for update
    using (user_id = auth.uid());

-- Author or note owner can delete comments
create policy "cmt_delete" on note_comments for delete
    using (
        user_id = auth.uid()
        or note_id in (select id from notes where user_id = auth.uid())
    );

-- ============================================================
-- 4. Extend notes RLS to cover collaborators
-- (only needed if notes RLS isn't already open to collaborators)
-- ============================================================

-- Allow collaborators to read notes shared with them
create policy "notes_collab_select" on notes for select
    using (
        user_id = auth.uid()
        or id in (select note_id from note_collaborators where user_id = auth.uid())
    );

-- Allow editors to update note content
create policy "notes_collab_update" on notes for update
    using (
        user_id = auth.uid()
        or id in (
            select note_id from note_collaborators
            where user_id = auth.uid() and role = 'editor'
        )
    );

-- ============================================================
-- 5. Enable Realtime
-- ============================================================
alter publication supabase_realtime add table note_comments;
alter publication supabase_realtime add table note_collaborators;

-- ============================================================
-- 6. Helper: claim pending invites after login
-- Call this from your app after the user authenticates:
--   UPDATE note_collaborators
--   SET user_id = auth.uid(), accepted = true
--   WHERE invited_email = <user_email> AND user_id IS NULL
-- ============================================================
