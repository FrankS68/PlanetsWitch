package de.witchcafe.planets;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlanetsService {

	@Autowired
	GamesService gameService;

	public ArrayList<Event> getEventsByGameid(String id) {
		return gameService.getEventsByGameid(id);
	}

	public ArrayList<Score> getScoresByGameid(String id) {
		return gameService.getScoresByGameid(id);
	}

}
