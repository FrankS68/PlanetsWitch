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

import de.witchcafe.planets.Game;
import de.witchcafe.planets.GamesService;

@PageTitle("Game Details")
@AnonymousAllowed
// @Menu(order = 0, icon = "vaadin:clipboard-check", title = "Raw Score List")
@Route("details/:gameId?")
public class GameDetailsView extends Main implements BeforeEnterObserver{

    private final GamesService gamesService;

    GameDetailsView(GamesService gamesService) {
        this.gamesService = gamesService;

        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        // add(new ViewToolbar("Score List", ViewToolbar.group(getSearchField())));
    }
    
	@Override
	public void beforeEnter(BeforeEnterEvent event) {
        String id = event.getRouteParameters().get("gameId").orElse("no ID");
        String type = event.getRouteParameters().get("gameType").orElse("planets");

        LinkedTreeMap<String, Object> gameInfoTree = gamesService.getGameInfo(id);
        if (gameInfoTree == null || gameInfoTree.get("game") == null) {
        	add(new H1(String.format("Kein Spiel mit ID %s gefunden", id)));
        	return;
        }
        LinkedTreeMap<String, Object> gameInfo = (LinkedTreeMap<String, Object>) gameInfoTree.get("game");
        System.out.println(gameInfo);
        ArrayList<String> specialKeys = new ArrayList<String>() {{
        	add("name");
        	add("shortdescription");
        	add("description");
        }};
        add(new H1(gameInfo.get("name").toString()));
        add(new H3(gameInfo.get("shortdescription").toString()));
        add(new Html("<span>"+gameInfo.get("description").toString()+"</span>"));
        gameInfo.keySet().forEach(key -> {
        	if (!specialKeys.contains(key)) {
        		add(new H3(key));
                add(new Span(gameInfo.get(key).toString()));
        	}
        });
        setSizeFull();


    }

}
