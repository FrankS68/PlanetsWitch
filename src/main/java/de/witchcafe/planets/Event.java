package de.witchcafe.planets;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.gson.internal.LinkedTreeMap;

public class Event {

	private static SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss aaa");
	private Date dateadded;
	private String 
		playerid,accountid,targetid,id,
		description,
		eventtype,
		turn;
	public static Event parse(LinkedTreeMap<String, Object> eventData) {
		Event event = new Event();
		event.accountid = eventData.get("accountid").toString();
		try {
			event.dateadded = sdf.parse( eventData.get("dateadded").toString());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		event.id = eventData.get("id").toString();
		event.targetid = eventData.get("targetid").toString();
		event.playerid = eventData.get("playerid").toString();
		event.description = eventData.get("description").toString();
		event.turn = eventData.get("turn").toString();
		event.eventtype = eventData.get("eventtype").toString();
		// TODO Auto-generated method stub
		return event;
	}
	
	public String toString() {
		return String.format("%s", playerid);
	}

}
