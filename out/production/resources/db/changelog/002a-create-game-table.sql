create table games (
    id serial primary key,
    hidden_word varchar(255),
    current_guesses varchar(255)[],
    player_id int not null
)