update games
set current_guesses = ARRAY[]::varchar(255)[]
where current_guesses is null;

alter table games
alter column current_guesses set default ARRAY[]::varchar(255)[];

alter table games
alter column current_guesses set not null;

