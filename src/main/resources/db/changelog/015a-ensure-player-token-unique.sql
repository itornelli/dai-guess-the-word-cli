do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'uk_players_token'
    ) then
        alter table players
        add constraint uk_players_token unique (token);
    end if;
end $$;

