-- ============================================================
-- ForceMultiplier – Betting System Migration (idempotent re-run)
-- Safe to run even if tables/constraints already exist.
-- ============================================================

-- ── 1. Tables (IF NOT EXISTS — safe) ─────────────────────────
create table if not exists user_profiles (
    id            uuid primary key references auth.users(id) on delete cascade,
    coin_balance  int not null default 100,
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now()
);

create table if not exists predictions (
    id                uuid primary key default gen_random_uuid(),
    title             text not null,
    description       text,
    category          text,
    status            text not null default 'ACTIVE'
                          check (status in ('ACTIVE', 'RESOLVED', 'CANCELLED')),
    created_by        uuid not null references auth.users(id) on delete cascade,
    ends_at           timestamptz not null,
    winning_option_id uuid,
    resolved_at       timestamptz,
    created_at        timestamptz not null default now()
);

create table if not exists prediction_options (
    id            uuid primary key default gen_random_uuid(),
    prediction_id uuid not null references predictions(id) on delete cascade,
    option_text   text not null,
    total_stake   int not null default 0,
    created_at    timestamptz not null default now()
);

-- FK: add only if missing
do $$ begin
    if not exists (
        select 1 from pg_constraint where conname = 'fk_winning_option'
    ) then
        alter table predictions
            add constraint fk_winning_option
            foreign key (winning_option_id) references prediction_options(id)
            deferrable initially deferred;
    end if;
end $$;

create index if not exists idx_opts_prediction_id on prediction_options (prediction_id);

create table if not exists bets (
    id             uuid primary key default gen_random_uuid(),
    prediction_id  uuid not null references predictions(id) on delete cascade,
    user_id        uuid not null references auth.users(id) on delete cascade,
    chosen_outcome text not null,
    stake          int not null check (stake > 0),
    settled        boolean not null default false,
    created_at     timestamptz not null default now(),
    settled_at     timestamptz,
    unique (prediction_id, user_id)
);

create index if not exists idx_bets_prediction_id on bets (prediction_id);
create index if not exists idx_bets_user_id       on bets (user_id);

-- ── 2. Trigger functions (CREATE OR REPLACE — always safe) ───

create or replace function handle_new_user()
returns trigger language plpgsql security definer as $$
begin
    insert into public.user_profiles (id, coin_balance)
    values (new.id, 100)
    on conflict (id) do nothing;
    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function handle_new_user();

create or replace function deduct_bet_coins()
returns trigger language plpgsql security definer as $$
declare
    v_balance int;
begin
    insert into user_profiles (id, coin_balance)
    values (new.user_id, 100)
    on conflict (id) do nothing;

    select coin_balance into v_balance
    from user_profiles where id = new.user_id;

    if v_balance < new.stake then
        raise exception 'Insufficient coins. Balance: %, Required: %',
            v_balance, new.stake;
    end if;

    update user_profiles
    set coin_balance = coin_balance - new.stake,
        updated_at   = now()
    where id = new.user_id;

    return new;
end;
$$;

drop trigger if exists trg_deduct_bet_coins on bets;
create trigger trg_deduct_bet_coins
    before insert on bets
    for each row execute function deduct_bet_coins();

create or replace function update_option_stake()
returns trigger language plpgsql as $$
begin
    update prediction_options
    set total_stake = total_stake + new.stake
    where prediction_id = new.prediction_id
      and option_text   = new.chosen_outcome;
    return new;
end;
$$;

drop trigger if exists trg_update_option_stake on bets;
create trigger trg_update_option_stake
    after insert on bets
    for each row execute function update_option_stake();

create or replace function resolve_prediction(
    p_prediction_id       uuid,
    p_winning_option_text text
)
returns void language plpgsql security definer as $$
declare
    v_created_by   uuid;
    v_status       text;
    v_total_pool   int;
    v_winning_pool int;
    v_payout       int;
    v_winning_opt  uuid;
    v_bet          record;
