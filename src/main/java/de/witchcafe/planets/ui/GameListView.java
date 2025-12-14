package de.witchcafe.planets.ui;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.witchcafe.base.ui.component.ViewToolbar;
import de.witchcafe.examplefeature.Task;
import de.witchcafe.planets.Game;
import de.witchcafe.planets.GamesService;

@Route("Games")
@PageTitle("Game List")
@Menu(order = 0, icon = "vaadin:clipboard-check", title = "Game List")
class GameListView extends Main {

    private final GamesService gamesService;

    final TextField description;
    final DatePicker dueDate;
    final Button createBtn;
    final Grid<Game> gameGrid;

    GameListView(GamesService gamesService) {
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

        gameGrid = new Grid<>();
        gameGrid.setItems(query -> gamesService.list(toSpringPageRequest(query)).stream());
        gameGrid.addColumn(Game::getName).setHeader("Name");
        gameGrid.addColumn(Game::getStatusname).setHeader("Status");
        gameGrid.addColumn(game -> getScoresButton(game.getId())).setHeader("Scores");
        // gameGrid.addColumn(Game::getDescription).setHeader("Description");
        // gameGrid.addColumn(game -> Optional.ofNullable(game.getDatecreated()).map(dateFormatter::format).orElse("Never")).setHeader("Due Date");
        // gameGrid.addColumn(game -> dateTimeFormatter.format(game.getDatecreated())).setHeader("Creation Date");
        gameGrid.setSizeFull();

        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar("Game List", ViewToolbar.group(description, dueDate, createBtn)));
        add(gameGrid);
    }

	private NativeButton getScoresButton(String gameId) {
		NativeButton button = new NativeButton(
		        "Navigate to scores:"+ gameId);
		button.addClickListener(e ->
		     button.getUI().ifPresent(ui ->
		           ui.navigate("Scores/"+gameId)));
		return button;
	}

    private void createTask() {
        gamesService.createTask(description.getValue(), dueDate.getValue());
        gameGrid.getDataProvider().refreshAll();
        description.clear();
        dueDate.clear();
        Notification.show("Task added", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

}
