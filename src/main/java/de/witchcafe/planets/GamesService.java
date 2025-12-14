package de.witchcafe.planets;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GamesService {

    private final GamesRepository gamesRepository;
	PlanetsRestTemplate pTemplate = new PlanetsRestTemplate();

	String[] mypois = new String[] {
			"lomize","moulliez d","gráinne","hawkeye418"		
	};
	
	String[] pois = new String[] {
			"strixy",
			"sualtim",
			"edama",
			"pivekop",
			"jollyroger"
	};
	
    GamesService(GamesRepository gamesRepository) {
        this.gamesRepository = gamesRepository;
        for (String poi : pois) {
            ArrayList<Game> games = pTemplate.getActiveGamesByUsername(poi);
            System.out.println(String.format("adding %s games for %s",games.size(),poi));
            games.forEach(game -> gamesRepository.save(game));

        }        
    }

    @Transactional
    public void createTask(String description, @Nullable LocalDate dueDate) {
        if ("fail".equals(description)) {
            throw new RuntimeException("This is for testing the error handler");
        }
        var task = new Game();
        // task.(dueDate);
        gamesRepository.saveAndFlush(task);
    }

    @Transactional(readOnly = true)
    public List<Game> list(Pageable pageable) {
        return gamesRepository.findAllBy(pageable).toList();
    }

	public ArrayList<Event> getEventsByGameid(String id) {
		return pTemplate.getEventsByGameid(id);
	}

	public ArrayList<Score> getScoresByGameid(String id) {
		return pTemplate.getScoresByGameid(id);
	}

}
