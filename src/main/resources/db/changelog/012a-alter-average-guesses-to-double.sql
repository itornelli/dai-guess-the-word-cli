alter table players
alter column average_guesses type double precision using average_guesses::double precision,
alter column average_guesses set default 0.0;

