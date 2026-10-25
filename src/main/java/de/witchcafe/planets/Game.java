package de.witchcafe.planets;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("Game")
public class Game {
		

	@Id
    @GeneratedValue(generatorClass = GeneratedValue.InternalIdGenerator.class) // Oder lass GeneratedValue weg, wenn du accountid nutzt
    private String id;
	String gameId;
	String name;
	String turn;
	String shortdescription;
	String status;
	String statusname;
	Date datecreated;
	Date lasthostdate;
	ArrayList<Score> scores;
	ArrayList<Event> events;
	private boolean scoresLoaded;
	private boolean detailsLoaded;
	private boolean archiveLoaded;
	protected static String[] statusArray = {"Joining","Running","Finished","Hold"};

	public static void parseFromHistory(String game) {
    	System.out.println("Name: "+ game.toString()+"\n");
		
	}

	public static Game parse(Map input) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		Game game = new Game();
		game.name = input.get("name").toString();
		game.gameId = input.get("id").toString().replace(".0", "");
		game.turn = input.get("turn").toString();
		// game.description = input.get("description").toString();
		game.shortdescription = input.get("shortdescription") == null ? "" : input.get("shortdescription").toString();
		game.status = input.get("status").toString();
		game.statusname = input.get("statusname").toString();
		try {
			game.datecreated = sdf.parse(input.get("datecreated").toString());
		} catch (ParseException e) {
			System.err.println(String.format("Error parsing %s for game %s","datecreated", game.gameId));
		}
		if (!game.statusname.equals("Joining") && input.get("lasthostdate") != null) try {
			game.lasthostdate = sdf.parse(input.get("lasthostdate").toString());
		} catch (ParseException e) {
			System.err.println(String.format("Error parsing %s for game %s","lasthostdate",game.gameId));
		}
		return game;
	}

	public void setEvents(ArrayList<Event> e) {
		events = e;
	}

	public ArrayList<Event> getEvents() {
		try{
			return events;
		}
		catch (Exception exc) {
			System.err.println(exc.getMessage());
			return new ArrayList<Event>();
		}
	}

	public void setScores(ArrayList<Score> s) {
		scores = s;
	}

	public ArrayList<Score> getScores() {
		try{
			return scores;
		}
		catch (Exception exc) {
			System.err.println(exc.getMessage());
			return new ArrayList<Score>();
		}
		// /game/loadscores
	}

	public String printScores() {
		try {
			StringBuilder output = new StringBuilder();
			getScores().forEach(score -> {
				output.append(String.format("%s\n", score));
			});
			return output.toString();
		}
		catch (Exception exc) {
			return String.format("Error printing scores for %s:\n%s", 
					gameId,exc.getMessage());
		}
		// /game/loadscores
	}
	
	public String toString() {
		return String.format(
				"%s\t%s\t %s\t%s", 
				name,statusname,
				scores == null ? "-" : scores.size(),
				events == null ? "-" : events.size());
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String id) {
		this.gameId = id;
	}

	/*
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	*/

	public Date getDatecreated() {
		return datecreated;
	}

	public void setDatecreated(Date datecreated) {
		this.datecreated = datecreated;
	}

	public String getTurn() {
		return turn;
	}

	public void setTurn(String turn) {
		this.turn = turn;
	}

	public String getStatusname() {
		return statusname;
	}

	public void setStatusname(String statusname) {
		this.statusname = statusname;
	}

	public boolean getScoresLoaded() {
		return scoresLoaded;
	}

	public void setScoresLoaded(boolean b) {
		scoresLoaded = b;
	}

	public boolean getArchiveLoaded() {
		return archiveLoaded;
	}

	public void setArchiveLoaded(boolean b) {
		archiveLoaded = b;
	}

	public boolean getDetailsLoaded() {
		return detailsLoaded;
	}

	public void setDetailsLoaded(boolean b) {
		detailsLoaded = b;
	}
}
