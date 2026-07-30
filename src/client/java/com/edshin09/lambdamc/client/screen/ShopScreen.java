package com.edshin09.lambdamc.client.screen;

import com.edshin09.lambdamc.client.LambdaClientBalance;
import com.edshin09.lambdamc.screen.ShopScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.text.NumberFormat;
import java.util.Locale;

/** Simple flat-drawn GUI for the LambdaMC shop screens (no vanilla chest texture dependency). */
public class ShopScreen extends HandledScreen<ShopScreenHandler> {
	private static final int PANEL_COLOR = 0xC0101010;
	private static final int BORDER_COLOR = 0xFF3A3A3A;
	private static final int SLOT_COLOR = 0x60FFFFFF;

	public ShopScreen(ShopScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = 176;
		this.backgroundHeight = 18 + ShopScreenHandler.ROWS * 18 + 16;
	}

	@Override
	protected void drawBackground(DrawContext drawContext, float delta, int mouseX, int mouseY) {
		int panelX = x;
		int panelY = y;
		drawContext.fill(panelX, panelY, panelX + backgroundWidth, panelY + backgroundHeight, PANEL_COLOR);
		drawContext.drawBorder(panelX, panelY, backgroundWidth, backgroundHeight, BORDER_COLOR);

		for (int row = 0; row < ShopScreenHandler.ROWS; row++) {
			for (int col = 0; col < 9; col++) {
				int slotX = panelX + 7 + col * 18;
				int slotY = panelY + 17 + row * 18;
				drawContext.fill(slotX, slotY, slotX + 18, slotY + 18, SLOT_COLOR);
			}
		}
	}

	@Override
	protected void drawForeground(DrawContext drawContext, int mouseX, int mouseY) {
		drawContext.drawText(textRenderer, title, 8, 6, 0xFFFFFF, false);

		String balanceText = LambdaClientBalance.isKnown()
				? "λ " + NumberFormat.getIntegerInstance(Locale.US).format(LambdaClientBalance.get())
				: "λ ...";
		Text balance = Text.literal(balanceText).formatted(Formatting.GOLD);
		int textWidth = textRenderer.getWidth(balance);
		drawContext.drawText(textRenderer, balance, backgroundWidth - textWidth - 8, 6, 0xFFFFFF, false);
	}
}
