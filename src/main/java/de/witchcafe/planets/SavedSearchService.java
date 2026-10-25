package de.witchcafe.planets;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.witchcafe.planets.ui.GraphFilter;
import de.witchcafe.planets.ui.GraphFilter.Condition;
import de.witchcafe.planets.ui.GraphFilter.Operator;

@Service
public class SavedSearchService {

	static final String SEARCH_ME = "SearchMe";

	@Autowired
	SavedSearchRepository savedSearchRepository;

	/** the saved searches, oldest first; without any, "SearchMe" (the former toolbar button) is created */
	public List<SavedSearch> findAll() {
		if (savedSearchRepository.count() == 0) {
			GraphFilter searchMe = new GraphFilter(
					new Condition("Player", "name", Operator.IN, "lomize, hawkeye418, gráinne, moulliez d"));
			save(SEARCH_ME, searchMe.toJson(), null);
		}
		return savedSearchRepository.findAllByOrderByCreatedAsc();
	}

	/** saves under this name, a search with the same name is overwritten (and keeps its position) */
	public SavedSearch save(String name, String filterJson, String cypher) {
		SavedSearch search = savedSearchRepository.findByName(name);
		if (search == null) {
			search = new SavedSearch(name);
		}
		search.setFilterJson(filterJson);
		search.setCypher(cypher);
		return savedSearchRepository.save(search);
	}

	public SavedSearch findByName(String name) {
		return savedSearchRepository.findByName(name);
	}

	public void delete(SavedSearch search) {
		savedSearchRepository.delete(search);
	}
}
