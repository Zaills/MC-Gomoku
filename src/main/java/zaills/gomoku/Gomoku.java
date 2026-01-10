package zaills.gomoku;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

public class Gomoku implements ModInitializer {
	public static final String MOD_ID = "gomoku";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");
		testAPI();

		Command<CommandSourceStack> pingCommand = context -> {
			testAPI();
			return 1;
		};

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				net.minecraft.commands.Commands.literal("ping")
						.executes(pingCommand)
		));
	}

	public static void testAPI() {
		// call the API from 127.0.0.1:8080/ping
		try {
			java.net.URL url = URI.create("http://172.22.248.30:8080/board").toURL();
			java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(3000);
			conn.setReadTimeout(3000);
			int responseCode = conn.getResponseCode();
			if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
				LOGGER.info("API is reachable");
				LOGGER.info("Response Code: " + responseCode);
				LOGGER.info("Response Message: " + conn.getResponseMessage());
				LOGGER.info("Response Content: ");
				java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
				String inputLine;
				StringBuilder content = new StringBuilder();
				while ((inputLine = in.readLine()) != null) {
					content.append(inputLine);
				}
				in.close();
				LOGGER.info(content.toString());
			} else {
				LOGGER.error("API is not reachable, response code: " + responseCode);
			}
		} catch (IOException e) {
			LOGGER.error("Error while trying to reach API: " + e.getMessage());
		}
	}
}