package de.witchcafe.planets.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.value.ValueChangeMode;

import de.witchcafe.planets.ui.GraphFilter.Condition;
import de.witchcafe.planets.ui.GraphFilter.Group;
import de.witchcafe.planets.ui.GraphFilter.Junction;
import de.witchcafe.planets.ui.GraphFilter.Node;
import de.witchcafe.planets.ui.GraphFilter.Operator;
import de.witchcafe.planets.ui.GraphFilter.Target;

/**
 * Shows the graph filter as AND/OR tree and the generated cypher. The cypher can be edited by hand,
 * it then no longer follows the tree until it is generated again.
 */
public class GraphFilterDialog extends Dialog {

	private static final Map<String, List<String>> PROPERTIES = Map.of(
			"Game", List.of("gameId", "name", "statusname", "status", "shortdescription", "turn", "datecreated", "lasthostdate"),
			"Player", List.of("name", "accountid"),
			"Officer", List.of("username", "accountid"),
			"OfficerSnapshot", List.of("username", "accountid"));

	private final GraphFilter filter;
	private final Consumer<String> applyCypher;
	private final TreeGrid<Node> tree = new TreeGrid<>();
	private final VerticalLayout editor = new VerticalLayout();
	private final TextArea cypherArea = new TextArea("Cypher");
	private final Span customHint = new Span("Von Hand geändert – folgt nicht mehr dem Baum");
	private final Button deleteButton = new Button("Löschen");
	private boolean custom;

	public GraphFilterDialog(GraphFilter filter, Consumer<String> applyCypher) {
		this.filter = filter;
		this.applyCypher = applyCypher;
		setHeaderTitle("Filter");
		setWidth("1100px");
		setDraggable(true);
		setResizable(true);

		tree.addHierarchyColumn(Node::describe).setHeader("Bedingungen");
		tree.setHeight("320px");
		tree.asSingleSelect().addValueChangeListener(e -> showEditor(e.getValue()));

		Button addCondition = new Button("+ Bedingung", e -> addToSelectedGroup(new Condition("Player", "name", Operator.EQUALS, "")));
		Button addGroup = new Button("+ Gruppe", e -> addToSelectedGroup(new Group(Junction.AND)));
		deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
		deleteButton.addClickListener(e -> deleteSelected());
		deleteButton.setEnabled(false);
		VerticalLayout treeColumn = new VerticalLayout(tree, new HorizontalLayout(addCondition, addGroup, deleteButton));
		treeColumn.setPadding(false);
		treeColumn.setWidth("55%");

		editor.setPadding(false);
		editor.setWidth("45%");
		HorizontalLayout top = new HorizontalLayout(treeColumn, editor);
		top.setWidthFull();

		cypherArea.setWidthFull();
		cypherArea.setMinHeight("140px");
		cypherArea.getStyle().set("font-family", "monospace");
		cypherArea.setValueChangeMode(ValueChangeMode.EAGER);
		cypherArea.addValueChangeListener(e -> {
			if (e.isFromClient()) {
				custom = !e.getValue().equals(filter.toCypher());
				customHint.setVisible(custom);
			}
		});
		customHint.getStyle().set("color", "var(--lumo-error-text-color)");
		customHint.setVisible(false);

		VerticalLayout content = new VerticalLayout(top, cypherArea, customHint);
		content.setPadding(false);
		add(content);

		Button regenerate = new Button("Aus Baum erzeugen", e -> useTree());
		Button apply = new Button("Anwenden", e -> applyCypher.accept(cypherArea.getValue()));
		apply.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		getFooter().add(regenerate, new Button("Schließen", e -> close()), apply);

		refresh();
	}

	/** Shows the current state of the filter, e.g. after a branch was added from the context menu. */
	public void refresh() {
		Node selected = tree.asSingleSelect().getValue();
		tree.setItems(List.of(filter.getRoot()), node -> node instanceof Group group ? group.getChildren() : List.of());
		tree.expandRecursively(List.of(filter.getRoot()), Integer.MAX_VALUE);
		if (selected != null && isInTree(selected)) {
			tree.select(selected);
		}
		updateCypher();
	}

	/** Makes the tree the source of the query again, e.g. after the filter was changed from the graph. */
	public void useTree() {
		custom = false;
		refresh();
	}

