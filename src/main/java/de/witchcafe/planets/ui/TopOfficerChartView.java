package de.witchcafe.planets.ui;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.svg.SVGGraphics2D;
import org.jfree.svg.ViewBox;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import de.witchcafe.planets.OfficerSnapshot;
import de.witchcafe.planets.OfficerSnapshotRepository;

@Route("topofficers")
@PageTitle("Top Officers")
@AnonymousAllowed
@Menu(order = 1, icon = "vaadin:trophy", title = "Top Officers")
public class TopOfficerChartView extends VerticalLayout {

	private static final String[] RACES = {
			"", "Solar Federation", "Lizard Alliance", "Empire of the Birds", "Hordes of Fury",
			"Privateer Bands", "Cyborg", "Crystal Confederation", "Evil Empire",
			"Robotic Imperium", "Rebel Confederation", "Missing Colonies of Man", "Horwasp Plague"
	};

	private static final LinkedHashMap<String, String> METRICS = new LinkedHashMap<String, String>() {{
		put("rating", "Rating");
		put("rank", "Platz");
		put("experience", "Erfahrung");
		put("tonssunk", "Versenkte Tonnage");
	}};

	private final OfficerSnapshotRepository snapshotRepository;
	private final Select<Integer> raceSelect = new Select<>();
	private final Select<String> metricSelect = new Select<>();
	private final VerticalLayout chartArea = new VerticalLayout();

	TopOfficerChartView(OfficerSnapshotRepository snapshotRepository) {
		this.snapshotRepository = snapshotRepository;

		raceSelect.setLabel("Rasse");
		raceSelect.setItems(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
		raceSelect.setItemLabelGenerator(raceId -> RACES[raceId]);
		raceSelect.setValue(1);
		raceSelect.addValueChangeListener(event -> refresh());

		metricSelect.setLabel("Wert");
		metricSelect.setItems(METRICS.keySet());
		metricSelect.setItemLabelGenerator(METRICS::get);
		metricSelect.setValue("rating");
		metricSelect.addValueChangeListener(event -> refresh());

		chartArea.setPadding(false);
		add(new HorizontalLayout(raceSelect, metricSelect), chartArea);
		setSizeFull();
		refresh();
	}

	private void refresh() {
		chartArea.removeAll();
		List<OfficerSnapshot> snapshots;
		try {
			snapshots = snapshotRepository.findByRaceidOrderByDayAsc(raceSelect.getValue());
		}
		catch (Exception exc) {
			System.err.println(String.format("loading top officer snapshots failed: %s", exc.getMessage()));
			chartArea.add(new Span("Die Datenbank ist nicht erreichbar. Läuft Neo4j?"));
			return;
		}
		if (snapshots.isEmpty()) {
			chartArea.add(new Span("Noch keine Snapshots für diese Rasse. Der Job lädt das Leaderboard täglich."));
			return;
		}
		chartArea.add(new Html(createChartSvg(snapshots, metricSelect.getValue())));
	}

	private String createChartSvg(List<OfficerSnapshot> snapshots, String metric) {
		LinkedHashMap<String, TimeSeries> seriesByOfficer = new LinkedHashMap<String, TimeSeries>();
		snapshots.forEach(snapshot -> {
			TimeSeries series = seriesByOfficer.computeIfAbsent(snapshot.getOfficerId(),
					officerId -> new TimeSeries(snapshot.getUsername()));
			LocalDate day = snapshot.getDay();
			series.addOrUpdate(new Day(day.getDayOfMonth(), day.getMonthValue(), day.getYear()), value(snapshot, metric));
		});
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		seriesByOfficer.values().forEach(dataset::addSeries);

		JFreeChart chart = ChartFactory.createTimeSeriesChart(
				String.format("Top Officers %s: %s", RACES[raceSelect.getValue()], METRICS.get(metric)),
				"Tag",
				METRICS.get(metric),
				dataset);
		XYPlot plot = chart.getXYPlot();
		if ("rank".equals(metric)) {
			// place 1 on top
			plot.getRangeAxis().setInverted(true);
		}
		// markers, so single days are visible before there is a line
		((XYLineAndShapeRenderer) plot.getRenderer()).setDefaultShapesVisible(true);
		((DateAxis) plot.getDomainAxis()).setDateFormatOverride(new SimpleDateFormat("dd.MM.yy"));

		SVGGraphics2D g2 = new SVGGraphics2D(1200, 600);
		chart.draw(g2, new java.awt.Rectangle(1200, 600));
		// scale with the page instead of a fixed 1200px
		return String.format("<div style=\"width:100%%;max-width:1200px\">%s</div>",
				g2.getSVGElement(null, false, new ViewBox(0, 0, 1200, 600), null, null));
	}

	private static Number value(OfficerSnapshot snapshot, String metric) {
		switch (metric) {
		case "rank":
			return snapshot.getRank();
		case "experience":
			return snapshot.getExperience();
		case "tonssunk":
			return snapshot.getTonssunk();
		default:
			return snapshot.getRating();
		}
	}
}
