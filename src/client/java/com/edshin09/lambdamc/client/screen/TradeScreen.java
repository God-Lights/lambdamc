package com.edshin09.lambdamc.client.screen;

import com.edshin09.lambdamc.client.LambdaClientBalance;
import com.edshin09.lambdamc.screen.TradeScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.text.NumberFormat;
import java.util.Locale;

/** Flat-drawn GUI for browsing/trading in the default shop or any custom shop. */
public class TradeScreen extends HandledScreen<TradeScreenHandler> {
	private static final int PANEL_COLOR = 0xC0101010;
	private static final int BORDER_COLOR = 0xFF3A3A3A;
	private static final int SLOT_COLOR = 0x60FFFFFF;

	public TradeScreen(TradeScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = 176;
		this.backgroundHeight = 18 + TradeScreenHandler.ROWS * 18 + 16;
	}

	@Override
	protected void drawBackground(DrawContext drawContext, float delta, int mouseX, int mouseY) {
		drawContext.fill(x, y, x + backgroundWidth, y + backgroundHeight, PANEL_COLOR);
		drawContext.drawBorder(x, y, backgroundWidth, backgroundHeight, BORDER_COLOR);

		for (int row = 0; row < TradeScreenHandler.ROWS; row++) {
			for (int col = 0; col < 9; col++) {
				int slotX = x + 7 + col * 18;
				int slotY = y + 17 + row * 18;
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
