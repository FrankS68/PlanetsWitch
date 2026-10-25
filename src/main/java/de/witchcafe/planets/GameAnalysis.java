package de.witchcafe.planets;

import java.util.List;
import java.util.Map;

/**
 * Analysis of one player (slot) in a game archive, written by tools/game_analysis.py
 * as data/analysis/game{gameId}-slot{slot}.json.
 */
public record GameAnalysis(
		long gameId,
		String gameName,
		int slot,
		String username,
		String race,
		int turns,
		Integer finishRank,
		String generated,
		Milestones milestones,
		List<EconomyTurn> economy,
		List<TechChange> tech,
		List<ShipBuild> builds,
		List<Contact> contacts,
		List<Message> messages) {

	public record Milestones(
			Integer firstWarshipTurn,
			String firstWarship,
			int freightersBeforeFirstWarship,
			Integer firstOwnMessageTurn,
			int contactsWritten,
			Map<String, EconomyTurn> opening) {
	}

	public record EconomyTurn(
			int turn, int planets, long clans, int mines, int factories, int defense,
			long megacredits, long supplies, long neutronium, long duranium, long tritanium, long molybdenum,
			double colonistTaxAvg, int ships, int warships, int freighters, int starbases) {
	}

	public record TechChange(int turn, int starbaseId, int planetId, int hull, int engine, int beam, int torpedo) {
	}

	/** origin is "start", "gebaut" (appeared at an own starbase) or "übernommen" (captured or given) */
	public record ShipBuild(
			int turn, int shipId, String name, String hull, String engine,
			int beams, String beam, int launchers, String torpedo, int bays, boolean warship, String origin) {
	}

	public record Contact(
			int slot, String username, String race, Integer firstMessageTurn, int sent, int received,
			Integer ambassadorSentTurn, Integer ambassadorReceivedTurn) {
	}

	/** direction is "out" (written by the analysed player) or "in" */
	public record Message(int turn, String direction, int slot, String username, String text, List<String> recipients) {
	}

	public String getLabel() {
		return String.format("%s [%d] – %s (%s)", gameName, gameId, username, race);
	}
}
