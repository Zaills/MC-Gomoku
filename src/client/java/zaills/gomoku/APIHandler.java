package zaills.gomoku;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static zaills.gomoku.Gomoku.LOGGER;

public class APIHandler {
	private static final String baseUrl = "http://127.0.0.1:8080";
	private static final Gson GSON = new Gson();
	private static int[][] boardState;

	public static int[][] getBoardState() {
		try {
			java.net.URL url = URI.create(baseUrl + "/board").toURL();
			java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(3000);
			conn.setReadTimeout(3000);
			int responseCode = conn.getResponseCode();
			if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
				java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
				String inputLine;
				StringBuilder content = new StringBuilder();
				while ((inputLine = in.readLine()) != null) {
					content.append(inputLine);
				}
				in.close();
				JsonObject boardJson = JsonParser.parseString(content.toString()).getAsJsonObject();
				boardState = GSON.fromJson(boardJson.get("board"), int[][].class);
			}
		} catch (IOException e) {
			LOGGER.error("Error while trying to reach API: " + e.getMessage());
		}
		if (boardState != null) {
			return boardState;
		}
		return null;
	}

	public static void sendMove(int x, int y, int player) {
		try {
			java.net.URL url = URI.create(baseUrl + "/move").toURL();
			java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json; utf-8");
			conn.setDoOutput(true);
			conn.setConnectTimeout(3000);
			conn.setReadTimeout(3000);

			JsonObject moveJson = new JsonObject();
			moveJson.addProperty("x", x);
			moveJson.addProperty("y", y);
			moveJson.addProperty("player", player);

			try (java.io.OutputStream os = conn.getOutputStream()) {
				byte[] input = moveJson.toString().getBytes(StandardCharsets.UTF_8);
				os.write(input, 0, input.length);
			}

			int responseCode = conn.getResponseCode();
			if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
				LOGGER.info("Move sent successfully");
			} else {
				LOGGER.error("Failed to send move, response code: " + responseCode);
			}
		} catch (IOException e) {
			LOGGER.error("Error while trying to send move to API: " + e.getMessage());
		}
	}
}
