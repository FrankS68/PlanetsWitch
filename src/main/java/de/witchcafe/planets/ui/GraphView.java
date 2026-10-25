package de.witchcafe.planets.ui;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

import com.google.gson.Gson;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

import de.witchcafe.base.ui.component.ViewToolbar;
import de.witchcafe.planets.ui.GraphFilter.Condition;
import de.witchcafe.planets.ui.GraphFilter.Operator;

@Route("graph")
@Menu(order = 0, icon = "vaadin:clipboard-check", title = "Graph")
@JavaScript("./dist/neovis.js")
public class GraphView extends VerticalLayout implements BeforeEnterObserver {

	private final GraphFilter filter = new GraphFilter(Condition.all());
	private GraphFilterDialog filterDialog;

	TextField searchField;
	NativeButton searchButton;
	TextField scriptField;
	NativeButton scriptButton;
	private final String neo4jUri;
	private final String neo4jUser;
	private final String neo4jPassword;

    public GraphView(
    		@Value("${spring.neo4j.uri}") String neo4jUri,
    		@Value("${spring.neo4j.authentication.username}") String neo4jUser,
    		@Value("${spring.neo4j.authentication.password:}") String neo4jPassword) {
        this.neo4jUri = neo4jUri;
        this.neo4jUser = neo4jUser;
        this.neo4jPassword = neo4jPassword;
        setSizeFull();
        setPadding(false);
        add(new ViewToolbar(
        		"Advanced neovis example", 
        		ViewToolbar.group(
        				new Button("Filter", new Icon(VaadinIcon.FILTER), e -> getFilterDialog().open()),
        				getSearchField(),
        				getSearchButton(),
        				getSearchMeButton(),
        				getSearchUnconnectedButton(),
        				getJSButton("console.log(window);"), 
        				getScriptField(), 
        				getScriptButton())));

        Div viz = new Div();
        viz.setId("viz");
        viz.getStyle().set("height", "90vh");
        viz.getStyle().set("width", "100%");
        viz.getStyle().set("border", "1px solid red");
        add(viz);
        add(new Html(createContextMenuDiv()));
    }
    
    public void filterGraph(String searchString) {
        getFilterDialog().setCustomCypher(searchString);
        renderCypher(searchString);
    }

    /** replaces the shown graph */
    private void renderCypher(String cypher) {
        pushFilterEntries();
        UI.getCurrent().getPage().executeJs("window.updateWithCypher($0);", cypher);
    }

    private void renderFilter() {
        getFilterDialog().useTree();
        renderCypher(filter.toCypher());
    }

    /** the context menu offers "remove from filter" for nodes matching one of these entries */
    private void pushFilterEntries() {
        List<Map<String, Object>> entries = filter.alternatives().stream()
        		.map(c -> Map.<String, Object>of("label", c.getLabel(), "property", c.getProperty(), "values", c.values()))
        		.toList();
        getElement().executeJs("window.filterEntries = JSON.parse($0);", new Gson().toJson(entries));
    }

    /** Called from the context menu: removes the node from the filter and reloads the graph. */
    @ClientCallable
    public void removeFromFilter(String label, String property, String value) {
        if (filter.removeValue(label, property, value)) {
            renderFilter();
        }
    }

    private GraphFilterDialog getFilterDialog() {
        if (filterDialog == null) {
            filterDialog = new GraphFilterDialog(filter, this::renderCypher);
        }
        return filterDialog;
    }

    /**
     * Called from the context menu after data was loaded: the loaded nodes become an OR branch
     * of the filter and are added to the graph without reloading it.
     */
    @ClientCallable
    public void addToFilter(String label, String property, String value) {
        Condition condition = filter.addAlternative(new Condition(label, property, Operator.EQUALS, value));
        getFilterDialog().refresh();
        pushFilterEntries();
        getElement().executeJs("window.visualisation.getVisualisation().updateWithCypher($0);", GraphFilter.toCypher(condition));
    }