begin
    select created_by, status
    into v_created_by, v_status
    from predictions where id = p_prediction_id;

    if v_created_by is null then
        raise exception 'Prediction not found';
    end if;
    if v_created_by != auth.uid() then
        raise exception 'Only the creator can resolve this prediction';
    end if;
    if v_status != 'ACTIVE' then
        raise exception 'Prediction is not active (status: %)', v_status;
    end if;

    select coalesce(sum(stake), 0) into v_total_pool
    from bets where prediction_id = p_prediction_id;

    select coalesce(sum(stake), 0) into v_winning_pool
    from bets
    where prediction_id  = p_prediction_id
      and chosen_outcome = p_winning_option_text;

    if v_winning_pool > 0 then
        for v_bet in
            select * from bets
            where prediction_id  = p_prediction_id
              and chosen_outcome = p_winning_option_text
              and not settled
        loop
            v_payout := (v_bet.stake * v_total_pool) / v_winning_pool;
            update user_profiles
            set coin_balance = coin_balance + v_payout, updated_at = now()
            where id = v_bet.user_id;

            update bets set settled = true, settled_at = now()
            where id = v_bet.id;
        end loop;
    end if;

    update bets set settled = true, settled_at = now()
    where prediction_id = p_prediction_id and not settled;

    select id into v_winning_opt
    from prediction_options
    where prediction_id = p_prediction_id
      and option_text   = p_winning_option_text
    limit 1;

    update predictions
    set status            = 'RESOLVED',
        winning_option_id = v_winning_opt,
        resolved_at       = now()
    where id = p_prediction_id;
end;
$$;

-- ── 3. RLS (drop-then-recreate — idempotent) ─────────────────

alter table user_profiles    enable row level security;
alter table predictions      enable row level security;
alter table prediction_options enable row level security;
alter table bets             enable row level security;

-- user_profiles
drop policy if exists "up_select_own"  on user_profiles;
drop policy if exists "up_insert_own"  on user_profiles;
create policy "up_select_own" on user_profiles for select using (id = auth.uid());
create policy "up_insert_own" on user_profiles for insert with check (id = auth.uid());

-- predictions
drop policy if exists "preds_select_all" on predictions;
drop policy if exists "preds_insert_own" on predictions;
drop policy if exists "preds_update_own" on predictions;
drop policy if exists "preds_delete_own" on predictions;
create policy "preds_select_all" on predictions for select using (true);
create policy "preds_insert_own" on predictions for insert with check (created_by = auth.uid());
create policy "preds_update_own" on predictions for update using (created_by = auth.uid());
create policy "preds_delete_own" on predictions for delete using (created_by = auth.uid());

-- prediction_options
drop policy if exists "opts_select_all" on prediction_options;
drop policy if exists "opts_insert_own" on prediction_options;
create policy "opts_select_all" on prediction_options for select using (true);
create policy "opts_insert_own" on prediction_options for insert
    with check (
        prediction_id in (select id from predictions where created_by = auth.uid())
    );

-- bets
drop policy if exists "bets_select_own" on bets;
drop policy if exists "bets_insert_own" on bets;
create policy "bets_select_own" on bets for select using (user_id = auth.uid());
create policy "bets_insert_own" on bets for insert
    with check (
        user_id = auth.uid()
        and (select status from predictions where id = prediction_id) = 'ACTIVE'
    );

-- ── 4. Realtime (ignore if already added) ────────────────────
do $$ begin
    alter publication supabase_realtime add table predictions;
exception when others then null; end $$;
do $$ begin
    alter publication supabase_realtime add table prediction_options;
exception when others then null; end $$;
do $$ begin
    alter publication supabase_realtime add table bets;
exception when others then null; end $$;
do $$ begin
    alter publication supabase_realtime add table user_profiles;
exception when others then null; end $$;

-- ── 5. Backfill existing users ────────────────────────────────
insert into user_profiles (id, coin_balance)
select id, 100 from auth.users
on conflict (id) do nothing;
