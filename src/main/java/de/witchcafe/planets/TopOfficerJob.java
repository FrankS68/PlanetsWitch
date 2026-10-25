package de.witchcafe.planets;

import java.time.LocalDate;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Loads the leaderboard once a day, so that every day gets a snapshot of the top officers.
 * The time can be changed with planets.topofficers.cron (default 06:00).
 */
@Component
public class TopOfficerJob {

	private final GamesService gamesService;
	private final OfficerSnapshotRepository snapshotRepository;

	TopOfficerJob(GamesService gamesService, OfficerSnapshotRepository snapshotRepository) {
		this.gamesService = gamesService;
		this.snapshotRepository = snapshotRepository;
	}

	@Scheduled(cron = "${planets.topofficers.cron:0 0 6 * * *}")
	public void loadDaily() {
		try {
			System.out.println(String.format("loaded %s top officers", gamesService.loadTopOfficers().size()));
		}
		catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	/** catches up on today's snapshot if the application was not running at the scheduled time */
	@EventListener(ApplicationReadyEvent.class)
	public void loadIfMissing() {
		try {
			if (!snapshotRepository.existsByDay(LocalDate.now())) {
				loadDaily();
			}
		}
		catch (Exception exc) {
			System.err.println(String.format("checking today's top officer snapshot failed: %s", exc.getMessage()));
		}
	}
}
