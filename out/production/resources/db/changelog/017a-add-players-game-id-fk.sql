-- Enforce relationship from players.game_id -> games.id
alter table players
add constraint fk_players_game_id
foreign key (game_id)
references games(id)
on delete set null;

