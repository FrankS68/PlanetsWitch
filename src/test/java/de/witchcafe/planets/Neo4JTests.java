package de.witchcafe.planets;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.http.ResponseEntity;

@SpringBootTest
//1. Sage Spring, wo die Repositories liegen
// @EnableNeo4jRepositories(basePackages = "de.witchcafe.planets") 
//2. Sage Spring, wo die @Node Klassen (Player) liegen
@EntityScan(basePackages = "de.witchcafe.planets")
class Neo4JTests {

	@Autowired
	private org.springframework.data.neo4j.core.Neo4jClient neo4jClient;
	
	@Test
	void debugConnection() {
	    assertNotNull(neo4jClient, "Der Neo4jClient wurde nicht erstellt! Prüfe deine application.properties.");
	}
	
	@Test
	void testNative() {
	    var result = neo4jClient.query("MATCH (n) RETURN count(n) as c").fetchAs(Long.class).one();
	    System.out.println("Nativ gezählt: " + result);
	}
	
	@Test
	void testMatchAllGames() {
	    List result = neo4jClient.query("MATCH (g:Game) RETURN g").fetchAs(List.class).one().get();
	    System.out.println("Nativ gezählt: " + result);
	}
	
	@Autowired
    private PlayerRepository playerRepository;
	
	@Test
	void testCount() {
		System.out.println(playerRepository.count());
	}

}
