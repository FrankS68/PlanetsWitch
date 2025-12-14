package de.witchcafe.planets;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.google.gson.JsonSyntaxException;

class BareBonePlanetsTests {

	private Game game;

	PlanetsRestTemplate pTemplate = new PlanetsRestTemplate();
	@Test
	void test() {
		ResponseEntity<String> gamesResult = pTemplate.getForEntity("http://api.planets.nu/games/list?username=Lomize", String.class);
		assertNotNull(gamesResult);
		System.out.println(gamesResult);
	}
	
	@Test
	void testActiveGamesByUsername() {
		
		GamesService gamesService;

        // String username = "lomize"; "moulliez d" "gráinne"
        String username = "hawkeye418";
        username = "gráinne";
		try {
			ArrayList<Game> result = pTemplate.getActiveGamesByUsername(username);
	        assertNotEquals(0, result.size(),"Result seems empty");
	        result.forEach(game -> {System.out.println(game.name);});
		}
		catch (JsonSyntaxException exc) {
			fail(exc.getMessage());
		}
        
    }

	@Test
	void testAccountHistory() {

        // String accountid = "25372"; 30826 
        String accountid = "176327";
		try {
			ArrayList<GameHistory> gameHistories = pTemplate.getAccountHistory(accountid);
	        assertNotEquals(0, gameHistories.size(),"Result seems empty");
	        gameHistories.forEach(gameHistory -> {
	        	try{
	        		gameHistory.game.getScores();
	        		gameHistory.game.getEvents();
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

}
