package de.witchcafe.planets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;

public class PlanetsRestTemplate extends RestTemplate {
	private byte[] getBytesFromUrl(String requestUrl) {
		byte[] content = new byte[0];

        HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.set(HttpHeaders.ACCEPT_ENCODING, "gzip");

		ResponseEntity<Resource> responseEntity = exchange(requestUrl, HttpMethod.GET,
                new HttpEntity<>(httpHeaders), Resource.class);

        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            System.err.println("error: " + responseEntity.getStatusCode().toString());
            throw new RuntimeException("error: " + responseEntity.getStatusCode().toString());
        }

        // Encoding type of the response body
        String contentEncoding = responseEntity.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);

        if ("gzip".equalsIgnoreCase(contentEncoding)) {

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            try (GZIPInputStream gzipInputStream = new GZIPInputStream(responseEntity.getBody().getInputStream())) {
                gzipInputStream.transferTo(byteArrayOutputStream);
            } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

            content = byteArrayOutputStream.toByteArray();
   
        } else {
        	try {
        		System.out.println(requestUrl +"\n"+
				responseEntity.getBody().getContentAsString(Charset.defaultCharset()));
			} catch (IOException e) {
				e.printStackTrace();
			}
            // TODO
        }
		return content;
	}
	
	public LinkedTreeMap<String, Object> getLinkedTreeMapFromUrl(String requestUrl) {
		LinkedTreeMap<String, Object> result = new Gson().fromJson(
				new String(getBytesFromUrl(requestUrl)), LinkedTreeMap.class);
		return result;
	}

	public ArrayList<Map> getArrayListFromUrl(String requestUrl) {
		ArrayList<Map> result = new Gson().fromJson(
				new String(getBytesFromUrl(requestUrl)), ArrayList.class);
		return result;
	}

	public Object getObjectFromUrl(String requestUrl) {
		Object result = new Gson().fromJson(
				new String(getBytesFromUrl(requestUrl)), Object.class);
		return result;
	}

	private String scoresByGameid(String gameid) {
		return String.format("http://api.planets.nu/game/loadscores?gameid=%s",gameid);
	}

	private String eventsByGameid(String gameid) {
		return String.format("http://api.planets.nu/game/loadevents?gameid=%s",gameid);
	}

	private String activeGamesByUsername(String username) {
		return String.format("http://api.planets.nu/games/list?username=%s",username);
	}

	private String accountHistoryByAccountId(String accountid) {
		return String.format("http://api.planets.nu/account/history?accountid=%s",accountid);
	}

	private String officerByUsername(String username) {
		return String.format("http://api.planets.nu/account/loadofficer?officerid=%s",username);
	}

	private String loadAccount(String username) {
		return String.format("http://api.planets.nu/account/load?username=%s",username);
	}

	private String profileByUsername(String username) {
		return String.format("http://api.planets.nu/account/loadprofile?username=%s",username);
	}
	
	private String finishedGamesByUsername(String username) {
		return String.format("http://api.planets.nu/games/list?status=3,4&username=%s",username);
	}

	public ArrayList<Score> getScoresByGameid(String gameid) {
		ArrayList<Score> allScores = new ArrayList<Score>();
		LinkedTreeMap<String, Object> scoreMap = getLinkedTreeMapFromUrl(scoresByGameid(gameid));
		((ArrayList<LinkedTreeMap<String, Object>>)scoreMap.get("scores")).forEach(scoreData -> {
			Score score = Score.parse(scoreData);
			allScores.add(score);
			// System.out.println(score);
		});
		return allScores;
	}

	public ArrayList<Event> getEventsByGameid(String gameid) {
		ArrayList<Event> allEvents = new ArrayList<Event>();
		LinkedTreeMap<String, Object> eventMap = getLinkedTreeMapFromUrl(eventsByGameid(gameid));
		((ArrayList<LinkedTreeMap<String, Object>>)eventMap.get("events")).forEach(eventData -> {
			Event event = Event.parse(eventData);
			allEvents.add(event);
			// System.out.println(score);
		});
		return allEvents;
	}
	
	public ArrayList<Game> getActiveGamesByUsername(String username) {
		String requestUrl = activeGamesByUsername(username);
		try {
			ArrayList<Map> result = getArrayListFromUrl(requestUrl);
			ArrayList<Game> games = new ArrayList<Game>();
	        
	        result.forEach(gameToken -> {
	        	games.add(Game.parse(gameToken));
	        	
	        	});
	        return games; 
		}
		catch (JsonSyntaxException exc) {
			System.err.println(getObjectFromUrl(requestUrl).getClass().getName());
			return null;
		}
	}

	
	public ArrayList<GameHistory> getAccountHistory(String accountid) {

		String requestUrl = accountHistoryByAccountId(accountid);
		try {
			LinkedTreeMap<String, Object> result = getLinkedTreeMapFromUrl(requestUrl);
			ArrayList<LinkedTreeMap> gameJSons = (ArrayList<LinkedTreeMap>) result.get("history");
			ArrayList<GameHistory> games = new ArrayList<GameHistory>();
			
			if (gameJSons.size() == 0) {
				throw new RuntimeException("Result seems empty");
			}
	        
	        gameJSons.forEach(game -> {
	        	GameHistory gameHistory = GameHistory.parse(game);
	        	games.add(gameHistory);
	        	});
	        return games;
		}
		catch (JsonSyntaxException exc) {
			System.err.println(getObjectFromUrl(requestUrl).getClass().getName());
			return null;
		}
        
    }

}
