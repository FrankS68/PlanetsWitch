package de.witchcafe.planets;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;

/**
 * Loads complete archives of finished games via /game/loadall
 * (https://help.planets.nu/API-game-loadall), keeps them as game{id}.zip in a local
 * directory and parses the contained turn files. The archive holds all turns except the last.
 */
@Service
public class GameArchiveService {

	private static final Pattern TURN_FILE = Pattern.compile("player(\\d+)-turn(\\d+)\\.trn");
	private static final Pattern BROKEN_EMPTY_LIST = Pattern.compile("(\":\\s*)\\]");

	private final GamesService gamesService;
	private final GamesRepository gamesRepository;
	private final Path archiveDir;
	PlanetsRestTemplate pTemplate = new PlanetsRestTemplate();

	GameArchiveService(
			GamesService gamesService,
			GamesRepository gamesRepository,
			@Value("${planetswitch.archive-dir:data/games}") String archiveDir) {
		this.gamesService = gamesService;
		this.gamesRepository = gamesRepository;
		this.archiveDir = Path.of(archiveDir);
	}

	public Path getArchivePath(String gameId) {
		return archiveDir.resolve(String.format("game%s.zip", gameId));
	}

	/**
	 * Downloads the archive unless it is already stored locally.
	 */
	public Path downloadCompleteGame(String gameId) throws IOException {
		Path archive = getArchivePath(gameId);
		if (Files.exists(archive)) {
			return archive;
		}
		System.out.println(String.format("downloading archive for game %s to %s", gameId, archive.toAbsolutePath()));
		pTemplate.downloadToFile(pTemplate.loadCompleteGame(gameId), archive);
		Game game = gamesRepository.findByGameId(gameId);
		if (game != null) {
			game.setArchiveLoaded(true);
			gamesRepository.save(game);
		}
		return archive;
	}

	/**
	 * Downloads the archives of all finished games of the given player. Games that
	 * cannot be loaded (e.g. "game not finished" for games on hold) are skipped.
	 */
	public List<Path> downloadFinishedGames(String username) {
		List<Path> archives = new ArrayList<>();
		ArrayList<Map> games = gamesService.getArrayListFromUrl(pTemplate.finishedGamesByUsername(username));
		games.forEach(gameData -> {
			String gameId = gameData.get("id").toString().replace(".0", "");
			try {
				archives.add(downloadCompleteGame(gameId));
			} catch (Exception exc) {
				System.err.println(String.format("Error loading archive for game %s: %s", gameId, exc.getMessage()));
			}
		});
		return archives;
	}

	/**
	 * Parses the turn files of an archive one by one, ordered by turn and player.
	 * A single turn file has a few hundred KB, so they are not all kept in memory.
	 */
	public void forEachTurn(String gameId, Consumer<Turn> consumer) throws IOException {
		forEachTurn(gameId, downloadCompleteGame(gameId), consumer);
	}

	public static void forEachTurn(String gameId, Path archive, Consumer<Turn> consumer) throws IOException {
		Gson gson = new Gson();
		try (ZipFile zip = new ZipFile(archive.toFile())) {
			List<ZipEntry> entries = new ArrayList<>();
			zip.stream().filter(entry -> TURN_FILE.matcher(entry.getName()).matches()).forEach(entries::add);
			entries.sort(Comparator.<ZipEntry>comparingInt(entry -> turnOf(entry))
					.thenComparingInt(entry -> playerOf(entry)));
			for (ZipEntry entry : entries) {
				String json = new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
				LinkedTreeMap<String, Object> data;
				try {
					data = gson.fromJson(json, LinkedTreeMap.class);
				}
				catch (JsonSyntaxException exc) {
					// turn 0 of old games has empty lists written as "scores": ] instead of []
					data = gson.fromJson(BROKEN_EMPTY_LIST.matcher(json).replaceAll("$1[]"), LinkedTreeMap.class);
				}
				consumer.accept(new Turn(gameId, playerOf(entry), turnOf(entry), data));
			}
		}
	}

	public List<Turn> getTurns(String gameId, int playerSlot) throws IOException {
		List<Turn> turns = new ArrayList<>();
		forEachTurn(gameId, turn -> {
			if (turn.playerSlot() == playerSlot) {
				turns.add(turn);
			}
		});
		return turns;
	}

	private static int playerOf(ZipEntry entry) {
		return parse(entry, 1);
	}

	private static int turnOf(ZipEntry entry) {
		return parse(entry, 2);
	}

	private static int parse(ZipEntry entry, int group) {
		Matcher matcher = TURN_FILE.matcher(entry.getName());
		matcher.matches();
		return Integer.parseInt(matcher.group(group));
	}
}
