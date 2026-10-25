package de.witchcafe.planets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class PlanetsRestTests {
	PlanetsRestTemplate pTemplate = new PlanetsRestTemplate();

	@Test
	void test() {
		ResponseEntity<String> gamesResult = pTemplate.getForEntity("http://api.planets.nu/games/list?username=Lomize", String.class);
		assertNotNull(gamesResult);
		System.out.println(gamesResult);
	}

	@Test
	void testLoadGameInfo() {
		String gamesDetailsResult = new String(pTemplate.getBytesFromUrl(pTemplate.loadGameInfo("642972")));
		assertNotNull(gamesDetailsResult);
		System.out.println(gamesDetailsResult);
		// String gamesResult = new String(pTemplate.getBytesFromUrl(apTemplate.loadGame("642972")));
		// assertNotNull(gamesResult);
		// System.out.println(gamesResult);
	}

	@Test
	void testInspectAccount() throws IOException {
		AuthenticatedPlanetsRestTemplate apTemplate = AuthenticatedPlanetsRestTemplate.fromCredentialsFile();
		assertEquals("true", apTemplate.login());
		String accountResult = new String(apTemplate.getBytesFromUrl(pTemplate.loadAccount("Lomize")));
		assertNotNull(accountResult);
		System.out.println(accountResult);
		String profileResult = new String(pTemplate.getBytesFromUrl(pTemplate.profileByUsername("Lomize")));
		assertNotNull(profileResult);
		System.out.println(profileResult);
	}
}