	private TextField getSearchField() {
		// Das Suchfeld erstellen
		searchField = new TextField();
		searchField.setPlaceholder("Suche eingeben ...");
		searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
		searchField.setClearButtonVisible(true); // Erlaubt schnelles Löschen der Suche
		searchField.setValueChangeMode(ValueChangeMode.EAGER); // Filtert sofort beim Tippen

		// Die Filter-Logik
		searchField.addValueChangeListener(event -> {
		    Boolean enabled = searchField.getValue().trim().length() > 0;
		    searchButton.setEnabled(enabled);
		});
		return searchField;
	}

	private NativeButton getSearchButton() {
		searchButton = new NativeButton(
		        "Search");
		searchButton.addClickListener(e -> filterGraph(searchField.getValue().toLowerCase()));
		searchButton.setEnabled(false);
		return searchButton;
	}

	private TextField getScriptField() {
		// Das Suchfeld erstellen
		scriptField = new TextField();
		scriptField.setPlaceholder("script eingeben ...");
		scriptField.setPrefixComponent(new Icon(VaadinIcon.ROAD));
		scriptField.setClearButtonVisible(true); // Erlaubt schnelles Löschen der Suche
		scriptField.setValueChangeMode(ValueChangeMode.EAGER); // Filtert sofort beim Tippen

		// Die Filter-Logik
		scriptField.addValueChangeListener(event -> {
		    Boolean enabled = scriptField.getValue().trim().length() > 0;
		    scriptButton.setEnabled(enabled);
		});
		return scriptField;
	}

	private NativeButton getScriptButton() {
		scriptButton = new NativeButton(
		        "run script");
		scriptButton.addClickListener(e -> {
			// getElement().executeJs(scriptField.getValue().toLowerCase());
			UI.getCurrent().getPage().executeJs(scriptField.getValue().toLowerCase());
		});
		scriptButton.setEnabled(false);
		return scriptButton;
	}

	private NativeButton getJSButton(String expression) {
		NativeButton button = new NativeButton(
		        "javascript:"+expression);
		button.addClickListener(e -> getElement().executeJs(expression));
		return button;
	}
	
	private NativeButton getSearchMeButton() {
		NativeButton button = new NativeButton(
		        "SearchMe");
		button.addClickListener(e -> {
			filter.reset(new Condition("Player", "name", Operator.IN, "lomize, hawkeye418, gráinne, moulliez d"));
			renderFilter();
		});
		return button;
	} // 
	
	private NativeButton getSearchUnconnectedButton() {
		NativeButton button = new NativeButton(
		        "Unconnected");
		button.addClickListener(e -> filterGraph("MATCH (g:Game) WHERE NOT (g)<-[:playedIn]-() RETURN g"));
		return button;
	} // 
	/*

	 */

