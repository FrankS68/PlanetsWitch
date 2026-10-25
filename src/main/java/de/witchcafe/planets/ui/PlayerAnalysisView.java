package de.witchcafe.planets.ui;

import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.svg.SVGGraphics2D;
import org.jfree.svg.ViewBox;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import de.witchcafe.planets.GameAnalysisService;
import de.witchcafe.planets.PlayerAnalysis;
import de.witchcafe.planets.PlayerAnalysis.PlayedGame;
import de.witchcafe.planets.PlayerAnalysis.ScorePoint;

/**
 * Shows the analysis of a player's last finished games (tools/player_analysis.py):
 * outcomes, planet curve against the leader, ships, alliances and Aletheia's assessment.
 * Query parameter player preselects a player, e.g. /playeranalysis?player=konstantin
 */
@Route("playeranalysis")
@PageTitle("Spieleranalyse")
@AnonymousAllowed
@Menu(order = 0, icon = "vaadin:user-card", title = "Spieleranalyse")
public class PlayerAnalysisView extends VerticalLayout implements BeforeEnterObserver {

	private static final Map<String, String> CATEGORY_LABELS = Map.of(
			"win", "Siege", "survived", "überlebt", "dead", "ausgeschieden", "left", "aufgegeben");

	private record Metric(String label, ToDoubleFunction<ScorePoint> value) {
	}

	private final GameAnalysisService analysisService;
	private final Select<PlayerAnalysis> playerSelect = new Select<>();
	private final VerticalLayout content = new VerticalLayout();

	PlayerAnalysisView(GameAnalysisService analysisService) {
		this.analysisService = analysisService;
		playerSelect.setLabel("Spieler");
		playerSelect.setItemLabelGenerator(player -> String.format("%s (%s, %d Spiele)",
				player.username(), player.title(), player.games().size()));
		playerSelect.setWidth("25em");
		playerSelect.addValueChangeListener(event -> show(event.getValue()));
		content.setPadding(false);
		content.setSizeFull();
		add(playerSelect, content);
		setSizeFull();
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		List<PlayerAnalysis> players = analysisService.findAllPlayers();
		playerSelect.setItems(players);
		if (players.isEmpty()) {
			content.removeAll();
			content.add(new Span(String.format(
					"Noch keine Spieleranalysen in %s. Erzeugen mit: python -I tools/player_analysis.py <username>",
					analysisService.getAnalysisDir().toAbsolutePath())));
			return;
		}
		String player = event.getLocation().getQueryParameters().getSingleParameter("player").orElse(null);
		playerSelect.setValue(players.stream()
				.filter(analysis -> player == null || analysis.username().equalsIgnoreCase(player))
				.findFirst().orElse(players.get(0)));
	}

	private void show(PlayerAnalysis player) {
		content.removeAll();
		if (player == null) {
			return;
		}
		List<PlayedGame> games = player.games();
		String record = games.stream().collect(Collectors.groupingBy(PlayedGame::category, Collectors.counting()))
				.entrySet().stream()
				.map(entry -> entry.getValue() + " " + CATEGORY_LABELS.getOrDefault(entry.getKey(), entry.getKey()))
				.collect(Collectors.joining(", "));
		String races = games.stream().collect(Collectors.groupingBy(PlayedGame::race, Collectors.counting()))
				.entrySet().stream().map(entry -> entry.getValue() + "× " + entry.getKey())
				.collect(Collectors.joining(", "));
		content.add(new H3(String.format("%s – letzte %d abgeschlossene Spiele", player.username(), games.size())));
		content.add(new Span(String.format("%s · %d Züge gespielt, %d verpasst · Bilanz: %s · Rassen: %s · Stand %s",
				player.title(), player.totalTurns(), player.turnsMissed(), record, races, player.generated())));
		if (player.assessment() != null && !player.assessment().isEmpty()) {
			content.add(new H4("Einschätzung von Aletheia"));
			player.assessment().forEach(paragraph -> content.add(new Paragraph(paragraph)));
		}
		content.add(createGameGrid(games));

		Select<PlayedGame> gameSelect = new Select<>();
		gameSelect.setLabel("Spielverlauf");
		gameSelect.setItems(games);
		gameSelect.setItemLabelGenerator(PlayedGame::getLabel);
		gameSelect.setWidth("50em");
		VerticalLayout gameArea = new VerticalLayout();
		gameArea.setPadding(false);
		gameSelect.addValueChangeListener(event -> {
			gameArea.removeAll();
			if (event.getValue() != null) {
				gameArea.add(createGameDetails(event.getValue()));
			}
		});
		content.add(gameSelect, gameArea);
		gameSelect.setValue(games.isEmpty() ? null : games.get(0));
	}

	private static String milestone(PlayedGame game, int turn) {
		ScorePoint point = game.milestones().get(String.valueOf(turn));
		return point == null ? "" : String.format("%d (#%d)", point.planets(), point.planetRank());
	}

