package de.witchcafe.planets;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * An officer (one per account and race) as listed in the "topofficers" section of
 * http://api.planets.nu/account/leaderboard.
 */
@Node("Officer")
public class Officer {
	@Id
    @GeneratedValue(generatorClass = GeneratedValue.InternalIdGenerator.class)
    private String id;
	private String officerId;
	private String accountid;
	private String username;
	private Integer raceid;
	private Integer rank;
	private Integer levelid;
	private String levelname;
	private Integer maxlevelid;
	private Long experience;
	private Integer rating;
	private Long tonssunk;
	private Long tonscaptured;
	private Long defensedestroyed;
	private Integer turnsplayed;
	private String commandshipname;
	private Date dateachieved;
	private Date topOfficerLoaded;

    @Relationship(type = "officerOf", direction = Relationship.Direction.OUTGOING)
	private Player player;

	public static Officer parse(Map input) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Officer officer = new Officer();
		officer.officerId = asString(input.get("id"));
		officer.accountid = asString(input.get("accountid"));
		officer.username = asString(input.get("username"));
		officer.raceid = asInteger(input.get("raceid"));
		officer.levelid = asInteger(input.get("levelid"));
		officer.levelname = asString(input.get("levelname"));
		officer.maxlevelid = asInteger(input.get("maxlevelid"));
		officer.experience = asLong(input.get("experience"));
		officer.rating = asInteger(input.get("rating"));
		officer.tonssunk = asLong(input.get("tonssunk"));
		officer.tonscaptured = asLong(input.get("tonscaptured"));
		officer.defensedestroyed = asLong(input.get("defensedestroyed"));
		officer.turnsplayed = asInteger(input.get("turnsplayed"));
		officer.commandshipname = asString(input.get("commandshipname"));
		if (input.get("dateachieved") != null) try {
			officer.dateachieved = sdf.parse(input.get("dateachieved").toString());
		} catch (ParseException e) {
			System.err.println(String.format("Error parsing %s for officer %s","dateachieved", officer.officerId));
		}
		return officer;
	}

	private static String asString(Object value) {
		return value == null ? null : value.toString().replaceAll("\\.0$", "");
	}

	private static Integer asInteger(Object value) {
		return value == null ? null : ((Number) value).intValue();
	}

	private static Long asLong(Object value) {
		return value == null ? null : ((Number) value).longValue();
	}

	/** copies the leaderboard values of another (freshly parsed) officer into this one */
	public void update(Officer other) {
		accountid = other.accountid;
		username = other.username;
		raceid = other.raceid;
		levelid = other.levelid;
		levelname = other.levelname;
		maxlevelid = other.maxlevelid;
		experience = other.experience;
		rating = other.rating;
		tonssunk = other.tonssunk;
		tonscaptured = other.tonscaptured;
		defensedestroyed = other.defensedestroyed;
		turnsplayed = other.turnsplayed;
		commandshipname = other.commandshipname;
		dateachieved = other.dateachieved;
	}

	public String toString() {
		return String.format("%s\trace %s\t#%s\t%s\t%s", username, raceid, rank, levelname, rating);
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

	public Integer getRank() {
		return rank;
	}

	public void setRank(Integer rank) {
		this.rank = rank;
	}

	public Integer getLevelid() {
		return levelid;
	}

	public String getLevelname() {
		return levelname;
	}

	public Integer getMaxlevelid() {
		return maxlevelid;
	}

	public Long getExperience() {
		return experience;
	}

	public Integer getRating() {
		return rating;
	}

	public Long getTonssunk() {
		return tonssunk;
	}

	public Long getTonscaptured() {
		return tonscaptured;
	}

	public Long getDefensedestroyed() {
		return defensedestroyed;
	}

	public Integer getTurnsplayed() {
		return turnsplayed;
	}

	public String getCommandshipname() {
		return commandshipname;
	}

	public Date getDateachieved() {
		return dateachieved;
	}

	public Date getTopOfficerLoaded() {
		return topOfficerLoaded;
	}

	public void setTopOfficerLoaded(Date date) {
		this.topOfficerLoaded = date;
	}

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}
}
