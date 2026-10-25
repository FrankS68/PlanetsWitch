package de.witchcafe.planets;

import java.time.Instant;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * A named filter of the graph view: the AND/OR tree as JSON and, when the query was edited
 * by hand, that cypher.
 */
@Node("SavedSearch")
public class SavedSearch {
	@Id
    @GeneratedValue(generatorClass = GeneratedValue.InternalIdGenerator.class)
    private String id;
	private String name;
	private String filterJson;
	private String cypher;
	private Instant created;

	public SavedSearch() {
	}

	public SavedSearch(String name) {
		this.name = name;
		this.created = Instant.now();
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getFilterJson() {
		return filterJson;
	}

	public void setFilterJson(String filterJson) {
		this.filterJson = filterJson;
	}

	/** the query edited by hand, null when it is generated from the tree */
	public String getCypher() {
		return cypher;
	}

	public void setCypher(String cypher) {
		this.cypher = cypher;
	}

	public Instant getCreated() {
		return created;
	}
}