	private Grid<PlayedGame> createGameGrid(List<PlayedGame> games) {
		Grid<PlayedGame> grid = new Grid<>();
		grid.addColumn(PlayedGame::dateEnded).setHeader("Ende").setAutoWidth(true).setFlexGrow(0);
		grid.addColumn(game -> String.format("%s [%d]", game.gameName().strip(), game.gameId())).setHeader("Spiel").setAutoWidth(true);
		grid.addColumn(PlayedGame::gameType).setHeader("Typ").setAutoWidth(true);
		grid.addColumn(PlayedGame::race).setHeader("Rasse").setAutoWidth(true);
		grid.addColumn(PlayedGame::outcome).setHeader("Ergebnis").setAutoWidth(true);
		grid.addColumn(game -> game.turnJoined() > 1 ? "ab Zug " + game.turnJoined() : "").setHeader("Eingestiegen").setAutoWidth(true);
		grid.addColumn(game -> milestone(game, 10)).setHeader("Zug 10").setAutoWidth(true);
		grid.addColumn(game -> milestone(game, 30)).setHeader("Zug 30").setAutoWidth(true);
		grid.addColumn(game -> milestone(game, 50)).setHeader("Zug 50").setAutoWidth(true);
		grid.addColumn(game -> String.format("%d (Zug %s)", game.peakPlanets(), game.peakPlanetsTurn())).setHeader("Max. Planeten").setAutoWidth(true);
		grid.addColumn(PlayedGame::turnsAsPlanetLeader).setHeader("Züge als Planeten-Erster").setAutoWidth(true);
		grid.addColumn(game -> String.format("%d (#%s)", game.finalPlanets(), game.finalPlanetRank())).setHeader("Planeten am Ende").setAutoWidth(true);
		grid.addColumn(PlayedGame::turnsMissed).setHeader("Verpasst").setAutoWidth(true);
		grid.addThemeVariants(GridVariant.LUMO_COMPACT);
		grid.setItems(games);
		grid.setAllRowsVisible(true);
		return grid;
	}

	private VerticalLayout createGameDetails(PlayedGame game) {
		Select<String> chartSelect = new Select<>();
		chartSelect.setLabel("Werte");
		chartSelect.setItems("Planeten gegen Führenden", "Schiffe & Starbases", "Rang");
		chartSelect.setValue("Planeten gegen Führenden");
		chartSelect.setWidth("16em");
		VerticalLayout chartArea = new VerticalLayout();
		chartArea.setPadding(false);
		Runnable refresh = () -> {
			chartArea.removeAll();
			List<Metric> metrics = switch (chartSelect.getValue()) {
			case "Schiffe & Starbases" -> List.of(new Metric("Kriegsschiffe", ScorePoint::capitalShips),
					new Metric("Frachter", ScorePoint::freighters), new Metric("Starbases", ScorePoint::starbases));
			case "Rang" -> List.of(new Metric("Planeten-Rang", ScorePoint::planetRank),
					new Metric("Militär-Rang", ScorePoint::militaryRank));
			default -> List.of(new Metric("eigene Planeten", ScorePoint::planets),
					new Metric("meiste Planeten der anderen", ScorePoint::leaderPlanets));
			};
			chartArea.add(new Html(createChartSvg(game, chartSelect.getValue(), metrics)));
		};
		chartSelect.addValueChangeListener(event -> refresh.run());
		refresh.run();

		String events = Stream.concat(
				game.alliances().stream().map(alliance -> String.format("Zug %d: Allianz mit %s %s", alliance.turn(),
						alliance.partner(), alliance.formed() ? "geschlossen" : "beendet")),
				game.deaths().stream().map(death -> String.format("Zug %d: %s (%s) tot", death.turn(), death.race(),
						death.username())))
				.collect(Collectors.joining(" · "));
		VerticalLayout details = new VerticalLayout(new HorizontalLayout(chartSelect), chartArea,
				new Span(String.format("Gewinner: %s", game.winners().isEmpty() ? "–" : String.join(", ", game.winners()))),
				new Span(events.isEmpty() ? "Keine Allianzen oder ausgeschiedenen Spieler." : events));
		details.setPadding(false);
		return details;
	}

	private String createChartSvg(PlayedGame game, String title, List<Metric> metrics) {
		XYSeriesCollection dataset = new XYSeriesCollection();
		metrics.forEach(metric -> {
			XYSeries series = new XYSeries(metric.label());
			game.curve().stream()
					.filter(point -> point.turn() >= game.turnJoined() && point.turn() <= game.lastAliveTurn())
					.forEach(point -> series.add(point.turn(), metric.value().applyAsDouble(point)));
			dataset.addSeries(series);
		});
		JFreeChart chart = ChartFactory.createXYLineChart(
				String.format("%s in %s: %s", game.race(), game.gameName().strip(), title), "Zug", title, dataset);
		((XYLineAndShapeRenderer) chart.getXYPlot().getRenderer()).setDefaultShapesVisible(false);
		if ("Rang".equals(title)) {
			chart.getXYPlot().getRangeAxis().setInverted(true);
		}
		game.alliances().forEach(alliance -> chart.getXYPlot().addDomainMarker(new ValueMarker(alliance.turn())));
		SVGGraphics2D g2 = new SVGGraphics2D(1200, 450);
		chart.draw(g2, new java.awt.Rectangle(1200, 450));
		return String.format("<div style=\"width:100%%;max-width:1200px\">%s</div>",
				g2.getSVGElement(null, false, new ViewBox(0, 0, 1200, 450), null, null));
	}
}