    /**
     * graph?player=<accountid> starts with that player and everything connected to it
     * (games, officers) instead of the whole graph.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        filter.reset(event.getLocation().getQueryParameters().getSingleParameter("player")
        		.filter(accountId -> accountId.matches("[0-9]+"))
        		.map(accountId -> new Condition("Player", "accountid", Operator.EQUALS, accountId))
        		.orElse(Condition.all()));
        if (filterDialog != null) {
        	filterDialog.refresh();
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        
        String cssScript = createCSSScript();        
        getElement().executeJs(cssScript);
        String script = withNeo4jConfig(createVizScriptEmbedded());
        getElement().executeJs(script);
        // the context menu reports loaded data back to this view
        getElement().executeJs("window.graphView = this;");
        pushFilterEntries();
        getElement().executeJs(createSetup());
        getElement().executeJs("drawembedded()");
        getElement().executeJs("setupContextMenu()");
    }
    
	private String createVizScriptEmbedded() {
		return """
class Visualisation {
	setVisualisation(viz){
		this.visualisation = viz;
	}
	getVisualisation(){
		return this.visualisation;
	}
	
	updateWithFunction(cypher){
		this.visualisation.updateWithFunction(cypher);
	}
	constructor() {
		console.log("viasualisation created");
	}
}

window.updateWithCypher2 = function(cypher) {
	window.visualisation.getVisualisation().clearNetwork().then()
	window.visualisation.getVisualisation().updateWithCypher(cypher);
	window.visualisation.getVisualisation().render();
}

window.updateWithCypher = function(cypher) {
    const viz = window.visualisation.getVisualisation();
    viz.clearNetwork();

    const checkAndLoad = () => {
        if (viz.nodes.length === 0) {
            viz.updateWithCypher(cypher);
        } else {
            // Falls noch Knoten da sind, warte noch 10ms
            setTimeout(checkAndLoad, 10);
        }
    };
    checkAndLoad();
}

window.drawembedded = function() {
	var config = {
		containerId: 'viz',
		neo4j: {
			serverUrl: __NEO4J_URI__,
			serverUser: __NEO4J_USER__,
			serverPassword: __NEO4J_PASSWORD__
		},
		visConfig: {
			nodes: {
				shape: 'square'
			},
			edges: {
				arrows: {
					to: { enabled: true }
				}
			},
		},
		initialCypher: __INITIAL_CYPHER__,
		labels: {
			Game: {
				label: 'name',
				group: 'community',
				[NeoVis.NEOVIS_ADVANCED_CONFIG]: {
					static: {
						type: 'Game',
						shape: 'dot',
						size: 25,
						font: { size: 14, color: '#000000', background: '#ffffff' }
					},
					function: {
						// finished games yellow, all others green
						color: (node) => ({ background: node.properties.statusname === 'Finished' ? '#ffd700' : '#7ce92b' })
					}
				}
			},
			Player: {
				label: 'name',
				group: 'community',
				[NeoVis.NEOVIS_ADVANCED_CONFIG]: {
					static: {
						type: 'Player',
						shape: 'square',
						size: 25,
						color: { background: '#ff002b' },
						font: { size: 14, color: '#000080', background: '#ffffff' }
					}
				}
			}
		},
		relationships: {
			INTERACTS: {
				value: 'weight',
				[NeoVis.NEOVIS_ADVANCED_CONFIG]: {
					function: {
						title: NeoVis.objectToTitleHtml
					},
				}
			}
		}
	};

	window.visualisation = new Visualisation();
	console.log('here the magic breaks');
	debugger;
	window.visualisation.setVisualisation(new NeoVis.default(config));
	window.visualisation.getVisualisation().render();
	console.log(window.visualisation.getVisualisation());

}
""";
	}

	private String createInitialCypher() {
		return """
		    initialCypher: "MATCH path=(:Player)-[:playedIn]->(:Game) RETURN path"
		    """;
	}

	private String createSetup() {
		return """
window.setupContextMenu = function() {
    const gameMenu = document.getElementById('game-context-menu');
    const playerMenu = document.getElementById('player-context-menu');
    const viz = window.visualisation.getVisualisation();
    
    // Wir registrieren den Rechtsklick direkt über das vis.js Netzwerk-Event
    viz.registerOnEvent("completed", (e) => {
        console.log("Netzwerk ist bereit, binde Kontextmenü...");
        
        if (viz.network) {
            viz.network.on("oncontext", function (params) {
                params.event.preventDefault();
                
		        const nodeId = viz.network.getNodeAt(params.pointer.DOM);
		
		        if (nodeId) {
		            console.log("Knoten gefunden!", nodeId);
		            selectedNodeData = viz.nodes.get(nodeId);
		            const inFilter = findFilterEntry(selectedNodeData) !== undefined;
		            document.querySelectorAll('.filter-remove').forEach(li => li.style.display = inFilter ? '' : 'none');
		        	if (selectedNodeData.type == 'Player'){
						console.log("Player Knoten gefunden!", nodeId);
						gameMenu.style.display = 'none';
			            playerMenu.style.display = 'block';
			            playerMenu.style.position = 'fixed';
			            // Wir nehmen die echten Bildschirm-Koordinaten vom Event
			            playerMenu.style.left = params.event.clientX + 'px';
			            playerMenu.style.top = params.event.clientY + 'px';
		        	}
		        	else if (selectedNodeData.type == 'Game'){
						console.log("Game Knoten gefunden!", nodeId);
						playerMenu.style.display = 'none';
			            gameMenu.style.display = 'block';
			            gameMenu.style.position = 'fixed';
			            // Wir nehmen die echten Bildschirm-Koordinaten vom Event
			            gameMenu.style.left = params.event.clientX + 'px';
			            gameMenu.style.top = params.event.clientY + 'px';
		        	}
		        	else{
						console.log("unbekannten Knoten gefunden!", nodeId);
						playerMenu.style.display = 'none';
						gameMenu.style.display = 'none';
		        	}
		
		        } else {
		            console.log("Rechtsklick auf Hintergrund (ID: undefined)");
					playerMenu.style.display = 'none';
		            gameMenu.style.display = 'none';
		        }
            });
        }
    });

    // Wichtig: Wir müssen trotzdem den Browser-Rechtsklick auf dem ganzen Div blockieren
    document.getElementById('viz').oncontextmenu = (e) => e.preventDefault();
}

// loads the data into neo4j, then the view adds the node as OR branch to the filter and to the graph
window.loadAndAdd = function(url, label, property, value) {
	fetch(url).then(res => res.text()).then(text => {
		console.log(text);
		window.graphView.$server.addToFilter(label, property, value);
	});
}

// the filter entry (label, property, values) that brings this node into the graph, if any
window.findFilterEntry = function(node) {
	const labels = node.raw.labels;
	const props = node.raw.properties;
	return (window.filterEntries || []).find(e =>
		labels.includes(e.label) && props[e.property] != null && e.values.includes(String(props[e.property])));
}

window.handleMenuAction = function(type) {
	var reset = false;
	if (!selectedNodeData) return;
	const p = selectedNodeData.raw.properties;

	if (type === 'removeFilter'){
		const entry = findFilterEntry(selectedNodeData);
		if (entry) window.graphView.$server.removeFromFilter(entry.label, entry.property, String(p[entry.property]));
		reset = true;
	}

	if (type === 'players'){
		loadAndAdd(`/api/gameinfos/${p.gameId}`, 'Game', 'gameId', p.gameId);
		reset = true;
	}
	if (type === 'scores') window.location.href = `http://localhost:8081/rawscores/${p.gameId}`;
	if (type === 'details') window.location.href = `http://localhost:8081/details/${p.gameId}`;
	if (type === 'active'){
		loadAndAdd(`/api/active/${p.name}`, 'Player', 'accountid', p.accountid);
		reset = true;
	}
	if (type === 'history'){
		loadAndAdd(`/api/history/${p.accountid}`, 'Player', 'accountid', p.accountid);
		reset = true;
	}
	// archive downloads can take minutes and don't change the graph, so only report the result
	if (type === 'archive'){
		fetch(`/api/archive/${p.gameId}`).then(res => res.text()).then(alert);
	}
	if (type === 'archiveFinished'){
		fetch(`/api/archive/finished/${p.name}`).then(res => res.text()).then(alert);
	}
	if (type === 'archive' || type === 'archiveFinished'){
		document.getElementById('player-context-menu').style.display = 'none';
		document.getElementById('game-context-menu').style.display = 'none';
	}

	if (reset){
		document.getElementById('player-context-menu').style.display = 'none';
		document.getElementById('game-context-menu').style.display = 'none';
	}
}

	""";
	}

	/*

	
	// 'viz' ist deine Neovis-Instanz
	viz.registerOnEvent("clickNode", (event) => {
	    const nodeId = event.node.id; // Die interne ID oder ein Property
	    
	    // Neues Query aufbauen: Zeige alles, was mit diesem Knoten verbunden ist
	    const newQuery = `MATCH (n)-[r]-(m) WHERE id(n) = ${nodeId} RETURN n, r, m`;
	    
	    // Den Graphen mit dem neuen Query aktualisieren
	    viz.renderWithCypher(newQuery);
	});

	function selectPlayer(playerName) {
	    const query = `MATCH (p:Player {name: '${playerName}'})-[r]->(m) RETURN p, r, m`;
	    viz.renderWithCypher(query);
	}
	
	Zusammenfassung für dein Vorhaben:
		Event: clickNode registrieren.

		Aktion: Das angeklickte Element identifizieren (z. B. node.properties.name).

		Update: viz.renderWithCypher("MATCH ...") ausführen.	 */
	private String createNeo4JSection() {
		return """
		neo4j: {
		    serverUrl: __NEO4J_URI__,
		    serverUser: __NEO4J_USER__,
		    serverPassword: __NEO4J_PASSWORD__,
            driverConfig: { encrypted: false }
		},""";
	}

