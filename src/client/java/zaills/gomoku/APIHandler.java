package zaills.gomoku;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.util.Tuple;
import zaills.gomoku.config.GomokuConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static zaills.gomoku.GomokuClient.LOGGER;

public class APIHandler {
	private static final String baseUrl = AutoConfig.getConfigHolder(GomokuConfig.class).getConfig().baseURL;
	private static final Gson GSON = new Gson();
	private static int[][] boardState;
	private static String token = "";
	private static String token2 = "";
	private static boolean isBoardFree = true;

	private static HttpURLConnection GetConnection(String path) {
		try	{
		URL url = URI.create(baseUrl + path).toURL();
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setConnectTimeout(3000);
		conn.setReadTimeout(3000);
		return conn;
		} catch (IOException e) {
			return null;
		}
	}

	private static HttpURLConnection PostConnection(String path, JsonObject payload) {
		try	{
		URL url = URI.create(baseUrl + path).toURL();
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json; utf-8");
		conn.setDoOutput(true);
		conn.setConnectTimeout(3000);
		conn.setReadTimeout(3000);
		try {
			byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
			conn.getOutputStream().write(input, 0, input.length);
		} catch (IOException e) {
			return null;
		}
		return conn;
		} catch (IOException e) {
			return null;
		}
	}

	private static String readResponse(HttpURLConnection conn) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String inputLine;
		StringBuilder content = new StringBuilder();
		while ((inputLine = in.readLine()) != null) {
			content.append(inputLine);
		}
		in.close();
		return content.toString();
	}

	public static Tuple<String, String> create_room(boolean ai_mode, boolean local_mode) {
		try {
			JsonObject roomJson = new JsonObject();
			roomJson.addProperty("ai_mode", ai_mode);
			roomJson.addProperty("local_mode", local_mode);

			HttpURLConnection conn = PostConnection("/create", roomJson);
			if (conn == null) {
				return null;
			}
			int responseCode = conn.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				LOGGER.info("Room created successfully");
				String response = readResponse(conn);
				JsonObject responseJson = JsonParser.parseString(response).getAsJsonObject();
				token = responseJson.get("player_one").getAsString();
				token2 = responseJson.get("player_two").getAsString();
                LOGGER.info("Room token: {}", token);
                LOGGER.info("token 2: {}", token2);
				return new Tuple<>(token, token2);
			} else {
                LOGGER.error("Failed to create room, response code: {}", responseCode);
			}
		} catch (IOException e) {
            LOGGER.error("Error while trying to create room via API: {}", e.getMessage());
		}
		return null;
	}

	public static int[][] join_room(String room_id) {
		try {
			token = "";
			token2 = "";
			JsonObject roomJson = new JsonObject();
			roomJson.addProperty("token", room_id);

			HttpURLConnection conn = PostConnection("/join", roomJson);
			if (conn == null) {
				return null;
			}
			int responseCode = conn.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				String response = readResponse(conn);
				System.out.println(response);
				JsonObject responseJson = JsonParser.parseString(response).getAsJsonObject();
				token = responseJson.get("token").getAsString();

				LOGGER.info("Joined room successfully");
				return getBoardState();
			} else {
				LOGGER.error("Failed to join room, response code: " + responseCode);
				return null;
			}
		} catch (IOException e) {
			LOGGER.error("Error while trying to join room via API: " + e.getMessage());
			return null;
		}
	}

	public static int[][] getBoardState() {
		try {
			HttpURLConnection conn = GetConnection("/board");
			if (conn == null) {
				return null;
			}
			int responseCode = conn.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				String response = readResponse(conn);
				JsonObject boardJson = JsonParser.parseString(response).getAsJsonObject();
				isBoardFree = GSON.fromJson(boardJson.get("goban_free"), boolean.class);
				boardState = GSON.fromJson(boardJson.get("board"), int[][].class);
			}
		} catch (IOException e) {
			LOGGER.error("Error while trying to reach API: " + e.getMessage());
			return null;
		}
		if (boardState != null) {
			return boardState;
		}
		return null;
	}

	public static boolean isBoardFree(){
		try {
			HttpURLConnection conn = GetConnection("/board");
			if (conn == null) {
				return false;
			}
			int responseCode = conn.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				String response = readResponse(conn);
				JsonObject boardJson = JsonParser.parseString(response).getAsJsonObject();
				isBoardFree = GSON.fromJson(boardJson.get("goban_free"), boolean.class);
			}
		} catch (IOException e) {
			LOGGER.error("Error while trying to reach API: " + e.getMessage());
		}
		return isBoardFree;
	}

	public static boolean sendMove(int x, int y, int player) {
		try {
			JsonObject moveJson = new JsonObject();
			moveJson.addProperty("x", x);
			moveJson.addProperty("y", y);
			moveJson.addProperty("color", player);
			moveJson.addProperty("token", token);

			HttpURLConnection conn = PostConnection("/move", moveJson);
			if (conn == null) {
				return false;
			}
			int responseCode = conn.getResponseCode();
			if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
				LOGGER.info("Move sent successfully");
				return true;
			} else {
				LOGGER.error("Failed to send move, response code: " + responseCode);
				return false;
			}
		} catch (IOException e) {
			LOGGER.error("Error while trying to send move to API: " + e.getMessage());
			return false;
		}
	}

	public static int[] getAISuggest() {
		try {
			JsonObject suggestJson = new JsonObject();
			suggestJson.addProperty("token", token);

			HttpURLConnection conn = PostConnection("/suggest", suggestJson);
			if (conn == null) {
				return new int[]{-1, -1};
			}
			int responseCode = conn.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				String response = readResponse(conn);
				JsonObject suggestionJson = JsonParser.parseString(response).getAsJsonObject();
				int x = suggestionJson.get("x").getAsInt();
				int y = suggestionJson.get("y").getAsInt();
				return new int[]{x, y};
			}
		} catch (IOException e) {
			LOGGER.error("Error while trying to reach API: " + e.getMessage());
		}
		return new int[]{-1, -1};
	}

	public static boolean give_up() {
		try {
			JsonObject giveUpJson = new JsonObject();
			giveUpJson.addProperty("token", token);

			HttpURLConnection conn = PostConnection("/giveUp", giveUpJson);
			if (conn == null) {
				return false;
			}
			int responseCode = conn.getResponseCode();
			if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
				LOGGER.info("Give up sent successfully");
				return true;
			} else {
				LOGGER.error("Failed to send give up, response code: " + responseCode);
				return false;
			}
		} catch (IOException e) {
			LOGGER.error("Error while trying to send give up to API: " + e.getMessage());
			return false;
		}

	}
}
