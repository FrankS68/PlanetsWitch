package de.witchcafe.planets;

import java.time.LocalDate;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * The leaderboard values of a top officer on one day; the series of snapshots
 * is the history shown in the top officer chart.
 */
@Node("OfficerSnapshot")
public class OfficerSnapshot {
	@Id
    @GeneratedValue(generatorClass = GeneratedValue.InternalIdGenerator.class)
    private String id;
	private String officerId;
	private String accountid;
	private String username;
	private Integer raceid;
	private LocalDate day;
	private Integer rank;
	private Integer levelid;
	private String levelname;
	private Integer rating;
	private Long experience;
	private Long tonssunk;

    @Relationship(type = "snapshotOf", direction = Relationship.Direction.OUTGOING)
	private Officer officer;

	public static OfficerSnapshot of(Officer officer, LocalDate day) {
		OfficerSnapshot snapshot = new OfficerSnapshot();
		snapshot.officerId = officer.getOfficerId();
		snapshot.day = day;
		snapshot.update(officer);
		return snapshot;
	}

	public void update(Officer officer) {
		this.officer = officer;
		accountid = officer.getAccountid();
		username = officer.getUsername();
		raceid = officer.getRaceid();
		rank = officer.getRank();
		levelid = officer.getLevelid();
		levelname = officer.getLevelname();
		rating = officer.getRating();
		experience = officer.getExperience();
		tonssunk = officer.getTonssunk();
	}

	public String getId() {
		return id;
	}

	public String getOfficerId() {
		return officerId;
	}

	public String getAccountid() {
		return accountid;
	}

	public String getUsername() {
		return username;
	}

	public Integer getRaceid() {
		return raceid;
	}

	public LocalDate getDay() {
		return day;
	}

	public Integer getRank() {
		return rank;
	}

	public Integer getLevelid() {
		return levelid;
	}

	public String getLevelname() {
		return levelname;
	}

	public Integer getRating() {
		return rating;
	}

	public Long getExperience() {
		return experience;
	}

	public Long getTonssunk() {
		return tonssunk;
	}

	public Officer getOfficer() {
		return officer;
	}
}
