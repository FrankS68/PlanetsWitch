package de.witchcafe.planets.ui;

import java.util.ArrayList;

import com.google.gson.internal.LinkedTreeMap;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.witchcafe.planets.Event;
import de.witchcafe.planets.Game;
import de.witchcafe.planets.GamesService;

@PageTitle("Game Events")
@AnonymousAllowed
// @Menu(order = 0, icon = "vaadin:clipboard-check", title = "Raw Score List")
@Route("events/:gameId?")
public class GameEventsView extends Main implements BeforeEnterObserver{

    private final GamesService gamesService;

    GameEventsView(GamesService gamesService) {
        this.gamesService = gamesService;

        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        // add(new ViewToolbar("Score List", ViewToolbar.group(getSearchField())));
    }
    
	@Override
	public void beforeEnter(BeforeEnterEvent event) {
        String id = event.getRouteParameters().get("gameId").orElse("no ID");
        String type = event.getRouteParameters().get("gameType").orElse("planets");

        ArrayList<Event> gameEvents = gamesService.getEventsByGameid(id);
        gameEvents.forEach(gameEvent -> {
            add(new Span(gameEvent.toString()));
        });
        setSizeFull();


    }

}
