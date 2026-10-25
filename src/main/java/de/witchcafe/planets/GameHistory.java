package de.witchcafe.planets;

import java.time.LocalDate;

import com.google.gson.internal.LinkedTreeMap;

public class GameHistory {
	String gameid;
	Game game;
	String raceid;
	Integer finishrank;
	Integer turnjoined;
	Integer turnleft;
	Integer gameStatus;
	LocalDate dateended;

	public static GameHistory parse(LinkedTreeMap input) {
    	GameHistory gameHistory = new GameHistory();
    	gameHistory.gameid = input.get("gameid").toString().replace(".0", "");
    	LinkedTreeMap gameData = (LinkedTreeMap) input.get("game");
    	gameHistory.game = Game.parse(gameData);
    	gameHistory.raceid = input.get("raceid").toString().replace(".0", "");
    	gameHistory.finishrank = ((Number) input.get("finishrank")).intValue();
    	gameHistory.turnjoined = ((Number) input.get("turnjoined")).intValue();
    	gameHistory.turnleft = ((Number) input.get("turnleft")).intValue();
    	gameHistory.gameStatus = ((Number) gameData.get("status")).intValue();
    	// "0001-01-01T00:00:00" for running games
    	String dateended = gameData.get("dateended").toString();
    	if (!dateended.startsWith("0001")) {
    		gameHistory.dateended = LocalDate.parse(dateended.substring(0, 10));
    	}
    	return gameHistory;
	}

	/** finished game (status 3) with this player on first place */
	public boolean isWon() {
		return finishrank != null && finishrank == 1 && gameStatus != null && gameStatus == 3;
	}

	public String getGameid() {
		return gameid;
	}

	public Game getGame() {
		return game;
	}

	public Integer getFinishrank() {
		return finishrank;
	}

	public LocalDate getDateended() {
		return dateended;
	}

}
