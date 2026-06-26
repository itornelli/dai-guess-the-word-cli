package wurdal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import wurdal.structures.Game;

@Repository
public interface GameRepository extends JpaRepository<Game, Integer> {
    @Query(value= """
    select * from games where player_id=:id order by id limit 1
""")
    public Game getByPlayerId(@Param("id") int id);
}
