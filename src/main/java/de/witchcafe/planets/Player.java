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
    
	/**
	 * Links the player to a game from the game infos, which only know the race. An existing link
	 * (e.g. from the history, with turns and rank) is kept.
	 */
	public void linkGame(Game game, String race) {
		PlayedIn playedIn = findLink(game);
		if (playedIn == null) {
			playedIn = new PlayedIn();
			playedIn.setGame(game);
			playedIn.setRace(normalizeRace(race));
			gamesPlayed.add(playedIn);
		}
	}

	/**
	 * Links the player to a game, updating an existing link to the same game
	 * instead of adding a second one.
	 */
	public void linkGame(Game game, String race, Integer starts, Integer ends, Integer finishrank) {
		PlayedIn playedIn = findLink(game);
		if (playedIn == null) {
			playedIn = new PlayedIn();
			playedIn.setGame(game);
		} else {
			// race is part of the hash code, so take it out before changing it
			gamesPlayed.remove(playedIn);
		}
		playedIn.setRace(normalizeRace(race));
		playedIn.setStarts(starts);
		playedIn.setEnds(ends);
		playedIn.setFinishrank(finishrank);
		gamesPlayed.add(playedIn);
	}

	private PlayedIn findLink(Game game) {
		if (gamesPlayed == null) {
			gamesPlayed = new HashSet<PlayedIn>();
		}
		return gamesPlayed.stream()
				.filter(existing -> existing.getGame() != null && game.getGameId().equals(existing.getGame().getGameId()))
				.findFirst().orElse(null);
	}

	/** the api sends race ids as "7.0" (game infos) or "7" (history); stored is "7" */
	static String normalizeRace(String race) {
		return race != null && race.endsWith(".0") ? race.substring(0, race.length() - 2) : race;
	}

	Set<PlayedIn> getGamesPlayed() {
		return gamesPlayed;
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
