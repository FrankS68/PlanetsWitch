package de.witchcafe.planets;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.gson.internal.LinkedTreeMap;

public class Score {

	private static SimpleDateFormat sdf = new SimpleDateFormat("dd/M/yyyy H:mm:ss aaa");
	private Date dateadded;
	private String 
		name,accountid,
		capitalships,freighters,
		planets,starbases,
		militaryscore,inventoryscore,prioritypoints,
		turn,percent,victoryscore;
	public static Score parse(LinkedTreeMap<String, Object> scoreData) {
		Score score = new Score();
		score.name = scoreData.get("name").toString();
		score.accountid = scoreData.get("accountid").toString().replace(".0", "");
		try {
			score.dateadded = sdf.parse( scoreData.get("dateadded").toString());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		score.turn = scoreData.get("turn").toString();
		score.capitalships = scoreData.get("capitalships").toString();
		score.freighters = scoreData.get("freighters").toString();
		score.planets = scoreData.get("planets").toString();
		score.starbases = scoreData.get("starbases").toString();
		// TODO Auto-generated method stub
		return score;
	}
	
	public String toString() {
		return String.format("%s", name);
	}

	public Date getDateadded() {
		return dateadded;
	}

	public void setDateadded(Date dateadded) {
		this.dateadded = dateadded;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAccountid() {
		return accountid;
	}

	public void setAccountid(String accountid) {
		this.accountid = accountid;
	}

	public String getCapitalships() {
		return capitalships;
	}

	public void setCapitalships(String capitalships) {
		this.capitalships = capitalships;
	}

	public String getFreighters() {
		return freighters;
	}

	public void setFreighters(String freighters) {
		this.freighters = freighters;
	}

	public String getPlanets() {
		return planets;
	}

	public void setPlanets(String planets) {
		this.planets = planets;
	}

	public String getStarbases() {
		return starbases;
	}

	public void setStarbases(String starbases) {
		this.starbases = starbases;
	}

	public String getTurn() {
		return turn;
	}

	public void setTurn(String turn) {
		this.turn = turn;
	}

}
