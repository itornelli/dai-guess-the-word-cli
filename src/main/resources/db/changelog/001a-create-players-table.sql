create table players (
    id              serial          primary key,
    name            varchar(255)    not null unique,
    games_won       integer         not null default 0,
    games_lost      integer         not null default 0,
    average_guesses double precision not null default 0.0,
    is_in_game      boolean         not null default false
);
