package de.witchcafe.planets.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter of the graph view as AND/OR tree. Every condition is evaluated on the start node n
 * or on its neighbour m of the pattern (n)-[r]-(m), so a match always brings its connections along.
 */
public class GraphFilter {

	public enum Junction {
		AND("UND"), OR("ODER");

		final String caption;

		Junction(String caption) {
			this.caption = caption;
		}

		@Override
		public String toString() {
			return caption;
		}
	}

	public enum Target {
		START("n", "Knoten"), NEIGHBOUR("m", "Nachbar");

		final String variable;
		final String caption;

		Target(String variable, String caption) {
			this.variable = variable;
			this.caption = caption;
		}

		@Override
		public String toString() {
			return caption;
		}
	}

	public enum Operator {
		EQUALS("="), NOT_EQUALS("<>"), STARTS_WITH("STARTS WITH"), ENDS_WITH("ENDS WITH"), CONTAINS("CONTAINS"), IN("IN"), REGEX("=~");

		final String cypher;

		Operator(String cypher) {
			this.cypher = cypher;
		}

		@Override
		public String toString() {
			return cypher;
		}
	}

	public abstract static class Node {
		Group parent;

		public Group getParent() {
			return parent;
		}

		abstract String toCypher();

		public abstract String describe();
	}

	public static class Group extends Node {
		Junction junction;
		final List<Node> children = new ArrayList<>();

		public Group(Junction junction, Node... children) {
			this.junction = junction;
			Arrays.stream(children).forEach(this::add);
		}

		public <T extends Node> T add(T child) {
			child.parent = this;
			children.add(child);
			return child;
		}

		public void remove(Node child) {
			children.remove(child);
			child.parent = null;
		}

		public List<Node> getChildren() {
			return children;
		}

		public Junction getJunction() {
			return junction;
		}

		public void setJunction(Junction junction) {
			this.junction = junction;
		}

		@Override
		String toCypher() {
			if (children.isEmpty()) {
				// neutral element: an empty AND matches everything, an empty OR nothing
				return junction == Junction.AND ? "true" : "false";
			}
			return children.stream()
					.map(child -> child instanceof Group ? "(" + child.toCypher() + ")" : child.toCypher())
					.collect(Collectors.joining(" " + junction.name() + " "));
		}

		@Override
		public String describe() {
			return junction.caption;
		}
	}

	public static class Condition extends Node {
		Target target = Target.START;
		String label;
		String property;
		Operator operator = Operator.EQUALS;
		String value;

		public Condition() {
		}

		public Condition(String label, String property, Operator operator, String value) {
			this.label = label;
			this.property = property;
			this.operator = operator;
			this.value = value;
		}

		public static Condition all() {
			return new Condition();
		}

		/** true when the condition selects start nodes by a list of property values, e.g. one game or some players */
		boolean selectsValues() {
			return target == Target.START && !isBlank(label) && !isBlank(property)
					&& (operator == Operator.EQUALS || operator == Operator.IN);
		}

		public List<String> values() {
			if (operator != Operator.IN) {
				return List.of(value == null ? "" : value);
			}
			return Arrays.stream((value == null ? "" : value).split(","))
					.map(String::trim)
					.filter(v -> !v.isEmpty())
					.toList();
		}

		@Override
		String toCypher() {
			String variable = target.variable;
			List<String> parts = new ArrayList<>();
			if (!isBlank(label)) {
				parts.add(variable + ":" + identifier(label));
			}
			if (!isBlank(property)) {
				parts.add(variable + "." + identifier(property) + " " + operator.cypher + " " + literal());
			}
			if (parts.isEmpty()) {
				return "true";
			}
			return parts.size() == 1 ? parts.get(0) : "(" + String.join(" AND ", parts) + ")";
		}

		private String literal() {
			if (operator == Operator.IN) {
				return values().stream()
						.map(GraphFilter::quote)
						.collect(Collectors.joining(", ", "[", "]"));
			}
			return quote(value == null ? "" : value);
		}

		@Override
		public String describe() {
			if (isBlank(label) && isBlank(property)) {
				return target.caption + ": alle";
			}
			String text = target.caption + ": " + (isBlank(label) ? "*" : label);
			if (!isBlank(property)) {
				text += "." + property + " " + operator.cypher + " " + (value == null ? "" : value);
			}
			return text;
		}

		public Target getTarget() {
			return target;
		}

		public void setTarget(Target target) {
			this.target = target;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public String getProperty() {
			return property;
		}

		public void setProperty(String property) {
			this.property = property;
		}

		public Operator getOperator() {
			return operator;
		}

		public void setOperator(Operator operator) {
			this.operator = operator;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	private Group root;

	public GraphFilter(Node initial) {
		reset(initial);
	}

	public void reset(Node initial) {
		root = new Group(Junction.OR, initial);
	}

	public Group getRoot() {
		return root;
	}

	/**
	 * Adds a condition as OR branch, so the already shown graph stays part of the filter.
	 * A root combined with AND is wrapped first.
	 */
	public Condition addAlternative(Condition condition) {
		if (root.junction != Junction.OR) {
			Group old = root;
			root = new Group(Junction.OR);
			root.add(old);
		}
		return root.add(condition);
	}

	/**
	 * Conditions of OR groups that select nodes by value. Each of them adds its nodes to the graph,
	 * so a node matched by one of them can be removed from the filter again.
	 */
	public List<Condition> alternatives() {
		List<Condition> result = new ArrayList<>();
		collectAlternatives(root, result);
		return result;
	}

	private static void collectAlternatives(Group group, List<Condition> result) {
		for (Node child : group.children) {
			if (child instanceof Group childGroup) {
				collectAlternatives(childGroup, result);
			} else if (group.junction == Junction.OR && child instanceof Condition condition && condition.selectsValues()) {
				result.add(condition);
			}
		}
	}

	/**
	 * Removes the node with this property value from all alternatives: from an IN list only the value,
	 * otherwise the whole condition.
	 *
	 * @return whether the filter was changed
	 */
	public boolean removeValue(String label, String property, String value) {
		boolean changed = false;
		for (Condition condition : alternatives()) {
			List<String> values = condition.values();
			if (!label.equals(condition.label) || !property.equals(condition.property) || !values.contains(value)) {
				continue;
			}
			List<String> remaining = values.stream().filter(v -> !v.equals(value)).toList();
			if (remaining.isEmpty()) {
				condition.parent.remove(condition);
			} else {
				condition.value = String.join(", ", remaining);
			}
			changed = true;
		}
		return changed;
	}

	public String toCypher() {
		return toCypher(root);
	}

	public static String toCypher(Node node) {
		// the undirected pattern finds every relationship twice, the graph needs it once
		return "MATCH (n)-[r]-(m)\nWHERE " + node.toCypher() + "\nWITH DISTINCT r\nRETURN startNode(r) AS n, r, endNode(r) AS m";
	}

	static String quote(String value) {
		return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
	}

	static String identifier(String name) {
		return name.matches("[A-Za-z_][A-Za-z0-9_]*") ? name : "`" + name.replace("`", "``") + "`";
	}

	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}
}
