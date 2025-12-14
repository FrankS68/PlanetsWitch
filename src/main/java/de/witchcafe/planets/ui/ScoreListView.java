package de.witchcafe.planets.ui;

import de.witchcafe.base.ui.component.ViewToolbar;
import de.witchcafe.examplefeature.Task;
import de.witchcafe.examplefeature.TaskService;
import de.witchcafe.planets.Game;
import de.witchcafe.planets.GamesService;
import de.witchcafe.planets.Score;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

@Route("Scores/:gameId?")
@PageTitle("Score List")
@Menu(order = 0, icon = "vaadin:clipboard-check", title = "Score List")
class ScoreListView extends Main implements BeforeEnterObserver{

    private final GamesService gamesService;

    final TextField description;
    final DatePicker dueDate;
    final Button createBtn;
    final Grid<Score> scoreGrid;

    ScoreListView(GamesService gamesService) {
        this.gamesService = gamesService;

        description = new TextField();
        description.setPlaceholder("What do you want to do?");
        description.setAriaLabel("Task description");
        description.setMaxLength(Task.DESCRIPTION_MAX_LENGTH);
        description.setMinWidth("20em");
        
        

        dueDate = new DatePicker();
        dueDate.setPlaceholder("Due date");
        dueDate.setAriaLabel("Due date");

        createBtn = new Button("Create", event -> createTask());
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(getLocale())
                .withZone(ZoneId.systemDefault());
        var dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(getLocale());

        scoreGrid = new Grid<>();

		scoreGrid.setItems(query -> gamesService.getScoresByGameid(gameId).stream());
        scoreGrid.addColumn(Score::getName).setHeader("Name");
        scoreGrid.addColumn(Score::getTurn).setHeader("Turn");
        scoreGrid.addColumn(Score::getCapitalships).setHeader("Capitalships");
        scoreGrid.addColumn(Score::getFreighters).setHeader("Freighters");

        // gameGrid.addColumn(Game::getDescription).setHeader("Description");
        // gameGrid.addColumn(game -> Optional.ofNullable(game.getDatecreated()).map(dateFormatter::format).orElse("Never")).setHeader("Due Date");
        // gameGrid.addColumn(game -> dateTimeFormatter.format(game.getDatecreated())).setHeader("Creation Date");
        scoreGrid.setSizeFull();

        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar("Score List", ViewToolbar.group(description, dueDate, createBtn)));
        add(scoreGrid);
    }
    
    private void createTask() {
        gamesService.createTask(description.getValue(), dueDate.getValue());
        scoreGrid.getDataProvider().refreshAll();
        description.clear();
        dueDate.clear();
        Notification.show("Task added", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private String gameId;
	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		Optional<String> gameId = event.getRouteParameters().get("gameId");
		this.gameId = gameId.orElse("659148");
	}

}
