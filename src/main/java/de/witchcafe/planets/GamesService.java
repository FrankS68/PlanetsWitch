package de.witchcafe.planets;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;

@Service
public class GamesService {

    private final GamesRepository gamesRepository;
    private final PlayerRepository playerRepository;
	AuthenticatedPlanetsRestTemplate apTemplate = new AuthenticatedPlanetsRestTemplate();
	PlanetsRestTemplate pTemplate = new PlanetsRestTemplate();

	String[] mypois = new String[] {
			"lomize","moulliez d","gráinne","hawkeye418"		
	};
	
	String[] pois = new String[] {
			"strixy",
			"sualtim",
			"jabbapapa",
			"zalrik",
			"haler garog",
			"edama",
			"pivekop",
			"jollyroger"
	};
	
    GamesService(
    		GamesRepository gamesRepository,
    		PlayerRepository playerRepository) {
        this.gamesRepository = gamesRepository;
        this.playerRepository = playerRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional()
    public void initDatabaseData() {
    	try {
	        for (String poi : pois) {
	            ArrayList<Game> games = getActiveGamesByUsername(poi);
	            System.out.println(String.format("adding %s games for %s",games.size(),poi));
	            games.forEach(game -> {
	            	saveOrUpdate(game);
	            });
	
	        } 
	        for (String poi : mypois) {
	            ArrayList<Game> games = getActiveGamesByUsername(poi);
	            System.out.println(String.format("adding %s games for %s",games.size(),poi));
	            games.forEach(game -> {
	            	saveOrUpdate(game);
	            });
	
	        } 
    	}
    	catch (Exception exc) {
    		exc.printStackTrace();
    	}
    }

	private Player saveOrUpdatePlayer(LinkedTreeMap<String, Object> playerData) {
		Player player = new Player();
		player.setName(playerData.get("username").toString());
		player.setAccountId(playerData.get("accountid").toString());
		return saveOrUpdate(player);
    }

	private Player saveOrUpdate(Player player) {
		if (!playerRepository.existsByAccountid(player.getAccountid())){
			player = playerRepository.save(player);
			System.out.println(player.getId());
		}
		return player;
    }

	private Game saveOrUpdate(Game game) {
		Game storedGame = gamesRepository.findByGameId(game.getGameId());
		if (storedGame == null) {
			System.out.println(String.format("looking up gameid [%s]. returned game list is null.",game.getGameId()));
			gamesRepository.save(game);					
		}
		return gamesRepository.findByGameId(game.getGameId());
	}
    
    @Transactional(readOnly = true)
    public List<Game> list(Pageable pageable) {
        return gamesRepository.findAllBy(pageable).toList();
    }

    public List<Game> findAll() {
        return gamesRepository.findAll();
    }

    public Game getGame(String id) {
        return gamesRepository.findByGameId(id);
    }
	
	public HashMap<String, HashMap<Integer, Double>> getScoreSeriesbyGameid(String gameId) {
		return getScoreSeriesbyGameid(gameId, "planets");
	}
	
	public HashMap<String, HashMap<Integer, Double>> getScoreSeriesbyGameid(String gameId,String scoreType) {
		HashMap<String, HashMap<Integer, Double>> scoreSeries = new HashMap<String, HashMap<Integer, Double>>();
		ArrayList<Score> scores = getScoresByGameid(gameId);
		scores.forEach(score -> {
			HashMap<Integer, Double> scoreSerie = scoreSeries.get(score.getName());
			if (scoreSerie == null) {
				scoreSerie = new HashMap<Integer, Double>();
				scoreSeries.put(score.getName(), scoreSerie);
			}
			String scoreValue = score.getPlanets();
			switch (scoreType) {
			case "planets": {
				scoreValue = score.getPlanets();
				break;
			}
			case "freighters": {
				scoreValue = score.getFreighters();
				break;
			}
			case "capitalships": {
				scoreValue = score.getCapitalships();
				break;
			}
			case "starbases": {
				scoreValue = score.getStarbases();
				break;
			}
			default:
				break;
			}
			scoreSerie.put(Integer.valueOf(score.getTurn().replace(".0", "")),Double.valueOf(scoreValue.replace(".0", "")));
		});
		getGame(gameId).setScoresLoaded(true);
		return scoreSeries;
	}
	
	public HashMap<String, HashMap<Integer, Double>> getScoreShipSeriesbyGameid(String gameId) {
		HashMap<String, HashMap<Integer, Double>> scoreSeries = new HashMap<String, HashMap<Integer, Double>>();
		ArrayList<Score> scores = getScoresByGameid(gameId);
		scores.forEach(score -> {
			HashMap<Integer, Double> scoreSerie = scoreSeries.get(score.getName());
			if (scoreSerie == null) {
				scoreSerie = new HashMap<Integer, Double>();
				scoreSeries.put(score.getName(), scoreSerie);
			}
			scoreSerie.put(Integer.valueOf(score.getTurn().replace(".0", "")),Double.valueOf(score.getCapitalships().replace(".0", "")));
		});
		return scoreSeries;
	}
	
	
	public LinkedTreeMap<String, Object> getLinkedTreeMapFromUrl(String requestUrl) {
		LinkedTreeMap<String, Object> result = new Gson().fromJson(
				new String(pTemplate.getBytesFromUrl(requestUrl)), LinkedTreeMap.class);
		return result;
	}

	public ArrayList<Map> getArrayListFromUrl(String requestUrl) {
		ArrayList<Map> result = new Gson().fromJson(
				new String(apTemplate.getBytesFromUrl(requestUrl)), ArrayList.class);
		return result;
	}

	public Object getObjectFromUrl(String requestUrl) {
		Object result = new Gson().fromJson(
				new String(apTemplate.getBytesFromUrl(requestUrl)), Object.class);
		return result;
	}

	public ArrayList<Score> getScoresByGameid(String gameid) {
		ArrayList<Score> allScores = new ArrayList<Score>();
		LinkedTreeMap<String, Object> scoreMap = getLinkedTreeMapFromUrl(apTemplate.scoresByGameid(gameid));
		((ArrayList<LinkedTreeMap<String, Object>>)scoreMap.get("scores")).forEach(scoreData -> {
			Score score = Score.parse(scoreData);
			allScores.add(score);
			// System.out.println(score);
		});
		Game game = getGame(gameid);
		game.setScoresLoaded(true);
		gamesRepository.save(game);
		return allScores;
	}

	public ArrayList<Event> getEventsByGameid(String gameid) {
		ArrayList<Event> allEvents = new ArrayList<Event>();
		LinkedTreeMap<String, Object> eventMap = getLinkedTreeMapFromUrl(apTemplate.eventsByGameid(gameid));
		((ArrayList<LinkedTreeMap<String, Object>>)eventMap.get("events")).forEach(eventData -> {
			Event event = Event.parse(eventData);
			allEvents.add(event);
			// System.out.println(score);
		});
		return allEvents;
	}
	
	public ArrayList<Game> getActiveGamesByUsername(String username) {
		String requestUrl = apTemplate.activeGamesByUsername(username);
		try {
			ArrayList<Map> result = getArrayListFromUrl(requestUrl);
			ArrayList<Game> games = new ArrayList<Game>();
	        
	        result.forEach(gameToken -> {
	        	Game retrievedGame = Game.parse(gameToken);
	        	saveOrUpdate(retrievedGame);
				games.add(retrievedGame);	        	
        	});
	        List<Player> players = playerRepository.findByName(username);
	        if (players.size() > 1) {
				System.err.println(String.format("Search for username %s resultet in %s players.", username,players.size()));
	        }
			Player player = players.size() == 0 ? null : players.iterator().next();
			if (player != null) {
				if (player.getAccountid().endsWith(".0")) {
					player.setAccountId(player.getAccountid().replace(".0", ""));
				}
				player.setActiveGamesLoaded(new Date());
				playerRepository.save(player);
			}

	        return games; 
		}
		catch (JsonSyntaxException exc) {
			System.err.println(getObjectFromUrl(requestUrl).getClass().getName());
			return null;
		}
	}
	
	
	public ArrayList<GameHistory> getAccountHistory(String accountid) {

		String requestUrl = apTemplate.accountHistoryByAccountId(accountid);
		try {
			LinkedTreeMap<String, Object> result = getLinkedTreeMapFromUrl(requestUrl);
			ArrayList<LinkedTreeMap> gameJSons = (ArrayList<LinkedTreeMap>) result.get("history");
			ArrayList<GameHistory> games = new ArrayList<GameHistory>();
			
			if (gameJSons.size() == 0) {
				throw new RuntimeException("Result seems empty");
			}
	        
			gameJSons.forEach(game -> {
				try {
					GameHistory gameHistory = GameHistory.parse(game);
					saveOrUpdate(gameHistory.game);
					games.add(gameHistory);
				} catch (Exception exc) {
					System.err.println(String.format("Error: %s\nparsing gamehistory\n%s", exc.getMessage(), game));
				}
			});
	        Player player = null;
			try{
				player = playerRepository.findByAccountid(accountid);
			}
			catch (Exception exc){
				player = playerRepository.findByAccountid(accountid+".0");				
				player.setAccountId(accountid);
			}
			if (player != null) {
				player.setHistoryLoaded(new Date());
				playerRepository.save(player);
			}
	        return games;
		}
		catch (JsonSyntaxException exc) {
			System.err.println(getObjectFromUrl(requestUrl).getClass().getName());
			return null;
		}
        
    }

	public LinkedTreeMap<String, Object> getGameInfo(String gameId) {
		LinkedTreeMap<String, Object> gameInfo = getLinkedTreeMapFromUrl(getPlanetsTemplate().loadGameInfo(gameId));
		ArrayList<LinkedTreeMap<String, Object>> players = (ArrayList<LinkedTreeMap<String, Object>>) gameInfo.get("players");
		if (players != null && players.size() > 0) {
			players.forEach(playerData -> {
				if (!playerData.get("accountid").toString().equals("0.0") && !playerData.get("username").toString().equals("dead")) {
					Player player = saveOrUpdatePlayer(playerData);

					Player foundPlayer = playerRepository.findByAccountid(player.getAccountid());

					Game foundGame = gamesRepository.findByGameId(gameId);
					String raceId = playerData.get("raceid").toString();
					foundPlayer.addPlayerIn(foundGame, null, null, raceId);
					playerRepository.save(foundPlayer);
				}
			});
			
		}
		Game game = getGame(gameId);
		game.setDetailsLoaded(true);
		gamesRepository.save(game);
		return gameInfo;
	}

	public AuthenticatedPlanetsRestTemplate getPlanetsTemplate() {
		return apTemplate;
	}

}
