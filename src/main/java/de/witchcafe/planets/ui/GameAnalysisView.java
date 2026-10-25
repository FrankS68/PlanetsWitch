package de.witchcafe.planets.ui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.svg.SVGGraphics2D;
import org.jfree.svg.ViewBox;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import de.witchcafe.planets.GameAnalysis;
import de.witchcafe.planets.GameAnalysis.Contact;
import de.witchcafe.planets.GameAnalysis.EconomyTurn;
import de.witchcafe.planets.GameAnalysis.Message;
import de.witchcafe.planets.GameAnalysis.ShipBuild;
import de.witchcafe.planets.GameAnalysis.TechChange;
import de.witchcafe.planets.GameAnalysisService;

/**
 * Shows the analyses of the top officers' game archives (tools/game_analysis.py):
 * opening economy, ship builds with equipment and the communication with the other players.
 * Query parameters game and slot preselect an analysis, e.g. /analysis?game=633782&slot=7
 */
@Route("analysis")
@PageTitle("Spielanalyse")
@AnonymousAllowed
@Menu(order = 0, icon = "vaadin:line-chart", title = "Spielanalyse")
public class GameAnalysisView extends VerticalLayout implements BeforeEnterObserver {

	private record Metric(String label, ToDoubleFunction<EconomyTurn> value) {
	}

	private static final LinkedHashMap<String, List<Metric>> METRIC_GROUPS = new LinkedHashMap<String, List<Metric>>() {{
		put("Infrastruktur", List.of(
				new Metric("Minen", EconomyTurn::mines),
				new Metric("Fabriken", EconomyTurn::factories),
				new Metric("Verteidigung", EconomyTurn::defense)));
		put("Planeten & Schiffe", List.of(
				new Metric("Planeten", EconomyTurn::planets),
				new Metric("Kriegsschiffe", EconomyTurn::warships),
				new Metric("Frachter", EconomyTurn::freighters),
				new Metric("Starbases", EconomyTurn::starbases)));
		put("Geld & Supplies", List.of(
				new Metric("Megacredits", EconomyTurn::megacredits),
				new Metric("Supplies", EconomyTurn::supplies)));
		put("Mineralien", List.of(
				new Metric("Neutronium", EconomyTurn::neutronium),
				new Metric("Duranium", EconomyTurn::duranium),
				new Metric("Tritanium", EconomyTurn::tritanium),
				new Metric("Molybdenum", EconomyTurn::molybdenum)));
		put("Kolonisten", List.of(
				new Metric("Clans", EconomyTurn::clans)));
	}};

	private final GameAnalysisService analysisService;
	private final Select<GameAnalysis> analysisSelect = new Select<>();
	private final VerticalLayout content = new VerticalLayout();

	GameAnalysisView(GameAnalysisService analysisService) {
		this.analysisService = analysisService;
		analysisSelect.setLabel("Spiel / Topofficer");
		analysisSelect.setItemLabelGenerator(GameAnalysis::getLabel);
		analysisSelect.setWidth("40em");
		analysisSelect.addValueChangeListener(event -> show(event.getValue()));
		content.setPadding(false);
		content.setSizeFull();
		add(analysisSelect, content);
		setSizeFull();
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		List<GameAnalysis> analyses = analysisService.findAll();
		analysisSelect.setItems(analyses);
		if (analyses.isEmpty()) {
			content.removeAll();
			content.add(new Span(String.format(
					"Noch keine Analysen in %s. Erzeugen mit: python -I tools/game_analysis.py",
					analysisService.getAnalysisDir().toAbsolutePath())));
			return;
		}
		String game = event.getLocation().getQueryParameters().getSingleParameter("game").orElse(null);
		String slot = event.getLocation().getQueryParameters().getSingleParameter("slot").orElse(null);
		analysisSelect.setValue(analyses.stream()
				.filter(analysis -> game == null || String.valueOf(analysis.gameId()).equals(game))
				.filter(analysis -> slot == null || String.valueOf(analysis.slot()).equals(slot))
				.findFirst().orElse(analyses.get(0)));
	}

