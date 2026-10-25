package de.witchcafe.planets;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PlanetsWitchRestAPI {
	
	@Autowired
	GamesService gamesService;

	@Autowired
	GameArchiveService gameArchiveService;

	@GetMapping("/gameinfos/{gameid}")
    public String getInfosByGameId(@PathVariable("gameid") String gameId) {
		gamesService.getGameInfo(gameId);
        return "Loaded infos for: " + gameId;
    }
	
	@GetMapping("/history/{accountId}")
    public String getHistoryByAccountId(@PathVariable("accountId") String accountId) {
		gamesService.getAccountHistory(accountId.replace(".0", ""));
        return "Loaded history for: " + accountId;
    }
	
	@GetMapping("/archive/{gameid}")
    public String getArchiveByGameId(@PathVariable("gameid") String gameId) throws IOException {
		Path archive = gameArchiveService.downloadCompleteGame(gameId);
		Set<Integer> turns = new TreeSet<>();
		Set<Integer> players = new TreeSet<>();
		GameArchiveService.forEachTurn(gameId, archive, turn -> {
			turns.add(turn.turn());
			players.add(turn.playerSlot());
		});
        return String.format("Loaded archive for %s: %s turns, players %s (%s)", gameId, turns.size(), players, archive);
    }

	@GetMapping("/archive/finished/{username}")
    public String getFinishedArchivesByUserName(@PathVariable("username") String username) {
		List<Path> archives = gameArchiveService.downloadFinishedGames(username);
        return String.format("Loaded %s archives for %s", archives.size(), username);
    }

	@GetMapping("/topofficers/load")
    public String loadTopOfficers() {
		List<Officer> officers = gamesService.loadTopOfficers();
        return String.format("Loaded %s top officers", officers.size());
    }

	@Autowired
	TopOfficerGamesService topOfficerGamesService;

	@GetMapping("/topofficers/games")
    public Map<String, Object> loadTopOfficerGames(@RequestParam(name = "days", defaultValue = "30") int days) {
		return topOfficerGamesService.loadTopOfficerGames(days);
    }

	@GetMapping("/active/{accountId}")
    public String getActivesByUserName(@PathVariable("accountId") String accountId) {
		gamesService.getActiveGamesByUsername(accountId);
        return "Loaded actives for: " + accountId;
    }
}
