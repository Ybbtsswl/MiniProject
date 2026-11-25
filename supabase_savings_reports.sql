-- ERD tables; user ownership follows existing income/expense foreign keys.
begin;
create table public.saving_goal (
 goal_id uuid primary key default gen_random_uuid(),
 user_id uuid not null references auth.users(id) on delete cascade,
 goal_name text not null check (length(trim(goal_name)) between 1 and 100),
 description text not null default '',
 target_amount numeric(14,2) not null check (target_amount > 0),
 current_amount numeric(14,2) not null default 0 check (current_amount >= 0),
 remaining_amount numeric(14,2) generated always as (greatest(target_amount-current_amount,0)) stored,
 deadline date not null,
 progress_percentage numeric(12,2) generated always as (least(round(current_amount/target_amount*100,2),100)) stored,
 status text generated always as (case when current_amount >= target_amount then 'COMPLETED' else 'ACTIVE' end) stored,
 created_at timestamptz not null default now()
);
create table public.financial_report (
 report_id uuid primary key default gen_random_uuid(),
 user_id uuid not null references auth.users(id) on delete cascade,
 report_type text not null check (report_type in ('DAILY','WEEKLY','MONTHLY','YEARLY','CUSTOM')),
 start_date date not null default current_date,
 end_date date not null default current_date,
 total_income numeric(14,2) not null default 0,
 total_expenses numeric(14,2) not null default 0,
 total_savings numeric(14,2) generated always as (total_income-total_expenses) stored,
 generated_at timestamptz not null default now(),
 file_path text,
 check (end_date >= start_date)
);
alter table public.saving_goal enable row level security;
alter table public.financial_report enable row level security;
create policy saving_goal_read on public.saving_goal for select to authenticated using (user_id = (select auth.uid()));
create policy saving_goal_create on public.saving_goal for insert to authenticated with check (user_id = (select auth.uid()));
create policy saving_goal_update on public.saving_goal for update to authenticated using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
create policy financial_report_read on public.financial_report for select to authenticated using (user_id = (select auth.uid()));
create policy financial_report_create on public.financial_report for insert to authenticated with check (user_id = (select auth.uid()));
create policy financial_report_update on public.financial_report for update to authenticated using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
grant select, insert, update on public.saving_goal, public.financial_report to authenticated;
create index saving_goal_user_idx on public.saving_goal(user_id);
create index financial_report_user_dates_idx on public.financial_report(user_id,start_date,end_date);
-- Calculate report results on the server for precisely the requested period.
create function public.calculate_financial_report() returns trigger language plpgsql security invoker set search_path = public as $$
begin
 select coalesce(sum(amount),0) into new.total_income from public.income where user_id=new.user_id and income_date::date between new.start_date and new.end_date;
 select coalesce(sum(amount),0) into new.total_expenses from public.expenses where user_id=new.user_id and date::date between new.start_date and new.end_date;
 new.generated_at := now();
 return new;
end;
$$;
create trigger financial_report_calculate before insert or update of start_date,end_date on public.financial_report for each row execute function public.calculate_financial_report();
commit;
