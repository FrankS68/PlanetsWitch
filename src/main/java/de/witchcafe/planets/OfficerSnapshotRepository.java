package de.witchcafe.planets;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfficerSnapshotRepository extends Neo4jRepository<OfficerSnapshot, String> {
    OfficerSnapshot findByOfficerIdAndDay(String officerId, LocalDate day);
    List<OfficerSnapshot> findByRaceidOrderByDayAsc(Integer raceid);
    Boolean existsByDay(LocalDate day);
}
