package de.witchcafe.planets.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import de.witchcafe.planets.ui.GraphFilter.Condition;
import de.witchcafe.planets.ui.GraphFilter.Group;
import de.witchcafe.planets.ui.GraphFilter.Junction;
import de.witchcafe.planets.ui.GraphFilter.Operator;
import de.witchcafe.planets.ui.GraphFilter.Target;

class GraphFilterTests {

	@Test
	void testAllNodes() {
		GraphFilter filter = new GraphFilter(Condition.all());
		assertEquals("MATCH (n)-[r]-(m)\nWHERE true\nWITH DISTINCT r\nRETURN startNode(r) AS n, r, endNode(r) AS m", filter.toCypher());
	}

	@Test
	void testAlternativesAndNestedGroup() {
		GraphFilter filter = new GraphFilter(new Condition("Game", "gameId", Operator.EQUALS, "664877"));
		Group group = filter.getRoot().add(new Group(Junction.AND));
		group.add(new Condition("Player", "name", Operator.IN, "lomize, o'neil"));
		Condition running = group.add(new Condition("Game", "statusname", Operator.EQUALS, "Running"));
		running.setTarget(Target.NEIGHBOUR);

		assertEquals("(n:Game AND n.gameId = '664877') OR "
				+ "((n:Player AND n.name IN ['lomize', 'o\\'neil']) AND (m:Game AND m.statusname = 'Running'))",
				filter.getRoot().toCypher());
	}

	@Test
	void testAddAlternativeWrapsAndRoot() {
		GraphFilter filter = new GraphFilter(Condition.all());
		Group oldRoot = filter.getRoot();
		oldRoot.setJunction(Junction.AND);
		filter.addAlternative(new Condition("Player", "accountid", Operator.EQUALS, "42"));

		assertEquals(Junction.OR, filter.getRoot().getJunction());
		assertSame(oldRoot, filter.getRoot().getChildren().get(0));
		assertEquals("(true) OR (n:Player AND n.accountid = '42')", filter.getRoot().toCypher());
	}

	@Test
	void testEmptyGroupsAndIdentifiers() {
		assertEquals("false", new Group(Junction.OR).toCypher());
		assertEquals("true", new Group(Junction.AND).toCypher());
		assertEquals("n:`Odd Label`", new Condition("Odd Label", null, Operator.EQUALS, null).toCypher());
	}
}
