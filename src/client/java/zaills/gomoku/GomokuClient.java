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

		Command<FabricClientCommandSource> run = context -> {
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
				context.getSource().sendFeedback(Component.literal("Failed to gave Up on the Gomoku Game"));
			return 0;
        };

		Command<FabricClientCommandSource> join = context -> {
			String code = StringArgumentType.getString(context, "code");
			 var succeed = APIHandler.join_room(code);
			 if (succeed != null){
				context.getSource().sendFeedback(Component.literal("Join room"));
				Minecraft instance = Minecraft.getInstance();
				instance.execute(() -> {
					try {
						Minecraft.getInstance().setScreen(new GobanScreen(Component.literal("Goban Screen")));
					} catch (Exception e) {
						context.getSource().sendFeedback(Component.literal("Failed to set GobanScreen"));
					}
				});
			 } else
				 context.getSource().sendFeedback(Component.literal("Failled to join room"));
			return 1;
		};

		Command<FabricClientCommandSource> create_local = context -> {
			var succeed = APIHandler.create_room(false, true);
			if (succeed != null){
				context.getSource().sendFeedback(Component.literal("Created a local Game"));
				Minecraft instance = Minecraft.getInstance();
				instance.execute(() -> {
					try {
						Minecraft.getInstance().setScreen(new GobanScreen(Component.literal("Goban Screen")));
					} catch (Exception e) {
						context.getSource().sendFeedback(Component.literal("Failed to set GobanScreen"));
					}
				});
			}
			else
				context.getSource().sendFeedback(Component.literal("Failed Created a local Game"));
			return 0;
		};

		Command<FabricClientCommandSource> create_remote = context -> {
			var succeed = APIHandler.create_room(false, false);
			if (succeed != null){
				context.getSource().sendFeedback(Component.literal("Created a remote Game"));
				context.getSource().sendFeedback(Component.literal("Invite token: " + succeed.getB()));
				context.getSource().sendFeedback(Component.literal("to join execute: /go"));
			}
			else
				context.getSource().sendFeedback(Component.literal("Failed Created a remote Game"));
			return 0;
		};

		Command<FabricClientCommandSource> help = context -> {
			context.getSource().sendFeedback(Component.literal("""
                    `/go` create an vs AI
                    `/go create local` to create an player vs player local
                    `/go create remote` to create an player vs player remotly
                    `/go join [code]` to join an player vs player
                    `/go giveUp` to giveUp the game"""
			));
			return 0;
		};

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("go")
					.executes(run)
					.then(ClientCommandManager.literal("create")
							.then(ClientCommandManager.literal("local")
									.executes(create_local))
							.then(ClientCommandManager.literal("remote")
									.executes(create_remote)))
					.then(ClientCommandManager.literal("giveUp")
							.executes(giveUp))
					.then(ClientCommandManager.literal("join")
							.then(ClientCommandManager.argument("code", StringArgumentType.string())
									.executes(join))
					)
					.then(ClientCommandManager.literal("help")
					.executes(help))
					);
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(context -> {
			APIHandler.give_up();
		});
	}
}