package de.witchcafe.planets.ui;

import java.util.ArrayList;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import de.witchcafe.planets.GamesService;
import de.witchcafe.planets.Player;
import de.witchcafe.planets.PlayerRepository;

@Route("players")
@PageTitle("Player List")
@AnonymousAllowed

@Menu(order = 0, icon = "vaadin:clipboard-check", title = "Player List")
class PlayerListView extends Main {

    private final GamesService gamesService;
    private final PlayerRepository repository;
	private ListDataProvider<Player> dataProvider;


    PlayerListView(GamesService gamesService,PlayerRepository repository) {
        this.gamesService = gamesService;
        this.repository = repository;

        Grid<Player> grid = new Grid<>(Player.class);
        grid.getColumnByKey("id").setVisible(false);
        dataProvider = new ListDataProvider<Player>(new ArrayList<Player>());
        grid.setDataProvider(dataProvider);
        add(grid);
        grid.setSizeFull();

        setSizeFull();

        // add(new Span("Player List"));
    }
    
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        dataProvider.getItems().addAll(repository.findAll());
    }
}
