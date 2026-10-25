package de.witchcafe.planets;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Properties;

import com.google.gson.Gson;

public class AuthenticatedPlanetsRestTemplate extends PlanetsRestTemplate {

	public static final String CREDENTIALS_FILE = "planets-credentials.properties";

	private final String username;
	private final String password;
	private String apiKey;
	private boolean isAuthenticated;

	/**
	 * @param username planets.nu login, configured as planets.username
	 * @param password planets.nu password, configured as planets.password
	 */
	public AuthenticatedPlanetsRestTemplate(String username, String password) {
		this.username = username;
		this.password = password;
	}

	/**
	 * Reads planets.username and planets.password from {@value #CREDENTIALS_FILE} in the
	 * working directory, for use outside of Spring (e.g. plain unit tests).
	 */
	public static AuthenticatedPlanetsRestTemplate fromCredentialsFile() throws IOException {
		Properties credentials = new Properties();
		try (Reader reader = Files.newBufferedReader(Path.of(CREDENTIALS_FILE))) {
			credentials.load(reader);
		}
		return new AuthenticatedPlanetsRestTemplate(
				credentials.getProperty("planets.username"),
				credentials.getProperty("planets.password"));
	}

	public String login() {
		if (username == null || username.isBlank() || password == null || password.isBlank()) {
			throw new IllegalStateException(String.format(
					"planets.username / planets.password not configured, see %s.example", CREDENTIALS_FILE));
		}
		// uri variables get url-encoded, so passwords containing e.g. '@' or '&' work
		String loginResult = getForObject("http://api.planets.nu/login?username={username}&password={password}",
				String.class, username, password);
		HashMap loginValues = new Gson().fromJson(loginResult,HashMap.class);
		isAuthenticated = "true".equals(loginValues.get("success").toString());
		if (isAuthenticated) {
			apiKey = loginValues.get("apikey").toString();
		}
		return loginValues.get("success").toString();
	}

	@Override
	protected String secureUrl(String requestUrl) {
		return super.secureUrl(requestUrl)+String.format("&apikey=%s", apiKey);
	}

}