	private void show(GameAnalysis analysis) {
		content.removeAll();
		if (analysis == null) {
			return;
		}
		content.add(createSummary(analysis));
		TabSheet tabs = new TabSheet();
		tabs.setWidthFull();
		tabs.add("Wirtschaft", createEconomyTab(analysis));
		tabs.add("Tech", createTechGrid(analysis.tech()));
		tabs.add("Schiffe", createShipsTab(analysis.builds()));
		tabs.add("Kontakte", createContactGrid(analysis.contacts()));
		tabs.add("Nachrichten", createMessagesTab(analysis));
		content.add(tabs);
	}

	private VerticalLayout createSummary(GameAnalysis analysis) {
		GameAnalysis.Milestones milestones = analysis.milestones();
		VerticalLayout summary = new VerticalLayout();
		summary.setPadding(false);
		summary.setSpacing(false);
		summary.add(new H3(String.format("%s in %s (Slot %d, %d Züge%s)", analysis.username(), analysis.gameName(),
				analysis.slot(), analysis.turns(),
				analysis.finishRank() != null && analysis.finishRank() > 0 ? ", Platz " + analysis.finishRank() : "")));
		summary.add(new Span(milestones.firstWarshipTurn() == null ? "Kein eigenes Kriegsschiff gebaut."
				: String.format("Erstes eigenes Kriegsschiff in Zug %d: %s, davor %d Frachter.",
						milestones.firstWarshipTurn(), milestones.firstWarship(), milestones.freightersBeforeFirstWarship())));
		summary.add(new Span(milestones.firstOwnMessageTurn() == null ? "Hat selbst keine Nachrichten geschrieben."
				: String.format("Erste eigene Nachricht in Zug %d, an %d Spieler geschrieben.",
						milestones.firstOwnMessageTurn(), milestones.contactsWritten())));
		String opening = milestones.opening().values().stream()
				.map(turn -> String.format("Zug %d: %d Planeten, %d Minen, %d Fabriken, %d Kriegsschiffe",
						turn.turn(), turn.planets(), turn.mines(), turn.factories(), turn.warships()))
				.collect(Collectors.joining(" · "));
		summary.add(new Span(opening));
		return summary;
	}

	private VerticalLayout createEconomyTab(GameAnalysis analysis) {
		Select<String> groupSelect = new Select<>();
		groupSelect.setLabel("Werte");
		groupSelect.setItems(METRIC_GROUPS.keySet());
		groupSelect.setValue("Infrastruktur");
		Select<Integer> turnSelect = new Select<>();
		turnSelect.setLabel("Züge");
		turnSelect.setItems(20, 30, 50, analysis.turns());
		turnSelect.setItemLabelGenerator(turns -> turns == analysis.turns() ? "alle" : "1–" + turns);
		turnSelect.setValue(30);
		VerticalLayout chartArea = new VerticalLayout();
		chartArea.setPadding(false);
		Runnable refresh = () -> {
			chartArea.removeAll();
			chartArea.add(new Html(createChartSvg(analysis, groupSelect.getValue(), turnSelect.getValue())));
		};
		groupSelect.addValueChangeListener(event -> refresh.run());
		turnSelect.addValueChangeListener(event -> refresh.run());
		refresh.run();
		VerticalLayout tab = new VerticalLayout(new HorizontalLayout(groupSelect, turnSelect), chartArea);
		tab.setPadding(false);
		return tab;
	}

