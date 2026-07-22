-- ── fix_bets_triggers.sql ──────────────────────────────────────────
-- Fixes live DB trigger functions that still reference old column names:
--   OLD name   → NEW name
--   stake      → amount
--   chosen_outcome → option_id  (now a UUID FK to prediction_options.id)
--   settled    → (dropped — resolve_prediction uses winning_option_id instead)
--
-- Run this in Supabase SQL editor ONCE.
-- Safe to re-run (all functions use CREATE OR REPLACE).
-- ────────────────────────────────────────────────────────────────────

-- ── 1. deduct_bet_coins trigger ──────────────────────────────────────
-- Fires BEFORE INSERT on bets.  Deducts NEW.amount coins from user_profiles.
create or replace function deduct_bet_coins()
returns trigger language plpgsql security definer as $$
declare
    v_balance int;
begin
    -- Ensure profile row exists (balance defaults to 100)
    insert into user_profiles (id, coin_balance)
    values (new.user_id, 100)
    on conflict (id) do nothing;

    select coin_balance into v_balance
    from user_profiles where id = new.user_id;

    if v_balance < new.amount then
        raise exception 'Insufficient coins. Balance: %, Required: %',
            v_balance, new.amount;
    end if;

    update user_profiles
    set coin_balance = coin_balance - new.amount,
        updated_at   = now()
    where id = new.user_id;

    return new;
end;
$$;

drop trigger if exists trg_deduct_bet_coins on bets;
create trigger trg_deduct_bet_coins
    before insert on bets
    for each row execute function deduct_bet_coins();


-- ── 2. update_option_stake trigger ───────────────────────────────────
-- Fires AFTER INSERT on bets.  Increments total_stake on the chosen option
-- (matched by option_id UUID, not option_text).
create or replace function update_option_stake()
returns trigger language plpgsql as $$
begin
    update prediction_options
    set total_stake = total_stake + new.amount
    where id = new.option_id;          -- option_id is a UUID FK
    return new;
end;
$$;

drop trigger if exists trg_update_option_stake on bets;
create trigger trg_update_option_stake
    after insert on bets
    for each row execute function update_option_stake();


-- ── 3. resolve_prediction stored procedure ───────────────────────────
-- Takes a prediction_id + the UUID of the winning option.
-- Pays out proportionally to all bets on that option.
-- OLD signature used option_text; new signature uses option UUID.
create or replace function resolve_prediction(
    p_prediction_id    uuid,
    p_winning_option_id uuid
)
returns void language plpgsql security definer as $$
declare
    v_created_by   uuid;
    v_status       text;
    v_total_pool   int;
    v_winning_pool int;
    v_payout       int;
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

    -- Total coins wagered on this prediction
    select coalesce(sum(amount), 0) into v_total_pool
    from bets where prediction_id = p_prediction_id;

    -- Coins wagered on the winning option
    select coalesce(sum(amount), 0) into v_winning_pool
    from bets
    where prediction_id = p_prediction_id
      and option_id     = p_winning_option_id;

    -- Pay out winners proportionally
    if v_winning_pool > 0 then
        for v_bet in
            select * from bets
            where prediction_id = p_prediction_id
              and option_id     = p_winning_option_id
        loop
            v_payout := (v_bet.amount * v_total_pool) / v_winning_pool;
            update user_profiles
            set coin_balance = coin_balance + v_payout,
                updated_at   = now()
            where id = v_bet.user_id;
        end loop;
    end if;

    -- Mark prediction resolved
    update predictions
    set status           = 'RESOLVED',
        winning_option_id = p_winning_option_id,
        resolved_at       = now()
    where id = p_prediction_id;
end;
$$;
