create table players (
    id serial primary key,
    name varchar(255) not null,
    games_won int default 0,
    game_id int,
    is_in_game bool default false
)