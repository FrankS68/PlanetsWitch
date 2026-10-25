package de.witchcafe.planets;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;

@SpringBootTest
class GamesServiceTests {

	private Game game;

	@Autowired
	GamesService gameService;

	@Test
	void testAccountHistory() {
        // String accountid = "25372"; 30826 
        String accountid = "176327";
		try {
			ArrayList<GameHistory> gameHistories = gameService.getAccountHistory(accountid);
	        assertNotEquals(0, gameHistories.size(),"Result seems empty");
	        gameHistories.forEach(gameHistory -> {
	        	game = gameHistory.game;
	        	try{
	        		game.setScores(gameService.getScoresByGameid(game.getGameId().toString()));
	        		ArrayList<Score> scores = gameHistory.game.getScores();
	        		assertNotNull(scores);
	        		game.setEvents(gameService.getEventsByGameid(game.getGameId().toString()));
	        		ArrayList<Event> events = gameHistory.game.getEvents();
	        		assertNotNull(events);
		        	System.out.println(gameHistory.game);
	        	}
	        	catch (Exception exc) {
	    			System.out.println(String.format("Error printing scores for %s:\n%s", 
	    					gameHistory.gameid,exc.getMessage()));
	        	}
	        });
		}
		catch (JsonSyntaxException exc) {
			fail(exc.getMessage());
		}
        
    }
	
	@Test
	void testLoadTopOfficers() {
		ArrayList<Officer> officers = gameService.loadTopOfficers();
		assertNotEquals(0, officers.size(),"Result seems empty");
		officers.forEach(officer -> {
			assertNotNull(officer.getPlayer(), String.format("no player for officer %s", officer));
			System.out.println(officer);
		});
	}

	@Test
	void testLoadGameInfo() {
		gameService.getGameInfo("665581");
	}
	
	@Test
	void testLoadGameEvents() {
		LinkedTreeMap<String, Object> gamesEventResult = gameService.getLinkedTreeMapFromUrl(gameService.getPlanetsTemplate().eventsByGameid("642972"));
		assertNotNull(gamesEventResult);
		ArrayList<LinkedTreeMap<String, Object>> gamesEvents = (ArrayList<LinkedTreeMap<String, Object>>) gamesEventResult.get("events");
		gamesEvents.forEach(value -> {
			System.out.println(String.format("%s",value));
		});
	}

	@Test
	void testActiveGamesByUsername() {
        // String accountid = "25372"; 30826 
        String accountid = "176327";
		try {
			ArrayList<Game> games = gameService.getActiveGamesByUsername("sualtim");
	        assertNotEquals(0, games.size(),"Result seems empty");
	        System.out.println(games.iterator().next().getGameId());
		}
		catch (JsonSyntaxException exc) {
			fail(exc.getMessage());
		}
        
    }

}