	/** Shows a query that was not built from the tree. */
	public void setCustomCypher(String cypher) {
		custom = true;
		cypherArea.setValue(cypher);
		customHint.setVisible(true);
	}

	private void updateCypher() {
		if (!custom) {
			cypherArea.setValue(filter.toCypher());
		}
		customHint.setVisible(custom);
	}

	private void changed(Node node) {
		// a tree change makes the tree the source of the query again
		custom = false;
		tree.getDataProvider().refreshItem(node);
		updateCypher();
	}

	private void addToSelectedGroup(Node child) {
		Node selected = tree.asSingleSelect().getValue();
		Group group = selected instanceof Group g ? g : selected != null ? selected.getParent() : filter.getRoot();
		group.add(child);
		custom = false;
		refresh();
		tree.select(child);
	}

	private void deleteSelected() {
		Node selected = tree.asSingleSelect().getValue();
		if (selected == null || selected.getParent() == null) {
			return;
		}
		selected.getParent().remove(selected);
		tree.deselectAll();
		custom = false;
		refresh();
	}

	private boolean isInTree(Node node) {
		while (node.getParent() != null) {
			node = node.getParent();
		}
		return node == filter.getRoot();
	}

	private void showEditor(Node node) {
		editor.removeAll();
		deleteButton.setEnabled(node != null && node.getParent() != null);
		if (node instanceof Group group) {
			Select<Junction> junction = new Select<>();
			junction.setLabel("Verknüpfung");
			junction.setItems(Junction.values());
			junction.setValue(group.getJunction());
			junction.addValueChangeListener(e -> {
				group.setJunction(e.getValue());
				changed(group);
			});
			editor.add(junction);
		} else if (node instanceof Condition condition) {
			Select<Target> target = new Select<>();
			target.setLabel("Gilt für");
			target.setItems(Target.values());
			target.setValue(condition.getTarget());
			target.addValueChangeListener(e -> {
				condition.setTarget(e.getValue());
				changed(condition);
			});

			ComboBox<String> property = comboBox("Eigenschaft (leer = nur Label)");
			setItemsWith(property, propertiesOf(condition.getLabel()), condition.getProperty());
			property.addValueChangeListener(e -> {
				condition.setProperty(e.getValue());
				changed(condition);
			});

			ComboBox<String> label = comboBox("Label (leer = beliebig)");
			setItemsWith(label, List.copyOf(PROPERTIES.keySet()), condition.getLabel());
			label.addValueChangeListener(e -> {
				condition.setLabel(e.getValue());
				setItemsWith(property, propertiesOf(e.getValue()), condition.getProperty());
				changed(condition);
			});

			Select<Operator> operator = new Select<>();
			operator.setLabel("Operator");
			operator.setItems(Operator.values());
			operator.setValue(condition.getOperator());
			operator.addValueChangeListener(e -> {
				condition.setOperator(e.getValue());
				changed(condition);
			});

			TextField value = new TextField("Wert (bei IN mit Komma getrennt)");
			value.setValue(condition.getValue() == null ? "" : condition.getValue());
			value.setValueChangeMode(ValueChangeMode.EAGER);
			value.addValueChangeListener(e -> {
				condition.setValue(e.getValue());
				changed(condition);
			});

			for (HasSize field : List.<HasSize>of(target, label, property, operator, value)) {
				field.setWidthFull();
			}
			editor.add(target, label, property, operator, value);
		}
		editor.setAlignItems(FlexComponent.Alignment.STRETCH);
	}

	private static List<String> propertiesOf(String label) {
		return label == null ? List.of() : PROPERTIES.getOrDefault(label, List.of());
	}

	private static ComboBox<String> comboBox(String caption) {
		ComboBox<String> box = new ComboBox<>(caption);
		box.setAllowCustomValue(true);
		box.setClearButtonVisible(true);
		box.addCustomValueSetListener(e -> {
			List<String> items = new ArrayList<>(box.getListDataView().getItems().toList());
			items.add(e.getDetail());
			box.setItems(items);
			box.setValue(e.getDetail());
		});
		return box;
	}

	/** custom values are only selectable when they are part of the items */
	private static void setItemsWith(ComboBox<String> box, List<String> items, String value) {
		List<String> all = new ArrayList<>(items);
		if (value != null && !value.isBlank() && !all.contains(value)) {
			all.add(value);
		}
		box.setItems(all);
		box.setValue(value == null || value.isBlank() ? null : value);
	}
}
