package de.witchcafe.planets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.internal.LinkedTreeMap;

/**
 * One turn file (playerX-turnY.trn) of a game archive loaded via /game/loadall.
 * The file is the turn result as the given player saw it, the raw JSON stays available
 * in {@link #data()}.
 */
public record Turn(String gameId, int playerSlot, int turn, LinkedTreeMap<String, Object> data) {

	@SuppressWarnings("unchecked")
	public Map<String, Object> getSettings() {
		return (Map<String, Object>) data.get("settings");
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getPlayer() {
		return (Map<String, Object>) data.get("player");
	}

	public List<Map<String, Object>> getPlanets() {
		return list("planets");
	}

	public List<Map<String, Object>> getShips() {
		return list("ships");
	}

	public List<Map<String, Object>> getStarbases() {
		return list("starbases");
	}

	public List<Map<String, Object>> getScores() {
		return list("scores");
	}

	public List<Map<String, Object>> getMessages() {
		return list("messages");
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> list(String key) {
		Object value = data.get(key);
		return value instanceof List ? (List<Map<String, Object>>) value : new ArrayList<>();
	}

	public String toString() {
		return String.format("game %s player %s turn %s: %s planets, %s ships",
				gameId, playerSlot, turn, getPlanets().size(), getShips().size());
	}
}