	private String createChartSvg(GameAnalysis analysis, String group, int maxTurn) {
		XYSeriesCollection dataset = new XYSeriesCollection();
		METRIC_GROUPS.get(group).forEach(metric -> {
			XYSeries series = new XYSeries(metric.label());
			analysis.economy().stream().filter(turn -> turn.turn() <= maxTurn)
					.forEach(turn -> series.add(turn.turn(), metric.value().applyAsDouble(turn)));
			dataset.addSeries(series);
		});
		JFreeChart chart = ChartFactory.createXYLineChart(
				String.format("%s: %s", analysis.username(), group), "Zug", group, dataset);
		((XYLineAndShapeRenderer) chart.getXYPlot().getRenderer()).setDefaultShapesVisible(maxTurn <= 50);
		SVGGraphics2D g2 = new SVGGraphics2D(1200, 500);
		chart.draw(g2, new java.awt.Rectangle(1200, 500));
		return String.format("<div style=\"width:100%%;max-width:1200px\">%s</div>",
				g2.getSVGElement(null, false, new ViewBox(0, 0, 1200, 500), null, null));
	}

	private Grid<TechChange> createTechGrid(List<TechChange> tech) {
		Grid<TechChange> grid = new Grid<>();
		grid.addColumn(TechChange::turn).setHeader("Zug").setAutoWidth(true).setFlexGrow(0);
		grid.addColumn(TechChange::planetId).setHeader("Starbase (Planet)").setAutoWidth(true);
		grid.addColumn(TechChange::hull).setHeader("Hüllen").setAutoWidth(true);
		grid.addColumn(TechChange::engine).setHeader("Triebwerke").setAutoWidth(true);
		grid.addColumn(TechChange::beam).setHeader("Strahlwaffen").setAutoWidth(true);
		grid.addColumn(TechChange::torpedo).setHeader("Torpedos").setAutoWidth(true);
		grid.setItems(tech);
		grid.setHeight("40em");
		return grid;
	}

	private static String weapons(ShipBuild ship) {
		StringBuilder weapons = new StringBuilder();
		if (ship.beams() > 0) {
			weapons.append(ship.beams()).append("× ").append(ship.beam());
		}
		if (ship.launchers() > 0) {
			weapons.append(weapons.isEmpty() ? "" : ", ").append(ship.launchers()).append("× ").append(ship.torpedo());
		}
		if (ship.bays() > 0) {
			weapons.append(weapons.isEmpty() ? "" : ", ").append(ship.bays()).append(" Hangars");
		}
		return weapons.toString();
	}

	private record Loadout(String hull, String engine, String weapons, long count, int firstTurn) {
	}

	private VerticalLayout createShipsTab(List<ShipBuild> builds) {
		Checkbox ownOnly = new Checkbox("nur selbst gebaute", true);
		Checkbox warshipsOnly = new Checkbox("nur Kriegsschiffe", false);

		Grid<Loadout> loadoutGrid = new Grid<>();
		loadoutGrid.addColumn(Loadout::count).setHeader("Anzahl").setAutoWidth(true).setFlexGrow(0);
		loadoutGrid.addColumn(Loadout::firstTurn).setHeader("Erstmals in Zug").setAutoWidth(true).setFlexGrow(0);
		loadoutGrid.addColumn(Loadout::hull).setHeader("Hülle").setAutoWidth(true);
		loadoutGrid.addColumn(Loadout::engine).setHeader("Triebwerk").setAutoWidth(true);
		loadoutGrid.addColumn(Loadout::weapons).setHeader("Bewaffnung").setAutoWidth(true);
		loadoutGrid.setHeight("20em");

		Grid<ShipBuild> buildGrid = new Grid<>();
		buildGrid.addColumn(ShipBuild::turn).setHeader("Zug").setAutoWidth(true).setFlexGrow(0);
		buildGrid.addColumn(ShipBuild::shipId).setHeader("ID").setAutoWidth(true).setFlexGrow(0);
		buildGrid.addColumn(ShipBuild::origin).setHeader("Herkunft").setAutoWidth(true);
		buildGrid.addColumn(ShipBuild::hull).setHeader("Hülle").setAutoWidth(true);
		buildGrid.addColumn(ShipBuild::engine).setHeader("Triebwerk").setAutoWidth(true);
		buildGrid.addColumn(GameAnalysisView::weapons).setHeader("Bewaffnung").setAutoWidth(true);
		buildGrid.setHeight("30em");

		Runnable refresh = () -> {
			List<ShipBuild> ships = builds.stream()
					.filter(ship -> !ownOnly.getValue() || "gebaut".equals(ship.origin()))
					.filter(ship -> !warshipsOnly.getValue() || ship.warship())
					.toList();
			buildGrid.setItems(ships);
			Map<List<String>, List<ShipBuild>> byLoadout = ships.stream().collect(Collectors.groupingBy(
					ship -> List.of(ship.hull(), ship.engine(), weapons(ship)), LinkedHashMap::new, Collectors.toList()));
			loadoutGrid.setItems(byLoadout.entrySet().stream()
					.map(entry -> new Loadout(entry.getKey().get(0), entry.getKey().get(1), entry.getKey().get(2),
							entry.getValue().size(), entry.getValue().get(0).turn()))
					.sorted((a, b) -> Long.compare(b.count(), a.count()))
					.toList());
		};
		ownOnly.addValueChangeListener(event -> refresh.run());
		warshipsOnly.addValueChangeListener(event -> refresh.run());
		refresh.run();
		VerticalLayout tab = new VerticalLayout(new HorizontalLayout(ownOnly, warshipsOnly),
				new H3("Ausrüstungsvarianten"), loadoutGrid, new H3("Alle Schiffe"), buildGrid);
		tab.setPadding(false);
		return tab;
	}

