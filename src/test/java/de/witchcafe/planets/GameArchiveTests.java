package de.witchcafe.planets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GameArchiveTests {

	PlanetsRestTemplate pTemplate = new PlanetsRestTemplate();

	@TempDir
	Path tempDir;

	@Test
	void testParseTurnsSorted() throws IOException {
		Path archive = tempDir.resolve("game1.zip");
		try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
			addTurn(zip, "player2-turn10.trn", 10);
			addTurn(zip, "player1-turn2.trn", 2);
			addTurn(zip, "player2-turn2.trn", 2);
			zip.putNextEntry(new ZipEntry("readme.txt"));
			zip.closeEntry();
		}
		List<Turn> turns = new ArrayList<>();
		GameArchiveService.forEachTurn("1", archive, turns::add);

		assertEquals(3, turns.size());
		assertEquals(List.of("1/2", "2/2", "2/10"),
				turns.stream().map(turn -> turn.playerSlot() + "/" + turn.turn()).toList());
		assertEquals(1, turns.get(0).getPlanets().size());
		assertTrue(turns.get(0).getShips().isEmpty());
	}

	private void addTurn(ZipOutputStream zip, String name, int turn) throws IOException {
		zip.putNextEntry(new ZipEntry(name));
		zip.write(String.format("{\"settings\":{\"turn\":%s},\"planets\":[{\"id\":1}],\"ships\":[]}", turn)
				.getBytes(StandardCharsets.UTF_8));
		zip.closeEntry();
	}

	@Test
	void testDownloadFinishedGame() throws IOException {
		// Alpha Beta Sector, finished after turn 28
		Path archive = pTemplate.downloadToFile(pTemplate.loadCompleteGame("661436"), tempDir.resolve("game661436.zip"));
		List<Turn> turns = new ArrayList<>();
		GameArchiveService.forEachTurn("661436", archive, turn -> {
			if (turn.playerSlot() == 1) {
				turns.add(turn);
			}
		});
		assertFalse(turns.isEmpty());
		assertEquals(turns.get(turns.size() - 1).turn(),
				((Number) turns.get(turns.size() - 1).getSettings().get("turn")).intValue());
		turns.forEach(System.out::println);
	}

	@Test
	void testDownloadErrorResponseFails() {
		// the API answers with {"success":false,"error":...} instead of a zip
		IOException exc = assertThrows(IOException.class, () -> pTemplate
				.downloadToFile(pTemplate.loadCompleteGame(""), tempDir.resolve("game.zip")));
		assertTrue(exc.getMessage().contains("\"success\":false"), exc.getMessage());
		assertFalse(Files.exists(tempDir.resolve("game.zip")));
		assertFalse(Files.exists(tempDir.resolve("game.zip.part")));
	}
}
