package de.witchcafe.planets;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.neo4j.repository.Neo4jRepository;

interface GamesRepository extends Neo4jRepository<Game, String> {

    // If you don't need a total row count, Slice is better than Page as it only performs a select query.
    // Page performs both a select and a count query.
    Slice<Game> findAllBy(Pageable pageable);
	Game findByGameId(String id);

}
