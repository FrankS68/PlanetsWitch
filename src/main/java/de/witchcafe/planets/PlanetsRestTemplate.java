package de.witchcafe.planets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;

public class PlanetsRestTemplate extends RestTemplate {
	public byte[] getBytesFromUrl(String requestUrl) {
		byte[] content = new byte[0];

        HttpHeaders httpHeaders = createHttpHeaders();

		ResponseEntity<Resource> responseEntity = exchange(secureUrl(requestUrl), HttpMethod.GET,
                new HttpEntity<>(httpHeaders), Resource.class);

        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            System.err.println("error: " + responseEntity.getStatusCode().toString());
            throw new RuntimeException("error: " + responseEntity.getStatusCode().toString());
        }

        // Encoding type of the response body
        String contentEncoding = responseEntity.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);

        if ("gzip".equalsIgnoreCase(contentEncoding)) {

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            try (GZIPInputStream gzipInputStream = new GZIPInputStream(responseEntity.getBody().getInputStream())) {
                gzipInputStream.transferTo(byteArrayOutputStream);
            } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

            content = byteArrayOutputStream.toByteArray();
   
        } else {
        	try {
        		System.out.println(requestUrl +"\n"+
				responseEntity.getBody().getContentAsString(Charset.defaultCharset()));
			} catch (IOException e) {
				e.printStackTrace();
			}
            // TODO
        }
		return content;
	}

	/**
	 * Streams a binary response (e.g. the ZIP from /game/loadall) into the given file.
	 * The API answers errors with a small JSON body instead of the file, so that case is
	 * detected and turned into an exception.
	 */
	public Path downloadToFile(String requestUrl, Path target) throws IOException {
		Files.createDirectories(target.toAbsolutePath().getParent());
		Path tmp = target.resolveSibling(target.getFileName() + ".part");
		execute(secureUrl(requestUrl), HttpMethod.GET, null, response -> {
			Files.copy(response.getBody(), tmp, StandardCopyOption.REPLACE_EXISTING);
			return null;
		});
		byte[] head = new byte[2];
		try (InputStream in = Files.newInputStream(tmp)) {
			in.readNBytes(head, 0, 2);
		}
		if (head[0] != 'P' || head[1] != 'K') {
			String body = Files.readString(tmp);
			Files.deleteIfExists(tmp);
			throw new IOException(String.format("no zip returned by %s: %s", requestUrl, body));
		}
		return Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
	}

	protected String secureUrl(String requestUrl) {
		return requestUrl;
	}

	protected HttpHeaders createHttpHeaders() {
		HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.set(HttpHeaders.ACCEPT_ENCODING, "gzip");
		return httpHeaders;
	}

	public String scoresByGameid(String gameid) {
		return String.format("http://api.planets.nu/game/loadscores?gameid=%s",gameid);
	}

	public String eventsByGameid(String gameid) {
		return String.format("http://api.planets.nu/game/loadevents?gameid=%s",gameid);
	}

	public String loadCompleteGame(String gameid) {
		return String.format("http://api.planets.nu/game/loadall?gameid=%s",gameid);
	}

	public String loadGameInfo(String gameid) {
		return String.format("http://api.planets.nu/game/loadinfo?gameid=%s",gameid);
	}

	public String loadGame(String gameid) {
		return String.format("http://api.planets.nu/game/details?id=%s",gameid);
	}

	public String activeGamesByUsername(String username) {
		return String.format("http://api.planets.nu/games/list?username=%s",username);
	}

	public String accountHistoryByAccountId(String accountid) {
		return String.format("http://api.planets.nu/account/history?accountid=%s",accountid);
	}

	public String officerByUsername(String username) {
		return String.format("http://api.planets.nu/account/loadofficer?officerid=%s",username);
	}

	public String loadAccount(String username) {
		return String.format("http://api.planets.nu/account/load?username=%s",username);
	}

	public String profileByUsername(String username) {
		return String.format("http://api.planets.nu/account/loadprofile?username=%s",username);
	}
	
	public String leaderboard() {
		return "http://api.planets.nu/account/leaderboard";
	}

	public String finishedGamesByUsername(String username) {
		return String.format("http://api.planets.nu/games/list?status=3,4&username=%s",username);
	}
}
