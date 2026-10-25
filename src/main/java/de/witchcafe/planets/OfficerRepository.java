package de.witchcafe.planets;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfficerRepository extends Neo4jRepository<Officer, String> {
    Officer findByOfficerId(String officerId);
    List<Officer> findByRaceidOrderByRankAsc(Integer raceid);
}
