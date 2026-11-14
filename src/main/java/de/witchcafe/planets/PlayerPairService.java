package de.witchcafe.planets;

import java.util.List;
import java.util.Map;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

/**
 * Finds players who played together, i.e. both have a playedIn edge to the same game.
 * Only covers players stored in the database.
 */
@Service
public class PlayerPairService {

	public record PlayerPair(String playerA, String accountidA, String playerB, String accountidB, long games) {
	}

	public record SharedGame(String gameId, String name, String statusname, String turn, String datecreated,
			String raceA, String raceB, Long finishrankA, Long finishrankB) {
	}

	private final Neo4jClient neo4jClient;

	PlayerPairService(Neo4jClient neo4jClient) {
		this.neo4jClient = neo4jClient;
	}

	/** all pairs with at least minGames shared games, most shared games first */
	public List<PlayerPair> findPairs(int minGames) {
		return List.copyOf(neo4jClient.query("""
				MATCH (a:Player)-[:playedIn]->(g:Game)<-[:playedIn]-(b:Player)
				WHERE a.name < b.name
				WITH a, b, count(DISTINCT g) AS games WHERE games >= $minGames
				RETURN a.name AS playerA, a.accountid AS accountidA, b.name AS playerB, b.accountid AS accountidB, games
				ORDER BY games DESC, playerA, playerB""")
				.bind(minGames).to("minGames")
				.fetchAs(PlayerPair.class)
				.mappedBy((types, row) -> new PlayerPair(row.get("playerA").asString(), row.get("accountidA").asString(null),
						row.get("playerB").asString(), row.get("accountidB").asString(null), row.get("games").asLong()))
				.all());
	}

	/** games both players played in, newest first */
	public List<SharedGame> findSharedGames(PlayerPair pair) {
		return List.copyOf(neo4jClient.query("""
				MATCH (a:Player {name: $playerA})-[ra:playedIn]->(g:Game)<-[rb:playedIn]-(b:Player {name: $playerB})
				WITH g, head(collect(ra)) AS ra, head(collect(rb)) AS rb
				RETURN g.gameId AS gameId, g.name AS name, g.statusname AS statusname, g.turn AS turn,
				       g.datecreated AS datecreated, ra.race AS raceA, rb.race AS raceB,
				       ra.finishrank AS finishrankA, rb.finishrank AS finishrankB
				ORDER BY g.datecreated DESC""")
				.bindAll(Map.of("playerA", pair.playerA(), "playerB", pair.playerB()))
				.fetchAs(SharedGame.class)
				.mappedBy((types, row) -> new SharedGame(row.get("gameId").asString(null), row.get("name").asString(null),
						row.get("statusname").asString(null), row.get("turn").asString(null),
						row.get("datecreated").asString(null), row.get("raceA").asString(null), row.get("raceB").asString(null),
						row.get("finishrankA").isNull() ? null : row.get("finishrankA").asLong(),
						row.get("finishrankB").isNull() ? null : row.get("finishrankB").asLong()))
				.all());
	}
}
