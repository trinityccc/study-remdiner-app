-- Study Reminder table + Row Level Security (RLS)
-- Run in Supabase SQL Editor.

create extension if not exists pgcrypto;

create table if not exists public.study_reminders (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  description text null,
  type text not null,
  priority text not null,
  due_date timestamp null,
  status text not null default 'pending',
  created_at timestamp not null default now()
);

alter table public.study_reminders
  drop constraint if exists study_reminders_type_check,
  drop constraint if exists study_reminders_priority_check,
  drop constraint if exists study_reminders_status_check,
  add constraint study_reminders_type_check
    check (type in ('assignment', 'exam', 'revision')),
  add constraint study_reminders_priority_check
    check (priority in ('high', 'medium', 'low')),
  add constraint study_reminders_status_check
    check (status in ('pending', 'completed'));

-- Helpful indexes
create index if not exists study_reminders_user_id_idx on public.study_reminders(user_id);
create index if not exists study_reminders_due_date_idx on public.study_reminders(due_date);
create index if not exists study_reminders_created_at_idx on public.study_reminders(created_at desc);

-- Enable Row Level Security
alter table public.study_reminders enable row level security;
alter table public.study_reminders force row level security;

-- Users can only access their own reminders
drop policy if exists "study_reminders_select_own" on public.study_reminders;
create policy "study_reminders_select_own"
  on public.study_reminders
  for select
  using (user_id = auth.uid());

drop policy if exists "study_reminders_insert_own" on public.study_reminders;
create policy "study_reminders_insert_own"
  on public.study_reminders
  for insert
  with check (user_id = auth.uid());

drop policy if exists "study_reminders_update_own" on public.study_reminders;
create policy "study_reminders_update_own"
  on public.study_reminders
  for update
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

drop policy if exists "study_reminders_delete_own" on public.study_reminders;
create policy "study_reminders_delete_own"
  on public.study_reminders
  for delete
  using (user_id = auth.uid());

