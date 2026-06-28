do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'uk_players_name'
    ) then
        alter table players
        add constraint uk_players_name unique (name);
    end if;
end $$;

