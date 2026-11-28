-- Shared savings schema for the Savings Management group UI.
-- Run as the Supabase project owner in the SQL editor.

create extension if not exists pgcrypto;

create table if not exists public.saving_groups (
    group_id uuid primary key default gen_random_uuid(),
    owner_id uuid not null references auth.users(id) on delete cascade,
    group_name text not null check (length(trim(group_name)) > 0),
    target_amount numeric(14, 2) not null check (target_amount > 0),
    start_date date not null,
    deadline date not null check (deadline >= start_date),
    created_at timestamptz not null default now()
);

create table if not exists public.saving_group_members (
    membership_id uuid primary key default gen_random_uuid(),
    group_id uuid not null references public.saving_groups(group_id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    target_share numeric(14, 2) not null default 0 check (target_share >= 0),
    goal_label text not null default 'Shared goal',
    joined_at timestamptz not null default now(),
    unique (group_id, user_id)
);

create table if not exists public.saving_group_invitations (
    invitation_id uuid primary key default gen_random_uuid(),
    group_id uuid not null references public.saving_groups(group_id) on delete cascade,
    inviter_id uuid not null references auth.users(id) on delete cascade,
    invitee_id uuid not null references auth.users(id) on delete cascade,
    status text not null default 'PENDING'
        check (status in ('PENDING', 'ACCEPTED', 'DECLINED')),
    created_at timestamptz not null default now(),
    responded_at timestamptz,
    unique (group_id, invitee_id),
    check (inviter_id <> invitee_id)
);

create table if not exists public.saving_group_contributions (
    contribution_id uuid primary key default gen_random_uuid(),
    group_id uuid not null references public.saving_groups(group_id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    amount numeric(14, 2) not null check (amount > 0),
    contributed_on date not null default current_date,
    created_at timestamptz not null default now()
);

create index if not exists saving_group_members_user_idx
    on public.saving_group_members(user_id);
create index if not exists saving_group_invitations_invitee_idx
    on public.saving_group_invitations(invitee_id, status);
create index if not exists saving_group_contributions_group_idx
    on public.saving_group_contributions(group_id, user_id);

-- Security-definer helpers avoid recursive RLS checks between group tables.
create or replace function public.owns_saving_group(target_group_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1 from public.saving_groups
        where group_id = target_group_id and owner_id = auth.uid()
    );
$$;

create or replace function public.can_view_saving_group(target_group_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select
        public.owns_saving_group(target_group_id)
        or exists (
            select 1 from public.saving_group_members
            where group_id = target_group_id and user_id = auth.uid()
        )
        or exists (
            select 1 from public.saving_group_invitations
            where group_id = target_group_id and invitee_id = auth.uid()
        );
$$;

revoke all on function public.owns_saving_group(uuid) from public;
revoke all on function public.can_view_saving_group(uuid) from public;
grant execute on function public.owns_saving_group(uuid) to authenticated;
grant execute on function public.can_view_saving_group(uuid) to authenticated;

-- A newly created challenge always includes its owner as its first member.
create or replace function public.add_saving_group_owner()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    insert into public.saving_group_members(group_id, user_id, goal_label)
    values (new.group_id, new.owner_id, 'Group owner')
    on conflict (group_id, user_id) do nothing;
    return new;
end;
$$;

drop trigger if exists saving_group_add_owner on public.saving_groups;
create trigger saving_group_add_owner
after insert on public.saving_groups
for each row execute function public.add_saving_group_owner();

-- Accepting an invitation atomically creates the membership row.
create or replace function public.apply_saving_group_invitation_response()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    if new.status <> old.status then
        new.responded_at := now();
    end if;
    if new.status = 'ACCEPTED' and old.status <> 'ACCEPTED' then
        insert into public.saving_group_members(group_id, user_id)
        values (new.group_id, new.invitee_id)
        on conflict (group_id, user_id) do nothing;
    end if;
    return new;
end;
$$;

drop trigger if exists saving_group_accept_invitation
    on public.saving_group_invitations;
create trigger saving_group_accept_invitation
before update on public.saving_group_invitations
for each row execute function public.apply_saving_group_invitation_response();

alter table public.saving_groups enable row level security;
alter table public.saving_group_members enable row level security;
alter table public.saving_group_invitations enable row level security;
alter table public.saving_group_contributions enable row level security;

drop policy if exists saving_groups_select on public.saving_groups;
create policy saving_groups_select on public.saving_groups
for select to authenticated
using (public.can_view_saving_group(group_id));

drop policy if exists saving_groups_insert on public.saving_groups;
create policy saving_groups_insert on public.saving_groups
for insert to authenticated
with check (owner_id = auth.uid());

drop policy if exists saving_groups_update on public.saving_groups;
create policy saving_groups_update on public.saving_groups
for update to authenticated
using (owner_id = auth.uid()) with check (owner_id = auth.uid());

drop policy if exists saving_groups_delete on public.saving_groups;
create policy saving_groups_delete on public.saving_groups
for delete to authenticated using (owner_id = auth.uid());

drop policy if exists saving_group_members_select on public.saving_group_members;
create policy saving_group_members_select on public.saving_group_members
for select to authenticated
using (public.can_view_saving_group(group_id));

drop policy if exists saving_group_invitations_select on public.saving_group_invitations;
create policy saving_group_invitations_select on public.saving_group_invitations
for select to authenticated
using (
    inviter_id = auth.uid()
    or invitee_id = auth.uid()
    or public.owns_saving_group(group_id)
);

drop policy if exists saving_group_invitations_insert on public.saving_group_invitations;
create policy saving_group_invitations_insert on public.saving_group_invitations
for insert to authenticated
with check (inviter_id = auth.uid() and public.owns_saving_group(group_id));

drop policy if exists saving_group_invitations_update on public.saving_group_invitations;
create policy saving_group_invitations_update on public.saving_group_invitations
for update to authenticated
using (invitee_id = auth.uid())
with check (invitee_id = auth.uid() and status in ('ACCEPTED', 'DECLINED'));

drop policy if exists saving_group_contributions_select on public.saving_group_contributions;
create policy saving_group_contributions_select on public.saving_group_contributions
for select to authenticated
using (public.can_view_saving_group(group_id));

drop policy if exists saving_group_contributions_insert on public.saving_group_contributions;
create policy saving_group_contributions_insert on public.saving_group_contributions
for insert to authenticated
with check (
    user_id = auth.uid()
    and exists (
        select 1 from public.saving_group_members member
        where member.group_id = saving_group_contributions.group_id
          and member.user_id = auth.uid()
    )
);

-- Authenticated users can invite real accounts without exposing auth.users.
create or replace view public.saving_group_user_directory
with (security_invoker = false)
as
select "User_id" as id, full_name, email
from public.profiles;

revoke all on public.saving_group_user_directory from public, anon;
grant select on public.saving_group_user_directory to authenticated;

grant select, insert, update, delete on public.saving_groups to authenticated;
grant select on public.saving_group_members to authenticated;
grant select, insert, update on public.saving_group_invitations to authenticated;
grant select, insert on public.saving_group_contributions to authenticated;

