package de.witchcafe.planets;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
	private final Gson gson = new Gson();

	GameAnalysisService(@Value("${planetswitch.analysis-dir:data/analysis}") String analysisDir) {
		this.analysisDir = Path.of(analysisDir);
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
}
