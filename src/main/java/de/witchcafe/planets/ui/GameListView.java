package de.witchcafe.planets.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.witchcafe.base.ui.component.ViewToolbar;
import de.witchcafe.planets.Game;
import de.witchcafe.planets.GamesService;

@Route("Games")
@PageTitle("Game List")
@AnonymousAllowed

@Menu(order = 0, icon = "vaadin:clipboard-check", title = "Game List")
class GameListView extends Main {

    private final GamesService gamesService;

    final Grid<Game> gameGrid;

	private ListDataProvider<Game> dataProvider;

    GameListView(GamesService gamesService) {
        this.gamesService = gamesService;

        gameGrid = new Grid<>(Game.class);
        gameGrid.getColumnByKey("events").setVisible(false);
        gameGrid.getColumnByKey("scores").setVisible(false);
        gameGrid.getColumnByKey("scoresLoaded").setVisible(false);
        gameGrid.getColumnByKey("detailsLoaded").setVisible(false);
        gameGrid.getColumnByKey("gameId").setVisible(false);
        gameGrid.getColumnByKey("turn").setWidth("10%");
        gameGrid.getColumnByKey("datecreated").setWidth("20%");
        SerializableFunction<Game, String> nameToolTip = new SerializableFunction<Game, String>(){

			@Override
			public String apply(Game game) {
				return game.getGameId();
			}
        	
        };
		gameGrid.getColumnByKey("name").setTooltipGenerator(nameToolTip );
        Comparator<Game> turnComparator = new Comparator<Game>() {

			@Override
			public int compare(Game o1, Game o2) {
				Double turn1 = (double) 0;
				try {
					turn1 = Double.valueOf(o1.getTurn());
				}
				catch (Exception exc) {
					System.err.println(exc.getMessage());
				}
				Double turn2 = (double) 0;
				try {
					turn2 = Double.valueOf(o2.getTurn());
				}
				catch (Exception exc) {
					System.err.println(exc.getMessage());
				}
				return (int)(turn1 - turn2);
			}
        	
        };
		gameGrid.getColumnByKey("turn").setComparator(turnComparator );
        dataProvider = new ListDataProvider<>(new ArrayList());
        
        gameGrid.setColumnOrder(
        		gameGrid.getColumnByKey("datecreated"), 
        		gameGrid.getColumnByKey("name"), 
        		gameGrid.getColumnByKey("statusname"), 
        		gameGrid.getColumnByKey("turn"),
        		gameGrid.getColumnByKey("events"),
        		gameGrid.getColumnByKey("gameId"),
        		gameGrid.getColumnByKey("scores"),
        		gameGrid.getColumnByKey("scoresLoaded"),
        		gameGrid.getColumnByKey("detailsLoaded"));

        gameGrid.setDataProvider(dataProvider);
        // gameGrid.addColumn(Game::getName).setHeader("Name").setFlexGrow(30).setWidth("0px");
        // gameGrid.addColumn(Game::getStatusname).setHeader("Status").setFlexGrow(10).setWidth("0px");

        gameGrid.addComponentColumn(item -> {
            HorizontalLayout actions = new HorizontalLayout();
            
            Button editBtn = new Button(new Icon(VaadinIcon.EDIT));
            editBtn.addClickListener(e -> System.out.println(item.getGameId()+"\t"+item.getName()));
            
            Button deleteBtn = new Button(new Icon(VaadinIcon.TRASH));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteBtn.addClickListener(e -> System.out.println(item.getGameId()+"\t"+item.getName()));
            item.getScores();
            
            RouterLink reportLink = getReportLink(item, "Game Scores");
            reportLink.setEnabled(item.getScoresLoaded());
			RouterLink detailsLink = getDetailsLink(item, "Game Details");
			detailsLink.setEnabled(item.getDetailsLoaded());
			actions.add(
            		detailsLink,
            		getEventsLink(item, "Game Events"),
            		reportLink,
            		getChartLink(item, "planets", "Planets Chart"),
            		getChartLink(item, "capitalships", "Capitalships Chart"),
            		getChartLink(item, "freighters", "Freighters Chart"),
            		getChartLink(item, "starbases", "Starbases Chart"),
            		editBtn, deleteBtn);
            
            return actions;
        }).setHeader("Optionen").setFlexGrow(50).setWidth("0px");
        gameGrid.setSizeFull();

        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar("Game List", ViewToolbar.group(getSearchField())));
        add(gameGrid);
    }
    
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        List<Game> all = gamesService.findAll();
        
		dataProvider.getItems().clear();
		dataProvider.getItems().addAll(all);
		dataProvider.refreshAll();	
    }

	private RouterLink getChartLink(Game item, String type, String titles) {
		RouteParameters params = new RouteParameters(
		        new RouteParam("gameId", item.getGameId()),
		        new RouteParam("gameType", type)
		    );
		RouterLink link = new RouterLink(titles, ScoreChartView.class,params);
		return link;
	}

	private RouterLink getReportLink(Game item, String titles) {
		RouteParameters params = new RouteParameters(
		        new RouteParam("gameId", item.getGameId())
		        // ,new RouteParam("gameType", type)
		    );
		RouterLink link = new RouterLink(titles, ScoreListView.class,params);
		return link;
	}

	private RouterLink getDetailsLink(Game item,String titles) {
		RouteParameters params = new RouteParameters(
		        new RouteParam("gameId", item.getGameId())
		        // ,new RouteParam("gameType", type)
		    );
		RouterLink link = new RouterLink(titles, GameDetailsView.class,params);
		return link;
	}

	private RouterLink getEventsLink(Game item,String titles) {
		RouteParameters params = new RouteParameters(
		        new RouteParam("gameId", item.getGameId())
		        // ,new RouteParam("gameType", type)
		    );
		RouterLink link = new RouterLink(titles, GameEventsView.class,params);
		return link;
	}
	
	private TextField getSearchField() {
		// Das Suchfeld erstellen
		TextField searchField = new TextField();
		searchField.setPlaceholder("Suche nach Name oder Typ...");
		searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
		searchField.setClearButtonVisible(true); // Erlaubt schnelles Löschen der Suche
		searchField.setValueChangeMode(ValueChangeMode.EAGER); // Filtert sofort beim Tippen

		// Die Filter-Logik
		searchField.addValueChangeListener(event -> {
		    String searchTerm = event.getValue().trim().toLowerCase();
		    
		    dataProvider.setFilter(game -> {
		        if (searchTerm.isEmpty()) return true;
		        
		        boolean matches = game.getName() != null && 
		                              game.getName().toLowerCase().contains(searchTerm);
		        matches |= game.getStatusname() != null && 
		                              game.getStatusname().toLowerCase().contains(searchTerm);
		        return matches;
		    });
		});
		return searchField;
	}

	private NativeButton getScoresButton(String gameId) {
		NativeButton button = new NativeButton(
		        "Navigate to scores:"+ gameId);
		button.addClickListener(e ->
		     button.getUI().ifPresent(ui ->
		           ui.navigate("Scores/"+gameId)));
		return button;
	}
}
