-- Recreate trigger function to set is_in_game and game_id on players
create or replace function update_player_in_game()
returns trigger as $$
begin
  -- Set player as in-game and link to the new game
  update players
  set is_in_game = true,
      game_id = new.id
  where id = new.player_id;

  return new;
end;
$$ language plpgsql;

-- Drop and recreate trigger to ensure clean state
drop trigger if exists trg_update_player_in_game on games;

create trigger trg_update_player_in_game
after insert on games
for each row
execute function update_player_in_game();

