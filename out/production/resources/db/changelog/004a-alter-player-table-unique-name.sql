alter table players add constraint uk_players_name unique (name);
alter table players alter column game_id set default null;