# 🪐 PlanetsWitch

**PlanetsWitch** ist ein Analyse-Tool für das Online-Strategie-Spiel [Planets.nu](https://planets.nu) ...

## Betrieb mit Docker Compose (z. B. auf spiekeroog)

App und eigene Neo4j laufen per `docker-compose.yml`; das Image baut auch auf arm64 (Raspberry Pi).

```bash
git clone <repo> planetswitch && cd planetswitch
cp .env.example .env                       # NEO4J_PASSWORD, PUBLIC_HOST eintragen
cp planets-credentials.properties.example planets-credentials.properties   # planets.nu-Login
docker compose up -d --build
```

- App: `http://<PUBLIC_HOST>:8081`, Neo4j-Browser: `http://<PUBLIC_HOST>:7474` (Ports in `.env` änderbar).
- `data/` (Spielarchive, Analysen) liegt neben der Compose-Datei und wird in den Container gemountet.
- Der Graph (neovis) verbindet sich aus dem Browser direkt mit Neo4j, deshalb muss `PUBLIC_HOST` der Name sein, unter dem der Browser den Rechner erreicht.
- Bestehende Datenbank übernehmen: lokal `neo4j-admin database dump neo4j --to-path=.`, die Datei als `backup/neo4j.dump` ablegen, dann
  `docker compose stop app neo4j && docker compose run --rm neo4j neo4j-admin database load neo4j --from-path=/backups --overwrite-destination && docker compose up -d`.
- Aletheia/Python-Tools gegen diese Datenbank: `NEO4J_HTTP=http://<PUBLIC_HOST>:7474 python -I tools/topofficer_changes.py`.
