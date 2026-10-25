package de.witchcafe.planets;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SavedSearchRepository extends Neo4jRepository<SavedSearch, String> {
    List<SavedSearch> findAllByOrderByCreatedAsc();
    SavedSearch findByName(String name);
}
