package de.witchcafe.planets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Loads the games of all top officers into the graph (via their account history) and
 * downloads the archives of games they recently won, up to a size limit for the archive directory.
 */
@Service
public class TopOfficerGamesService {

	private final GamesService gamesService;
	private final GameArchiveService gameArchiveService;
	private final OfficerRepository officerRepository;
	private final Path archiveDir;
	private final long maxArchiveBytes;

	TopOfficerGamesService(
			GamesService gamesService,
			GameArchiveService gameArchiveService,
			OfficerRepository officerRepository,
			@Value("${planetswitch.archive-dir:data/games}") String archiveDir,
			@Value("${planetswitch.archive-max-gb:10}") long maxArchiveGb) {
		this.gamesService = gamesService;
		this.gameArchiveService = gameArchiveService;
		this.officerRepository = officerRepository;
		this.archiveDir = Path.of(archiveDir);
		this.maxArchiveBytes = maxArchiveGb * 1024 * 1024 * 1024;
	}

	/**
	 * @param winDays only games won within the last winDays days get their archive downloaded
	 * @return summary for the topofficer report (counts and the recent wins with their archive state)
	 */
	public Map<String, Object> loadTopOfficerGames(int winDays) {
		LocalDate since = LocalDate.now().minusDays(winDays);
		TreeSet<String> accountIds = new TreeSet<String>();
		officerRepository.findAll().forEach(officer -> accountIds.add(officer.getAccountid()));

		TreeSet<String> games = new TreeSet<String>();
		List<Map<String, Object>> wins = new ArrayList<Map<String, Object>>();
		List<String> failedAccounts = new ArrayList<String>();
		for (String accountId : accountIds) {
			ArrayList<GameHistory> history;
			try {
				history = gamesService.getAccountHistory(accountId);
			}
			catch (Exception exc) {
				System.err.println(String.format("loading history of account %s failed: %s", accountId, exc.getMessage()));
				failedAccounts.add(accountId);
				continue;
			}
			if (history == null) {
				failedAccounts.add(accountId);
				continue;
			}
			for (GameHistory gameHistory : history) {
				games.add(gameHistory.getGameid());
				if (gameHistory.isWon() && gameHistory.getDateended() != null && !gameHistory.getDateended().isBefore(since)) {
					Map<String, Object> win = new LinkedHashMap<String, Object>();
					win.put("accountid", accountId);
					win.put("username", usernameOf(accountId));
					win.put("gameid", gameHistory.getGameid());
					win.put("name", gameHistory.getGame().getName());
					win.put("race", gameHistory.raceid);
					win.put("dateended", gameHistory.getDateended().toString());
					win.put("archive", loadArchive(gameHistory.getGameid()));
					wins.add(win);
				}
			}
		}

		Map<String, Object> summary = new LinkedHashMap<String, Object>();
		summary.put("accounts", accountIds.size());
		summary.put("failedAccounts", failedAccounts);
		summary.put("games", games.size());
		summary.put("winsSince", since.toString());
		summary.put("wins", wins);
		summary.put("archiveDirBytes", directorySize());
		summary.put("archiveMaxBytes", maxArchiveBytes);
		return summary;
	}

	/** @return "present", "downloaded", "skipped: size limit" or the error */
	private String loadArchive(String gameId) {
		if (Files.exists(gameArchiveService.getArchivePath(gameId))) {
			return "present";
		}
		if (directorySize() >= maxArchiveBytes) {
			return "skipped: size limit";
		}
		try {
			gameArchiveService.downloadCompleteGame(gameId);
			return "downloaded";
		}
		catch (Exception exc) {
			System.err.println(String.format("Error loading archive for game %s: %s", gameId, exc.getMessage()));
			return "error: " + exc.getMessage();
		}
	}

	private String usernameOf(String accountId) {
		return officerRepository.findAll().stream()
				.filter(officer -> accountId.equals(officer.getAccountid()))
				.map(Officer::getUsername).findFirst().orElse(accountId);
	}

	private long directorySize() {
		if (!Files.isDirectory(archiveDir)) {
			return 0;
		}
		try (Stream<Path> files = Files.list(archiveDir)) {
			return files.filter(Files::isRegularFile).mapToLong(file -> {
				try {
					return Files.size(file);
				} catch (IOException e) {
					return 0;
				}
			}).sum();
		}
		catch (IOException exc) {
			return 0;
		}
	}
}
