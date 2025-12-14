package de.witchcafe.planets;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import de.witchcafe.examplefeature.Task;

interface PlanetsRepository extends JpaRepository<Planet, Long>, JpaSpecificationExecutor<Planet> {

    // If you don't need a total row count, Slice is better than Page as it only performs a select query.
    // Page performs both a select and a count query.
    Slice<Planet> findAllBy(Pageable pageable);
}
