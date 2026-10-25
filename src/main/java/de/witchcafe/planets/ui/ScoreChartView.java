package de.witchcafe.planets.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.svg.SVGGraphics2D;
import org.jfree.svg.SVGUtils;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import de.witchcafe.planets.Game;
import de.witchcafe.planets.GamesService;
import de.witchcafe.planets.Score;

@PageTitle("Score List")
@AnonymousAllowed
@Route("shipscores/:gameId/:gameType")
public class ScoreChartView extends VerticalLayout implements BeforeEnterObserver {

	private final GamesService gamesService;

	private ListDataProvider<Score> dataprovider;

	ScoreChartView(GamesService gamesService) {
		this.gamesService = gamesService;

		
	}

	private String gameId;
	private Game game;

	HashMap<String, List<Double>> series = new HashMap<String, List<Double>>();

	private static HashMap<String, String> mappedTitles = new HashMap<String, String>(){{
		put("planets","Planets by Turn");
		put("freighters","Freighters by Turn");
		put("starbases","Starbases by Turn");
		put("capitalships","Capitalships by Turn");
	}};
    @Override
	public void beforeEnter(BeforeEnterEvent event) {
        gameId= event.getRouteParameters().get("gameId").orElse("no ID");
        String type = event.getRouteParameters().get("gameType").orElse("planets");
        game = gamesService.getGame(gameId);

		String svgContent = createLineChartSvg(type, gamesService.getScoreSeriesbyGameid(gameId,type));

        Html chartHtml = new Html(svgContent);
        chartHtml.getStyle().setHeight("800");
        chartHtml.getStyle().setWidth("100%");

        add(new H2(String.format("%s[%s]",this.game.getName(),gameId)), chartHtml);
	}
	
	public void exportAsSVG(JFreeChart chart, int width, int height) {
	    String svgElement = createSVG(chart, width, height);
	    
	    // 4. Optional: Direkt in eine Datei speichern
	    try {
	        SVGUtils.writeToSVG(new File("mein_chart.svg"), svgElement);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

	private String createSVG(JFreeChart chart, int width, int height) {
		// 1. Erstelle den SVG-Kontext
	    SVGGraphics2D g2 = new SVGGraphics2D(width, height);
	    
	    // 2. Zeichne das Chart in den Kontext
	    chart.draw(g2, new java.awt.Rectangle(width, height));
	    
	    // 3. Generiere das SVG-Element (als String)
	    String svgElement = g2.getSVGElement();
		return svgElement;
	}
	
	public String createLineChartSvg(String chartType, HashMap<String, HashMap<Integer, Double>> dataSeries) {

		String chartTitle = mappedTitles.get(chartType);
	    XYSeriesCollection dataset = new XYSeriesCollection();

		dataSeries.keySet().forEach(key -> {
		    XYSeries series = new XYSeries(key);
		    HashMap<Integer, Double> dataSerie = dataSeries.get(key);
		    ArrayList<Integer> xValues = new ArrayList<Integer>();
		    xValues.addAll(dataSerie.keySet());
		    Collections.sort(xValues);
		    xValues.forEach(xValue -> {
			    series.add(xValue, dataSerie.get(xValue));
		    });
		    dataset.addSeries(series);
		});

	    // 3. Chart erstellen (Line Chart)
	    JFreeChart chart = ChartFactory.createXYLineChart(
	            chartTitle, // Titel
	            "turn",    // X-Achsen Label
	            chartType,     // Y-Achsen Label
	            dataset                 // Daten
	    );

	    // 4. Mit JFreeSVG in einen String rendern
	    SVGGraphics2D g2 = new SVGGraphics2D(1200, 600);
	    chart.draw(g2, new java.awt.Rectangle(1200, 600));
	    
	    return g2.getSVGElement();
	}
	
	private void hashScore(Score score) {
		List<Double> serieByAccount = series.get(score.getAccountid());
		if (serieByAccount == null) {
			series.put(score.getAccountid(), new ArrayList<Double>());
			serieByAccount = series.get(score.getAccountid());
		}
		serieByAccount.add(Double.valueOf(score.getPlanets()));		
	}

}
