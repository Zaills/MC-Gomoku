package zaills.gomoku;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class GobanScreen extends Screen {
	final static Identifier BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(GomokuClient.MOD_ID, "textures/gui/goban_hud.png");
	final static Identifier BLACK_STONE_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/block/blackstone.png");
	final static Identifier WHITE_STONE_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/block/white_wool.png");
	final static Identifier EMPTY_CELL_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/block/oak_planks.png");
	final static Identifier SUGGEST_CELL_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/block/gold_block.png");
	final static int[] positionTextureX = new int[]{
			0, 2, 3, 2, 2, 3, 2, 2, 3, 2, 3, 2, 2, 3, 2, 2, 3, 2, 2
	};
	final static int[] positionTextureY = new int[]{
			0, 1, 2, 1, 1, 1, 2, 1, 2, 1, 1, 2, 1, 2, 1, 1, 2, 1, 2
	};
	static int[][] boardState;

	private float xMouse;
	private float yMouse;

	private int x_suggest = -1;
	private int y_suggest = -1;

	protected GobanScreen(Component component) {
		super(component);
	}

	@Override
	protected void init() {
		APIHandler.create_room(true, false);
		boardState = APIHandler.getBoardState();
	}

	@Override
	public void render(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {
		super.render(guiGraphics, i, j, f);

		if (APIHandler.isBoardFree()) {
			this.minecraft.gui.getChat().addMessage(Component.literal("Party ended"));
			this.minecraft.setScreen(null);
		}

		this.xMouse = i;
		this.yMouse = j;

		int imageWidth = 256;
		int imageHeight = 256;
		int x = (this.width - imageWidth) / 2;
		int y = (this.height - imageHeight) / 2;
		guiGraphics.drawString(this.font, Component.literal("Goban Screen"), 40, 40 - this.font.lineHeight - 10, 0xFFFFFF, true);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
		renderBoard(guiGraphics, x, y);
	}

	private void renderBoard(GuiGraphics guiGraphics, int startX, int startY) {
		waitForBoardUpdate();
		if (boardState == null) {
			guiGraphics.drawString(this.font, Component.literal("Failed to load board state."), 50, 80, 0xFF0000, true);
			return;
		}
		int cellSize = 10;
		startX += 12;
		startY += 25;
		for (int row = 0; row < boardState.length; row++) {
			for (int col = 0; col < boardState[row].length; col++) {
				int x = startX + col * cellSize + spacingX(col);
				int y = startY + row * cellSize + spacingY(row);
				if (col == x_suggest && row == y_suggest) {
					guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SUGGEST_CELL_TEXTURE, x, y, 0, 0, 10, 10, 16, 16); // Suggestion highlight
				} else if (boardState[row][col] == 1) {
					guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BLACK_STONE_TEXTURE, x, y, 0, 0, 10, 10, 16, 16); // Black stone
				} else if (boardState[row][col] == 2) {
					guiGraphics.blit(RenderPipelines.GUI_TEXTURED, WHITE_STONE_TEXTURE, x, y, 0,0, 10, 10, 16, 16); // White stone
				} else {
//					guiGraphics.blit(RenderPipelines.GUI_TEXTURED, EMPTY_CELL_TEXTURE, x, y, 0,0, 10, 10, 16, 16);
				}
			}
		}
	}

	private int spacingX(int idx) {
		int count = 0;
		for (int i = 0; i <= idx; i++) {
			count += positionTextureX[i];
		}
		return count;
	}

	private int spacingY(int idx) {
		int count = 0;
		for (int i = 0; i <= idx; i++) {
			count += positionTextureY[i];
		}
		return count;
	}

	@Override
	public boolean mouseReleased(@NotNull MouseButtonEvent mouseButtonEvent) {
		int cellSize = 10;
		int startX = (this.width - 256) / 2 + 12;
		int startY = (this.height - 256) / 2 + 25;
		for (int row = 0; row < 19; row++) {
			for (int col = 0; col < 19; col++) {
				if (boardState == null || boardState[row][col] != 0) {
					continue;
				}
				int x = startX + col * cellSize + spacingX(col);
				int y = startY + row * cellSize + spacingY(row);
				if (this.xMouse >= x && this.xMouse <= x + cellSize &&
						this.yMouse >= y && this.yMouse <= y + cellSize) {
					GomokuClient.LOGGER.info("Clicked on cell: ({}, {})", row, col);
					if (APIHandler.sendMove(col, row, 1)) {
						x_suggest = -1;
						y_suggest = -1;
					}
					return true;
				}
			}
		}
		return super.mouseReleased(mouseButtonEvent);
	}

	@Override
	public boolean keyReleased(KeyEvent keyEvent) {
		if (keyEvent.key() == 72) {
			int[] suggest = APIHandler.getAISuggest();
			if (suggest[0] != -1 && suggest[1] != -1) {
				x_suggest = suggest[0];
				y_suggest = suggest[1];
				GomokuClient.LOGGER.info("AI Suggestion: ({}, {})", x_suggest, y_suggest);
			}
		}
		return super.keyReleased(keyEvent);
	}

	private void waitForBoardUpdate() {
		int [][] board = APIHandler.getBoardState();
		if (board == null) {
			this.minecraft.gui.getChat().addMessage(Component.literal("Could not connect to the server"));
			this.minecraft.setScreen(null);
		}
		boardState = board;
	}
}