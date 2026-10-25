package de.witchcafe.planets;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerRepository extends Neo4jRepository<Player, String> {
    List<Player> findByName(String name);
    Player findByAccountid(String accountId);
    Boolean existsByAccountid(String accountId);
    
    // @Query("MERGE (p:Player {name: :#{#game.name}}) ON CREATE SET g += :#{#game} RETURN p")
    // Player saveOrUpdate(Player player);
}