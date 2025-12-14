package de.witchcafe.planets;

import com.google.gson.internal.LinkedTreeMap;

public class GameHistory {
	String gameid;
	Game game;

	public static GameHistory parse(LinkedTreeMap input) {
    	GameHistory gameHistory = new GameHistory();
    	gameHistory.gameid = input.get("gameid").toString();
    	gameHistory.game = Game.parse((LinkedTreeMap) input.get("game"));
    	return gameHistory;
	}

}
