create table games (
    id               serial          primary key,
    hidden_word      varchar(255)    not null,
    current_guesses  varchar(255)[]  not null default array[]::varchar(255)[],
    player_id        integer         not null references players(id) on delete cascade,
    status           integer         not null default 1
);
