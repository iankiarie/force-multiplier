-- ============================================================
-- FIX: RLS for bets table
-- Run AFTER fix_rls_v2.sql (or run both together)
-- ============================================================

ALTER TABLE public.bets ENABLE ROW LEVEL SECURITY;

-- Drop all existing bets policies
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN SELECT policyname FROM pg_policies
             WHERE schemaname = 'public' AND tablename = 'bets'
    LOOP
        EXECUTE format('DROP POLICY IF EXISTS %I ON public.bets', r.policyname);
    END LOOP;
END $$;

-- Users can see their own bets
CREATE POLICY "bets_select_own" ON public.bets
    FOR SELECT USING (user_id = auth.uid());

-- Users can place bets on ACTIVE predictions
CREATE POLICY "bets_insert_own" ON public.bets
    FOR INSERT WITH CHECK (
        user_id = auth.uid()
        AND EXISTS (
            SELECT 1 FROM public.predictions p
            WHERE  p.id = prediction_id
              AND  p.status = 'ACTIVE'
        )
    );