	/**
	 * neovis connects from the browser, so the connection data from the spring config
	 * (spring.neo4j.*, password in planets-credentials.properties) is inserted as JS strings.
	 */
	private String withNeo4jConfig(String script) {
		Gson gson = new Gson();
		return script
				.replace("__NEO4J_URI__", gson.toJson(neo4jUri))
				.replace("__NEO4J_USER__", gson.toJson(neo4jUser))
				.replace("__NEO4J_PASSWORD__", gson.toJson(neo4jPassword))
				.replace("__INITIAL_CYPHER__", gson.toJson(filter.toCypher()));
	}

	private String createLabelSection() {
		return """
		labels: {"""+
				createGame2LabelSection()+","+
				createPlayerLabelSection()+","+
				createParticipationLabelSection()+"""
		},
		""";
	}
	private String createRelationshipSection() {
		return """
	relationships: {
        "playedIn": {
            "caption":  "race",
            [NeoVis.NEOVIS_ADVANCED_CONFIG]: {
                static: {
                    arrows: { to: { enabled: true } }, // Pfeilrichtung anzeigen
                    color: "#840084",
                    font: { size: 10 }
                }
            }
        },
        // Fallback für alle anderen Relationen
        "*": {
            "caption": false, // Zeigt keinen Text an
            [NeoVis.NEOVIS_ADVANCED_CONFIG]: {
                static: {
                    arrows: { to: { enabled: true } }
                }
            }
        }
    },
		""";
	}

