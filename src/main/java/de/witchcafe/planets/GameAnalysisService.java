package de.witchcafe.planets;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

/**
 * Reads the analyses of the game archives that tools/game_analysis.py writes to data/analysis.
 */
@Service
public class GameAnalysisService {

	private final Path analysisDir;
	private final String python;
	private final Gson gson = new Gson();

	GameAnalysisService(@Value("${planetswitch.analysis-dir:data/analysis}") String analysisDir,
			@Value("${planetswitch.python:python}") String python) {
		this.analysisDir = Path.of(analysisDir);
		this.python = python;
	}

	/**
	 * Runs tools/player_analysis.py for the player (needs only api.planets.nu) and returns its output.
	 * An assessment already written into the player's file is kept by the script.
	 */
	public String runPlayerAnalysis(String username) throws IOException, InterruptedException {
		Process process = new ProcessBuilder(python, "-I", "tools/player_analysis.py", username)
				.redirectErrorStream(true).start();
		process.getOutputStream().close();
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		if (!process.waitFor(5, TimeUnit.MINUTES) || process.exitValue() != 0) {
			throw new IOException(String.format("player analysis of %s failed: %s", username, output));
		}
		return output;
	}

	public Path getAnalysisDir() {
		return analysisDir;
	}

	/** all analyses, newest game first */
	public List<GameAnalysis> findAll() {
		List<GameAnalysis> analyses = new ArrayList<>();
		if (!Files.isDirectory(analysisDir)) {
			return analyses;
		}
		try (Stream<Path> files = Files.list(analysisDir)) {
			files.filter(file -> file.getFileName().toString().matches("game\\d+-slot\\d+\\.json")).forEach(file -> {
				try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
					analyses.add(gson.fromJson(reader, GameAnalysis.class));
				}
				catch (Exception exc) {
					System.err.println(String.format("reading analysis %s failed: %s", file, exc.getMessage()));
				}
			});
		}
		catch (IOException exc) {
			System.err.println(String.format("listing %s failed: %s", analysisDir, exc.getMessage()));
		}
		analyses.sort(Comparator.comparingLong(GameAnalysis::gameId).reversed().thenComparingInt(GameAnalysis::slot));
		return analyses;
	}

	/** all player analyses (tools/player_analysis.py), sorted by username */
	public List<PlayerAnalysis> findAllPlayers() {
		List<PlayerAnalysis> analyses = new ArrayList<>();
		if (!Files.isDirectory(analysisDir)) {
			return analyses;
		}
		try (Stream<Path> files = Files.list(analysisDir)) {
			files.filter(file -> file.getFileName().toString().matches("player-.+\\.json")).forEach(file -> {
				try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
					analyses.add(gson.fromJson(reader, PlayerAnalysis.class));
				}
				catch (Exception exc) {
					System.err.println(String.format("reading player analysis %s failed: %s", file, exc.getMessage()));
				}
			});
		}
		catch (IOException exc) {
			System.err.println(String.format("listing %s failed: %s", analysisDir, exc.getMessage()));
		}
		analyses.sort(Comparator.comparing(PlayerAnalysis::username, String.CASE_INSENSITIVE_ORDER));
		return analyses;
	}
}
