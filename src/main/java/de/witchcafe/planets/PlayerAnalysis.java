package de.witchcafe.planets;

import java.util.List;
import java.util.Map;

/**
 * Analysis of the last finished games of one player, written by tools/player_analysis.py
 * as data/analysis/player-{username}.json from the planets.nu API (history, scores, events).
 */
public record PlayerAnalysis(
		String username,
		long accountId,
		String title,
		int totalTurns,
		int turnsMissed,
		String generated,
		List<String> assessment,
		List<PlayedGame> games) {

	/** category is "win", "survived", "dead" or "left" (resigned) */
	public record PlayedGame(
			long gameId,
			String gameName,
			String gameType,
			Integer slot,
			String race,
			String dateEnded,
			int turns,
			int players,
			int finishRank,
			String outcome,
			String category,
			List<String> winners,
			int turnJoined,
			Integer turnLeft,
			Integer deathTurn,
			int lastAliveTurn,
			int peakPlanets,
			Integer peakPlanetsTurn,
			Integer bestPlanetRank,
			int turnsAsPlanetLeader,
			int finalPlanets,
			Integer finalPlanetRank,
			double militaryPercent,
			Integer turnsMissed,
			Map<String, ScorePoint> milestones,
			List<Alliance> alliances,
			List<Death> deaths,
			List<ScorePoint> curve) {

		public String getLabel() {
			return String.format("%s [%d] – %s, %s", gameName.strip(), gameId, race, outcome);
		}
	}

	/** planetRank / militaryRank are the player's rank among all slots in that turn, leader the slot with the most planets besides him */
	public record ScorePoint(
			int turn, int planets, int capitalShips, int freighters, int starbases, long military,
			int planetRank, int militaryRank, int leaderPlanets, String leader) {
	}

	public record Alliance(int turn, String partner, boolean formed) {
	}

	public record Death(int turn, int slot, String race, String username) {
	}
}