	private String createContextMenuDiv() {
		return """
	<div>
    <div id="game-context-menu">
        <li onclick="handleMenuAction('players')">Spieler laden</li>
        <li onclick="handleMenuAction('scores')">🌐 Scores öffnen</li>
        <li onclick="handleMenuAction('details')">📋 Details anzeigen</li>
        <li onclick="handleMenuAction('archive')">Archiv laden</li>
        <li class="filter-remove" onclick="handleMenuAction('removeFilter')">Aus Filter entfernen</li>
    </div>
    <div id="player-context-menu">
        <li onclick="handleMenuAction('active')">Aktive Spiele laden</li>
        <li onclick="handleMenuAction('history')">History laden</li>
        <li onclick="handleMenuAction('archiveFinished')">Beendete Spiele archivieren</li>
        <li class="filter-remove" onclick="handleMenuAction('removeFilter')">Aus Filter entfernen</li>
    </div>		
	</div>
    """;
	}

	private String createPlayerLabelSection() {
		return """
		    "Player": {
		        "caption":"name",
		        "id": "elementId",
		        [NeoVis.NEOVIS_ADVANCED_CONFIG]: {
		            static: {
						color: "#ff8c00", // Orange
		                font: {
		                    size: 14,      // Kleinerer, lesbarer Wert
		                    color: '#340034',
		                    strokeWidth: 0, // Entfernt hässliche Umrandungen
		                    face: 'arial'
		                }
		            }
		        }
		    }
		    """;
	}

