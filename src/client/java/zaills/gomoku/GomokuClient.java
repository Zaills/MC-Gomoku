package zaills.gomoku;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zaills.gomoku.config.GomokuConfig;

public class GomokuClient implements ClientModInitializer {
	public static final String MOD_ID = "gomoku";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		AutoConfig.register(GomokuConfig.class, GsonConfigSerializer::new);

		Command<FabricClientCommandSource> run = commandContext -> {
			if (!APIHandler.isBoardFree()) {
				commandContext.getSource().sendFeedback(Component.literal("Game already started, wait for your turn"));
				return 0;
			}
			Minecraft instance = Minecraft.getInstance();
			instance.execute(() -> {
				try {
					Minecraft.getInstance().setScreen(new GobanScreen(Component.literal("Goban Screen")));
				} catch (Exception e) {
					LOGGER.error("Failed to set GobanScreen", e);
				}
			});
			return 1;
		};

		Command<FabricClientCommandSource> giveUp = context -> {
			boolean succeed = APIHandler.give_up();
			if (succeed)
				context.getSource().sendFeedback(Component.literal("You gave Up on the Gomoku Game"));
			else
				context.getSource().sendFeedback(Component.literal("Failed tp gave Up on the Gomoku Game"));
			return 0;
        };

		Command<FabricClientCommandSource> cWipe = context -> {
			context.getSource().sendFeedback(Component.literal("WIP"));
			return 1;
		};

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("go")
					.executes(run)
					.then(ClientCommandManager.literal("giveUp")
							.executes(giveUp))
					.then(ClientCommandManager.literal("create")
							.executes(cWipe))
					.then(ClientCommandManager.literal("join")
							.then(ClientCommandManager.argument("code", StringArgumentType.string())
									.executes(cWipe))
					));
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(context -> {
			APIHandler.give_up();
		});
	}
}