	private Grid<Contact> createContactGrid(List<Contact> contacts) {
		Grid<Contact> grid = new Grid<>();
		grid.addColumn(Contact::slot).setHeader("Slot").setAutoWidth(true).setFlexGrow(0);
		grid.addColumn(Contact::username).setHeader("Spieler").setAutoWidth(true);
		grid.addColumn(Contact::race).setHeader("Rasse").setAutoWidth(true);
		grid.addColumn(Contact::ambassadorSentTurn).setHeader("Botschafter gesendet (Zug)").setAutoWidth(true);
		grid.addColumn(Contact::ambassadorReceivedTurn).setHeader("Botschafter erhalten (Zug)").setAutoWidth(true);
		grid.addColumn(Contact::firstMessageTurn).setHeader("Erste Nachricht (Zug)").setAutoWidth(true);
		grid.addColumn(Contact::sent).setHeader("Geschrieben").setAutoWidth(true);
		grid.addColumn(Contact::received).setHeader("Erhalten").setAutoWidth(true);
		grid.setItems(contacts);
		grid.setAllRowsVisible(true);
		return grid;
	}

	private VerticalLayout createMessagesTab(GameAnalysis analysis) {
		Select<Contact> partnerSelect = new Select<>();
		partnerSelect.setLabel("Gesprächspartner");
		partnerSelect.setEmptySelectionAllowed(true);
		partnerSelect.setEmptySelectionCaption("alle");
		partnerSelect.setItems(analysis.contacts().stream().filter(contact -> contact.sent() + contact.received() > 0).toList());
		partnerSelect.setItemLabelGenerator(contact -> contact == null ? "alle"
				: String.format("%s (%s)", contact.username(), contact.race()));

		Grid<Message> grid = new Grid<>();
		grid.addColumn(Message::turn).setHeader("Zug").setAutoWidth(true).setFlexGrow(0);
		grid.addColumn(message -> "out".equals(message.direction()) ? "→ an" : "← von")
				.setHeader("").setAutoWidth(true).setFlexGrow(0);
		grid.addColumn(message -> String.join(", ", message.recipients())).setHeader("Spieler").setAutoWidth(true).setFlexGrow(0);
		grid.addColumn(Message::text).setHeader("Text").setFlexGrow(1);
		grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
		grid.setHeight("40em");
		grid.setItems(analysis.messages());
		partnerSelect.addValueChangeListener(event -> grid.setItems(analysis.messages().stream()
				.filter(message -> event.getValue() == null || Objects.equals(message.slot(), event.getValue().slot())
						|| message.recipients().contains(event.getValue().username()))
				.toList()));
		VerticalLayout tab = new VerticalLayout(partnerSelect, grid);
		tab.setPadding(false);
		return tab;
	}
}
