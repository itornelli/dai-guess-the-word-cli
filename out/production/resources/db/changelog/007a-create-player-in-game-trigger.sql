-- Create trigger function to update player isInGame and delete old games
create or replace function update_player_in_game()
returns trigger as $$
begin
  -- Update player's isInGame to true
  update players set is_in_game = true where id = new.player_id;

  -- Delete any other games for this player (keep only the newest)
  delete from games
  where player_id = new.player_id
    and id != new.id;

  return new;
end;
$$ language plpgsql;

-- Create trigger on games insert
create trigger trg_update_player_in_game
after insert on games
for each row
execute function update_player_in_game();

