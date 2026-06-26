package wurdal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import wurdal.structures.Player;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Integer> {
}
