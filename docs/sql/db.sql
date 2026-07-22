-- WARNING: This schema is for context only and is not meant to be run.
-- Table order and constraints may not be valid for execution.

CREATE TABLE public.organizations (
  id uuid NOT NULL DEFAULT uuid_generate_v4(),
  name text NOT NULL,
  slug text NOT NULL UNIQUE,
  created_at timestamp with time zone DEFAULT now(),
  CONSTRAINT organizations_pkey PRIMARY KEY (id)
);
CREATE TABLE public.users (
  id uuid NOT NULL,
  email text NOT NULL UNIQUE,
  username text UNIQUE,
  full_name text,
  organization_id uuid,
  avatar_url text,
  points integer DEFAULT 0,
  is_active boolean DEFAULT true,
  created_at timestamp with time zone DEFAULT now(),
  updated_at timestamp with time zone DEFAULT now(),
  CONSTRAINT users_pkey PRIMARY KEY (id),
  CONSTRAINT users_id_fkey FOREIGN KEY (id) REFERENCES auth.users(id),
  CONSTRAINT users_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES public.organizations(id)
);
CREATE TABLE public.roles (
  id text NOT NULL,
  description text,
  CONSTRAINT roles_pkey PRIMARY KEY (id)
);
CREATE TABLE public.user_roles (
  user_id uuid NOT NULL,
  role_id text NOT NULL,
  CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id),
  CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id),
  CONSTRAINT user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(id)
);
CREATE TABLE public.predictions (
  id uuid NOT NULL DEFAULT uuid_generate_v4(),
  title text NOT NULL,
  description text,
  category text,
  created_by uuid,
  ends_at timestamp with time zone NOT NULL,
  resolved_at timestamp with time zone,
  winning_option_id uuid,
  status text DEFAULT 'ACTIVE'::text,
  created_at timestamp with time zone DEFAULT now(),
  CONSTRAINT predictions_pkey PRIMARY KEY (id),
  CONSTRAINT predictions_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id),
  CONSTRAINT fk_winning_option FOREIGN KEY (winning_option_id) REFERENCES public.prediction_options(id)
);
CREATE TABLE public.prediction_options (
  id uuid NOT NULL DEFAULT uuid_generate_v4(),
  prediction_id uuid,
  option_text text NOT NULL,
  total_stake integer DEFAULT 0,
  CONSTRAINT prediction_options_pkey PRIMARY KEY (id),
  CONSTRAINT prediction_options_prediction_id_fkey FOREIGN KEY (prediction_id) REFERENCES public.predictions(id)
);
CREATE TABLE public.bets (
  id uuid NOT NULL DEFAULT uuid_generate_v4(),
  user_id uuid,
  prediction_id uuid,
  option_id uuid,
  amount integer NOT NULL,
  created_at timestamp with time zone DEFAULT now(),
  CONSTRAINT bets_pkey PRIMARY KEY (id),
  CONSTRAINT bets_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id),
  CONSTRAINT bets_prediction_id_fkey FOREIGN KEY (prediction_id) REFERENCES public.predictions(id),
  CONSTRAINT bets_option_id_fkey FOREIGN KEY (option_id) REFERENCES public.prediction_options(id)
);
CREATE TABLE public.transactions (
  id uuid NOT NULL DEFAULT uuid_generate_v4(),
  user_id uuid,
  amount integer NOT NULL,
  type USER-DEFINED NOT NULL,
  reference_id uuid,
  description text,
  created_at timestamp with time zone DEFAULT now(),
  CONSTRAINT transactions_pkey PRIMARY KEY (id),
  CONSTRAINT transactions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id)
);
CREATE TABLE public.notes (
  id uuid NOT NULL DEFAULT uuid_generate_v4(),
  user_id uuid,
  title text NOT NULL,
  content text,
  preview text,
  created_at timestamp with time zone DEFAULT now(),
  updated_at timestamp with time zone DEFAULT now(),
  CONSTRAINT notes_pkey PRIMARY KEY (id),
  CONSTRAINT notes_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id)
);
CREATE TABLE public.tags (
  id uuid NOT NULL DEFAULT uuid_generate_v4(),
  name text NOT NULL UNIQUE,
  CONSTRAINT tags_pkey PRIMARY KEY (id)
);
CREATE TABLE public.note_tags (
  note_id uuid NOT NULL,
  tag_id uuid NOT NULL,
  CONSTRAINT note_tags_pkey PRIMARY KEY (note_id, tag_id),
  CONSTRAINT note_tags_note_id_fkey FOREIGN KEY (note_id) REFERENCES public.notes(id),
  CONSTRAINT note_tags_tag_id_fkey FOREIGN KEY (tag_id) REFERENCES public.tags(id)
);
CREATE TABLE public.badges (
  id text NOT NULL,
  name text NOT NULL,
  description text,
  icon_url text,
  CONSTRAINT badges_pkey PRIMARY KEY (id)
);
CREATE TABLE public.user_badges (
  user_id uuid NOT NULL,
  badge_id text NOT NULL,
  awarded_at timestamp with time zone DEFAULT now(),
  CONSTRAINT user_badges_pkey PRIMARY KEY (user_id, badge_id),
  CONSTRAINT user_badges_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id),
  CONSTRAINT user_badges_badge_id_fkey FOREIGN KEY (badge_id) REFERENCES public.badges(id)
);
CREATE TABLE public.audit_logs (
  id uuid NOT NULL DEFAULT uuid_generate_v4(),
  user_id uuid,
  action USER-DEFINED NOT NULL,
  entity_type text NOT NULL,
  entity_id uuid NOT NULL,
  payload jsonb,
  created_at timestamp with time zone DEFAULT now(),
  CONSTRAINT audit_logs_pkey PRIMARY KEY (id),
  CONSTRAINT audit_logs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id)
);
CREATE TABLE public.feature_flags (
  key text NOT NULL,
  is_enabled boolean DEFAULT false,
  description text,
  updated_at timestamp with time zone DEFAULT now(),
  CONSTRAINT feature_flags_pkey PRIMARY KEY (key)
);
CREATE TABLE public.note_collaborators (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  note_id uuid NOT NULL,
  user_id uuid,
  invited_email text,
  role text NOT NULL DEFAULT 'viewer'::text CHECK (role = ANY (ARRAY['owner'::text, 'editor'::text, 'commenter'::text, 'viewer'::text])),
  invited_by uuid,
  accepted boolean NOT NULL DEFAULT false,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT note_collaborators_pkey PRIMARY KEY (id),
  CONSTRAINT note_collaborators_note_id_fkey FOREIGN KEY (note_id) REFERENCES public.notes(id),
  CONSTRAINT note_collaborators_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id),
  CONSTRAINT note_collaborators_invited_by_fkey FOREIGN KEY (invited_by) REFERENCES auth.users(id)
);
CREATE TABLE public.note_comments (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  note_id uuid NOT NULL,
  user_id uuid NOT NULL,
  content text NOT NULL,
  resolved boolean NOT NULL DEFAULT false,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT note_comments_pkey PRIMARY KEY (id),
  CONSTRAINT note_comments_note_id_fkey FOREIGN KEY (note_id) REFERENCES public.notes(id),
  CONSTRAINT note_comments_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id)
);
CREATE TABLE public.user_profiles (
  id uuid NOT NULL,
  coin_balance integer NOT NULL DEFAULT 100,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT user_profiles_pkey PRIMARY KEY (id),
  CONSTRAINT user_profiles_id_fkey FOREIGN KEY (id) REFERENCES auth.users(id)
);