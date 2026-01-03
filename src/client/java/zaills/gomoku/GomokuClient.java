package zaills.gomoku;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class GomokuClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		Command<net.minecraft.commands.CommandSourceStack> openScreen = context -> {
			net.minecraft.server.MinecraftServer server = context.getSource().getServer();
			server.execute(() -> {
				Minecraft instance = Minecraft.getInstance();
				instance.execute(() -> {
					try {
						Minecraft.getInstance().setScreen(new GobanScreen(Component.literal("Goban Screen")));
					} catch (Exception e) {
						Gomoku.LOGGER.error("Failed to set GobanScreen", e);
					}
				});
			});
			return 1;
		};

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				net.minecraft.commands.Commands.literal("openscreen")
						.executes(openScreen)
		));
	}
}