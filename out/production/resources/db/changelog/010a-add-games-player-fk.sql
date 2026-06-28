-- Remove orphaned games so FK creation succeeds
delete from games g
where not exists (
    select 1
    from players p
    where p.id = g.player_id
);

alter table games
add constraint fk_games_player_id
foreign key (player_id)
references players(id)
on delete cascade;

