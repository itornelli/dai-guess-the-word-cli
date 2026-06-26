package wurdal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import wurdal.structures.Game;

@Repository
public interface GameRepository extends JpaRepository<Game, Integer> {
    @Query
    public Game getByPlayerId(Integer id);
}
