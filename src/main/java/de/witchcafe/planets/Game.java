package de.witchcafe.planets;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "game")
public class Game {
		
    @Column(name = "name", nullable = false)
	String name;

	@Id
    @Column(name = "id")
	String id;
	String turn;
    // @Column(name = "description", nullable = false)
	// String description;
    @Column(name = "shortdescription", nullable = false)
	String shortdescription;
	String status;
	String statusname;
	Date datecreated;
	Date lasthostdate;
	ArrayList<Score> scores;
	ArrayList<Event> events;
	protected static String[] statusArray = {"Joining","Running","Finished","Hold"};

	public static void parseFromHistory(String game) {
    	System.out.println("Name: "+ game.toString()+"\n");
		
	}

	public static Game parse(Map input) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		Game game = new Game();
		game.name = input.get("name").toString();
		game.id = input.get("id").toString().replace(".0", "");
		game.turn = input.get("turn").toString();
		// game.description = input.get("description").toString();
		game.shortdescription = input.get("shortdescription").toString();
		game.status = input.get("status").toString();
		game.statusname = input.get("statusname").toString();
		try {
			game.datecreated = sdf.parse(input.get("datecreated").toString());
		} catch (ParseException e) {
			System.err.println(String.format("Error parsing %s for game %s","datecreated", game.id));
		}
		if (!game.statusname.equals("Joining")) try {
			game.lasthostdate = sdf.parse(input.get("lasthostdate").toString());
		} catch (ParseException e) {
			System.err.println(String.format("Error parsing %s for game %s","lasthostdate",game.id));
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
					id,exc.getMessage());
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

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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
}