	private String createParticipationLabelSection() {
		return """
		    "Participation": {
		        [NeoVis.NEOVIS_ADVANCED_CONFIG]: {
		            static: {
						color: "#008cff", // Orange
		                font: {
		                    size: 14,      // Kleinerer, lesbarer Wert
		                    color: '#340034',
		                    strokeWidth: 0, // Entfernt hässliche Umrandungen
		                    face: 'arial'
		                }
		            }
		        }
		    }
		    """;
	}

	private String createCSSScript() {
		return "const style = document.createElement('style');" +
		        "style.textContent = `" +createMenuCSS()+
		        "`;" +
		        "document.head.appendChild(style);";
	}

	private String createMenuCSS() {
		return """
#game-context-menu {
    display: none;
    position: fixed;
	/* Ein absurd hoher Z-Index, um Vaadin-Overlays zu schlagen */
    z-index: 9999999 !important; 
    
    /* Das hier ist entscheidend: */
    pointer-events: auto !important;    
    
    background: #309bff;
    border: 1px solid #d0d0d0;
    box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    border-radius: 6px;
    min-width: 180px;
    overflow: hidden; /* Damit die abgerundeten Ecken auch beim Hover sichtbar bleiben */
}

#player-context-menu {
    display: none;
    position: fixed;
	/* Ein absurd hoher Z-Index, um Vaadin-Overlays zu schlagen */
    z-index: 9999999 !important; 
    
    /* Das hier ist entscheidend: */
    pointer-events: auto !important;    
    
    background: #ff9b30;
    border: 1px solid #d0d0d0;
    box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    border-radius: 6px;
    min-width: 180px;
    overflow: hidden; /* Damit die abgerundeten Ecken auch beim Hover sichtbar bleiben */
}

.menu-list {
    list-style: none;
    margin: 0;
    padding: 5px 0;
    font-family: sans-serif;
    font-size: 14px;
}

.menu-list li {
    padding: 10px 15px;
    cursor: pointer;
    color: #333;
    transition: background 0.1s;
}

#game-context-menu li:hover {
    background-color: #007bff !important;
    color: white !important;
}

#player-context-menu li:hover {
    background-color: #ff7b00 !important;
    color: white !important;
}

.menu-divider {
    height: 1px;
    background-color: #eee;
    margin: 5px 0 !important;
    cursor: default !important;
}		    """;
	}

	private String createGame2LabelSection() {
		return """
			"Game": {
				"caption":"name",
				"id": "elementId",
				[NeoVis.NEOVIS_ADVANCED_CONFIG]: {
					static: { shape: 'dot', size: 25, color: { background: '#2b7ce9' } }
				}
			}
		    """;
	}

	private String createGameLabelSection() {
		return """
		    "Game": {
		        label: "name",
				[NeoVis.NEOVIS_ADVANCED_CONFIG]: {
				            function: {
				                title: (node) => {
				                    const props = node.properties;
				                    return `
				                        <strong>${props.name}</strong><br>
				                        Status: ${props.statusname}<br>
				                        <a href="https://deine-seite.de/game/${props.gameId}" style="color: lightblue;">
				                           Zum Spiel Details
				                        </a>
				                    `;
				                }
				            }
				        },
		            [NeoVis.NEOVIS_ADVANCED_CONFIG]: {
		            static: {
						color: "#2b7ce9", // Ein schönes Blau
		                font: {
		                    size: 14,      // Kleinerer, lesbarer Wert
		                    color: '#343400',
		                    strokeWidth: 0, // Entfernt hässliche Umrandungen
		                    face: 'arial'
		                }
		            }
		        }
		    }
		    """;
	}
}
