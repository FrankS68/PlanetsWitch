package de.witchcafe.planets.ui;

import java.util.ArrayList;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.witchcafe.base.ui.component.ViewToolbar;
import de.witchcafe.planets.Game;
import de.witchcafe.planets.GamesService;
import de.witchcafe.planets.Score;

@PageTitle("Raw Score List")
@AnonymousAllowed
// @Menu(order = 0, icon = "vaadin:clipboard-check", title = "Raw Score List")
@Route("rawscores/:gameId?")
class ScoreListView extends Main implements BeforeEnterObserver{

    private final GamesService gamesService;

    final Grid<Score> scoreGrid;

	private ListDataProvider<Score> dataProvider;

    ScoreListView(GamesService gamesService) {
        this.gamesService = gamesService;


        scoreGrid = new Grid<>(Score.class);
        dataProvider = new ListDataProvider<Score>(new ArrayList<Score>());

        scoreGrid.setDataProvider(dataProvider);

        scoreGrid.setColumns("turn", "name", "planets", "starbases", "capitalships", "freighters");
        scoreGrid.getColumnByKey("name").setHeader("blafasel");

        scoreGrid.setSizeFull();

        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar("Score List", ViewToolbar.group(getSearchField())));
        add(scoreGrid);
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
		        /*
		        matches |= game.getStatusname() != null && 
		                              game.getStatusname().toLowerCase().contains(searchTerm);
		                              */
		        return matches;
		    });
		});
		return searchField;
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
        String id = event.getRouteParameters().get("gameId").orElse("no ID");
        String type = event.getRouteParameters().get("gameType").orElse("planets");

		dataProvider.getItems().clear();
		dataProvider.getItems().addAll(gamesService.getScoresByGameid(id));
		dataProvider.refreshAll();	
    }

}
