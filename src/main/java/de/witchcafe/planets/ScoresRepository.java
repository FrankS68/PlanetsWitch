package de.witchcafe.planets;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface ScoresRepository extends JpaRepository<Score, Long>, JpaSpecificationExecutor<Score> {

    // If you don't need a total row count, Slice is better than Page as it only performs a select query.
    // Page performs both a select and a count query.
    Slice<Score> findAllBy(Pageable pageable);
}
