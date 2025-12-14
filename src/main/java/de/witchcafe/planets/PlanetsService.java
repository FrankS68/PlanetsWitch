package de.witchcafe.planets;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlanetsService {

    private final PlanetsRepository planetsRepository;

    PlanetsService(PlanetsRepository planetsRepository) {
        this.planetsRepository = planetsRepository;
    }

    @Transactional
    public void createTask(String description, @Nullable LocalDate dueDate) {
        if ("fail".equals(description)) {
            throw new RuntimeException("This is for testing the error handler");
        }
        var task = new Planet(description, Instant.now());
        task.setDueDate(dueDate);
        planetsRepository.saveAndFlush(task);
    }

    @Transactional(readOnly = true)
    public List<Planet> list(Pageable pageable) {
        return planetsRepository.findAllBy(pageable).toList();
    }

	PlanetsRestTemplate pTemplate = new PlanetsRestTemplate();

	public ArrayList<Event> getEventsByGameid(String id) {
		return pTemplate.getEventsByGameid(id);
	}

	public ArrayList<Score> getScoresByGameid(String id) {
		return pTemplate.getScoresByGameid(id);
	}

}
