package com.edshin09.lambdamc.client.screen;

import com.edshin09.lambdamc.client.LambdaClientBalance;
import com.edshin09.lambdamc.screen.TradeScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Flat-drawn GUI for browsing/trading in the default shop or any custom
 * shop. The default shop ("LambdaSharp") is laid out in labelled rows
 * (exchange / fixed / daily); custom shops are split left/right into
 * "buy here" and "sell here" columns, since a shop can offer both at once.
 * GRID_TOP must match the slot y-offset baked into {@code TradeScreenHandler}
 * (28 + row*18) so the drawn slot backgrounds line up with the real items.
 */
public class TradeScreen extends HandledScreen<TradeScreenHandler> {
	private static final int GRID_TOP = 28;
	private static final int SLOT_LEFT = 7;
	private static final int PANEL_COLOR = 0xC0161613;
	private static final int BORDER_COLOR = 0xFF4A3A2A;
	private static final int SLOT_COLOR = 0x60FFFFFF;
	private static final int DIVIDER_COLOR = 0xFF4A3A2A;
	private static final int EXCHANGE_TINT = 0x4055FFFF;
	private static final int FIXED_TINT = 0x40FF55FF;
	private static final int DAILY_TINT = 0x40FFAA00;
	private static final int BUY_SIDE_TINT = 0x4055FF55;
	private static final int SELL_SIDE_TINT = 0x40FFAA00;

	private final boolean isDefaultShop;

	public TradeScreen(TradeScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.isDefaultShop = title.getString().equals("LambdaSharp");
		this.backgroundWidth = 176;
		this.backgroundHeight = GRID_TOP + TradeScreenHandler.ROWS * 18 + 9;
	}

	@Override
	protected void drawBackground(DrawContext drawContext, float delta, int mouseX, int mouseY) {
		drawContext.fill(x, y, x + backgroundWidth, y + backgroundHeight, PANEL_COLOR);
		drawContext.drawBorder(x, y, backgroundWidth, backgroundHeight, BORDER_COLOR);
		drawContext.fill(x + 1, y + GRID_TOP - 4, x + backgroundWidth - 1, y + GRID_TOP - 2, DIVIDER_COLOR);

		for (int row = 0; row < TradeScreenHandler.ROWS; row++) {
			for (int col = 0; col < 9; col++) {
				int slotX = x + SLOT_LEFT + col * 18;
				int slotY = y + GRID_TOP + row * 18;
				drawContext.fill(slotX, slotY, slotX + 18, slotY + 18, SLOT_COLOR);
				drawContext.fill(slotX, slotY, slotX + 18, slotY + 18, sectionTint(row, col));
			}
		}

		if (isDefaultShop) {
			drawDefaultShopSections(drawContext);
		} else {
			drawCustomShopSplit(drawContext);
		}
	}

	/** Faint per-section color wash so each part of the shop reads as its own "counter". */
	private int sectionTint(int row, int col) {
		if (isDefaultShop) {
			return switch (row) {
				case 0 -> EXCHANGE_TINT;
				case 1 -> FIXED_TINT;
				default -> DAILY_TINT;
			};
		}
		if (col < 4) {
			return BUY_SIDE_TINT;
		}
		if (col > 4) {
			return SELL_SIDE_TINT;
		}
		return 0;
	}

	/** Row 0 = exchange, row 1 = fixed offers, rows 2-4 = daily rotation. */
	private void drawDefaultShopSections(DrawContext drawContext) {
		int rowTop1 = y + GRID_TOP + 1 * 18;
		int rowTop2 = y + GRID_TOP + 2 * 18;
		drawContext.fill(x + 4, rowTop1 - 1, x + backgroundWidth - 4, rowTop1 + 1, DIVIDER_COLOR);
		drawContext.fill(x + 4, rowTop2 - 1, x + backgroundWidth - 4, rowTop2 + 1, DIVIDER_COLOR);
	}

	/** Cols 0-3 = sell-to-visitor listings ("buy here"), col 4 = divider, cols 5-8 = buy-from-visitor listings ("sell here"). */
	private void drawCustomShopSplit(DrawContext drawContext) {
		int dividerX = x + SLOT_LEFT + 4 * 18;
		int gridTop = y + GRID_TOP;
		int gridBottom = gridTop + TradeScreenHandler.ROWS * 18;
		drawContext.fill(dividerX + 7, gridTop, dividerX + 9, gridBottom, DIVIDER_COLOR);
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

		int labelY = GRID_TOP - 10;
		if (isDefaultShop) {
			Text label = Text.literal("교환").formatted(Formatting.AQUA)
					.append(Text.literal(" / ").formatted(Formatting.DARK_GRAY))
					.append(Text.literal("고정").formatted(Formatting.LIGHT_PURPLE))
					.append(Text.literal(" / ").formatted(Formatting.DARK_GRAY))
					.append(Text.literal("일일 상품").formatted(Formatting.GOLD));
			drawContext.drawText(textRenderer, label, 8, labelY, 0xFFFFFF, false);
		} else {
			drawContext.drawText(textRenderer, Text.literal("◀ 구매").formatted(Formatting.GREEN), 8, labelY, 0xFFFFFF, false);
			Text sellLabel = Text.literal("판매 ▶").formatted(Formatting.GOLD);
			int sellWidth = textRenderer.getWidth(sellLabel);
			drawContext.drawText(textRenderer, sellLabel, backgroundWidth - sellWidth - 8, labelY, 0xFFFFFF, false);
		}
	}

	/**
	 * Manual hover hit-test instead of relying on {@code focusedSlot}: this screen's
	 * slots are drawn every frame from a supplier-refreshed display inventory, and the
	 * default tracking was found to miss hover state in testing, so tooltips never
	 * appeared. Hit-testing directly against the handler's slots is a robust fallback.
	 */
	@Override
	protected void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
		for (Slot slot : handler.slots) {
			if (!slot.hasStack()) {
				continue;
			}
			int slotX = x + slot.x;
			int slotY = y + slot.y;
			if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
				context.drawItemTooltip(this.textRenderer, slot.getStack(), mouseX, mouseY);
				return;
			}
		}
	}
}
