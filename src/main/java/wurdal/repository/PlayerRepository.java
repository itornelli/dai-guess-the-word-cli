package wurdal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import wurdal.structures.Player;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Integer> {
    Optional<Player> findFirstByName(String name);
    Optional<Player> findFirstByToken(UUID token);
}
