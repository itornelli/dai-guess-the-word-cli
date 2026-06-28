alter table players
add column if not exists games_lost integer not null default 0,
add column if not exists average_guesses decimal not null default 0;

