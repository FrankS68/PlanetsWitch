package de.witchcafe.planets;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface GamesRepository extends JpaRepository<Game, Long>, JpaSpecificationExecutor<Game> {

    // If you don't need a total row count, Slice is better than Page as it only performs a select query.
    // Page performs both a select and a count query.
    Slice<Game> findAllBy(Pageable pageable);
}
