package de.witchcafe.planets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlayerTests {

	@Test
	void testGameInfoKeepsHistoryLink() {
		Game game = new Game();
		game.setGameId("635801");
		Player player = new Player();
		player.linkGame(game, "7", 1, 0, 4);
		player.linkGame(game, "7.0");

		assertEquals(1, player.getGamesPlayed().size());
		PlayedIn playedIn = player.getGamesPlayed().iterator().next();
		assertEquals("7", playedIn.getRace());
		assertEquals(4, playedIn.getFinishrank());
	}

	@Test
	void testHistoryUpdatesGameInfoLink() {
		Game game = new Game();
		game.setGameId("635801");
		Player player = new Player();
		player.linkGame(game, "7.0");
		player.linkGame(game, "7", 1, 0, 4);

		assertEquals(1, player.getGamesPlayed().size());
		assertEquals("7", player.getGamesPlayed().iterator().next().getRace());
	}
}
