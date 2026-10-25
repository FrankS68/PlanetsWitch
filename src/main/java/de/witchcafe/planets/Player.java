package de.witchcafe.planets;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("Player")
public class Player {
	@Id
    @GeneratedValue(generatorClass = GeneratedValue.InternalIdGenerator.class) // Oder lass GeneratedValue weg, wenn du accountid nutzt
    private String id;
    private String name;
    private String accountid;

    @Relationship(type = "playedIn", direction = Relationship.Direction.OUTGOING)
    private Set<PlayedIn> gamesPlayed;
	private Date historyLoaded;
	private Date activeGamesLoaded;
    
    public void addPlayerIn(Game game,Integer starts,Integer ends,String race) {
    	PlayedIn playedIn = new PlayedIn();
    	playedIn.setGame(game);
    	playedIn.setStarts(starts);
    	playedIn.setEnds(ends);
    	playedIn.setRace(race);
    	if (gamesPlayed == null) {
    		gamesPlayed = new HashSet<PlayedIn>();
    	}
    	gamesPlayed.add(playedIn);
    }

	/**
	 * Links the player to a game, updating an existing link to the same game
	 * instead of adding a second one.
	 */
	public void linkGame(Game game, String race, Integer starts, Integer ends, Integer finishrank) {
		if (gamesPlayed == null) {
			gamesPlayed = new HashSet<PlayedIn>();
		}
		PlayedIn playedIn = gamesPlayed.stream()
				.filter(existing -> existing.getGame() != null && game.getGameId().equals(existing.getGame().getGameId()))
				.findFirst().orElse(null);
		if (playedIn == null) {
			playedIn = new PlayedIn();
			playedIn.setGame(game);
		} else {
			// race is part of the hash code, so take it out before changing it
			gamesPlayed.remove(playedIn);
		}
		playedIn.setRace(race);
		playedIn.setStarts(starts);
		playedIn.setEnds(ends);
		playedIn.setFinishrank(finishrank);
		gamesPlayed.add(playedIn);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAccountid() {
		return accountid;
	}

	public void setAccountId(String accountid) {
		this.accountid = accountid.replace(".0", "");
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getHistoryLoaded() {
		return historyLoaded;
	}

	public void setHistoryLoaded(Date date) {
		this.historyLoaded = date;
	}

	public Date getActiveGamesLoaded() {
		return activeGamesLoaded;
	}

	public void setActiveGamesLoaded(Date date) {
		this.activeGamesLoaded = date;
	}

}
