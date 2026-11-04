package de.witchcafe.planets.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import de.witchcafe.planets.PlayerPairService;
import de.witchcafe.planets.PlayerPairService.PlayerPair;
import de.witchcafe.planets.PlayerPairService.SharedGame;

/**
 * Lists pairs of players who played at least n games together; selecting a pair shows the shared games.
 * Query parameters min (default 5) and player preset the filters, e.g. /playerpairs?min=10&player=king
 */
@Route("playerpairs")
@PageTitle("Spielerpaare")
@AnonymousAllowed
@Menu(order = 0, icon = "vaadin:users", title = "Spielerpaare")
public class PlayerPairsView extends VerticalLayout implements BeforeEnterObserver {

	private static final int DEFAULT_MIN_GAMES = 5;

	private final PlayerPairService pairService;
	private final IntegerField minGames = new IntegerField("Mindestens gemeinsame Spiele");
	private final TextField playerFilter = new TextField("Spieler");
	private final Span summary = new Span();
	private final Grid<PlayerPair> pairGrid = new Grid<>();
	private final ListDataProvider<PlayerPair> pairs = new ListDataProvider<>(new ArrayList<>());
	private final H4 gamesTitle = new H4();
	private final Grid<SharedGame> gameGrid = new Grid<>();

	PlayerPairsView(PlayerPairService pairService) {
		this.pairService = pairService;

		minGames.setMin(1);
		minGames.setStepButtonsVisible(true);
		minGames.setValue(DEFAULT_MIN_GAMES);
		minGames.addValueChangeListener(event -> load());
		playerFilter.setPlaceholder("Name enthält …");
		playerFilter.setClearButtonVisible(true);
		playerFilter.setValueChangeMode(ValueChangeMode.LAZY);
		playerFilter.addValueChangeListener(event -> applyFilter());
		HorizontalLayout filters = new HorizontalLayout(minGames, playerFilter, summary);
		filters.setAlignItems(FlexComponent.Alignment.BASELINE);

		pairGrid.addColumn(PlayerPair::playerA).setHeader("Spieler A").setSortable(true).setAutoWidth(true);
		pairGrid.addColumn(PlayerPair::playerB).setHeader("Spieler B").setSortable(true).setAutoWidth(true);
		pairGrid.addColumn(PlayerPair::games).setHeader("Gemeinsame Spiele").setSortable(true).setAutoWidth(true);
		pairGrid.addComponentColumn(pair -> new HorizontalLayout(graphButton(pair.playerA(), pair.accountidA()),
				graphButton(pair.playerB(), pair.accountidB()))).setHeader("Konstellationen").setAutoWidth(true);
		pairGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
		pairGrid.setDataProvider(pairs);
		pairGrid.asSingleSelect().addValueChangeListener(event -> showGames(event.getValue()));
		pairGrid.setSizeFull();

		gameGrid.addColumn(game -> game.datecreated() == null ? "" : game.datecreated().substring(0, 10))
				.setHeader("Erstellt").setAutoWidth(true).setFlexGrow(0);
		gameGrid.addComponentColumn(game -> new RouterLink(String.format("%s [%s]", game.name(), game.gameId()),
				GameDetailsView.class, new RouteParameters("gameId", game.gameId()))).setHeader("Spiel").setAutoWidth(true);
		gameGrid.addColumn(SharedGame::statusname).setHeader("Status").setAutoWidth(true);
		gameGrid.addColumn(SharedGame::turn).setHeader("Zug").setAutoWidth(true);
		gameGrid.addColumn(SharedGame::raceA).setHeader("Rasse A").setAutoWidth(true);
		gameGrid.addColumn(SharedGame::finishrankA).setHeader("Platz A").setAutoWidth(true);
		gameGrid.addColumn(SharedGame::raceB).setHeader("Rasse B").setAutoWidth(true);
		gameGrid.addColumn(SharedGame::finishrankB).setHeader("Platz B").setAutoWidth(true);
		gameGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
		gameGrid.setSizeFull();

		VerticalLayout gameArea = new VerticalLayout(gamesTitle, gameGrid);
		gameArea.setPadding(false);
		gameArea.setSizeFull();
		SplitLayout split = new SplitLayout(pairGrid, gameArea, SplitLayout.Orientation.VERTICAL);
		split.setSplitterPosition(60);
		split.setSizeFull();

		add(filters, new Span("Zählt nur Spieler, die in der Datenbank geladen sind."), split);
		setSizeFull();
		showGames(null);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		QueryParameters params = event.getLocation().getQueryParameters();
		params.getSingleParameter("player").ifPresent(playerFilter::setValue);
		Integer min = params.getSingleParameter("min").map(value -> {
			try {
				return Integer.valueOf(value);
			} catch (NumberFormatException e) {
				return null;
			}
		}).orElse(null);
		if (min != null && !min.equals(minGames.getValue())) {
			minGames.setValue(min); // triggers load()
		} else {
			load();
		}
	}

	private Button graphButton(String name, String accountid) {
		Button button = new Button(name, event -> getUI().ifPresent(ui -> ui.navigate("graph",
				QueryParameters.simple(Map.of("player", accountid)))));
		button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
		button.setEnabled(accountid != null);
		return button;
	}

	private void load() {
		int min = minGames.getValue() == null ? DEFAULT_MIN_GAMES : minGames.getValue();
		List<PlayerPair> found = pairService.findPairs(min);
		pairs.getItems().clear();
		pairs.getItems().addAll(found);
		applyFilter();
	}

	private void applyFilter() {
		String filter = playerFilter.getValue() == null ? "" : playerFilter.getValue().strip().toLowerCase();
		pairs.setFilter(pair -> filter.isEmpty() || pair.playerA().toLowerCase().contains(filter)
				|| pair.playerB().toLowerCase().contains(filter));
		long shown = pairs.size(new Query<>());
		summary.setText(String.format("%d Paare", shown));
	}

	private void showGames(PlayerPair pair) {
		if (pair == null) {
			gamesTitle.setText("Paar auswählen, um die gemeinsamen Spiele zu sehen");
			gameGrid.setItems(List.of());
			return;
		}
		List<SharedGame> games = pairService.findSharedGames(pair);
		gamesTitle.setText(String.format("%s & %s – %d gemeinsame Spiele", pair.playerA(), pair.playerB(), games.size()));
		gameGrid.setItems(games);
	}
}
