package de.witchcafe.planets.ui;

import java.util.ArrayList;

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

@Route("")
@PageTitle("Home")
@AnonymousAllowed

@Menu(order = 0, icon = "vaadin:clipboard-check", title = "Home")
class LandingPage extends Main {

    LandingPage() {

        add(new Span("Dashboard"));
    }

}
