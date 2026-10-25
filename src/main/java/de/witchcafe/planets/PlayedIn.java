package de.witchcafe.planets;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
public class PlayedIn {
    @RelationshipId
    private String id;

    // Diese Attribute landen direkt auf der Relation in Neo4j
    private String race;
    private Integer starts;
    private Integer ends;
    private Integer finishrank;

    @TargetNode
    private Game game; // Der Knoten, zu dem die Relation führt

	public String getRace() {
		return race;
	}

	public void setRace(String race) {
		this.race = race;
	}

	public Integer getStarts() {
		return starts;
	}

	public void setStarts(Integer starts) {
		this.starts = starts;
	}

	public Integer getEnds() {
		return ends;
	}

	public void setEnds(Integer ends) {
		this.ends = ends;
	}

	public Integer getFinishrank() {
		return finishrank;
	}

	public void setFinishrank(Integer finishrank) {
		this.finishrank = finishrank;
	}

	public Game getGame() {
		return game;
	}

	public void setGame(Game game) {
		this.game = game;
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(race, game == null ? null : game.gameId);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		PlayedIn alsoPlayedIn = (PlayedIn) obj;
		boolean result = java.util.Objects.equals(race, alsoPlayedIn.race);
		result &= java.util.Objects.equals(game.gameId, alsoPlayedIn.game.gameId);
		result &= java.util.Objects.equals(starts, alsoPlayedIn.starts);
		result &= java.util.Objects.equals(ends, alsoPlayedIn.ends);
		return result;
	}
	
}