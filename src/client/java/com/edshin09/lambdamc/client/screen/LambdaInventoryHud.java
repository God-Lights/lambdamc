package com.edshin09.lambdamc.client.screen;

import com.edshin09.lambdamc.client.LambdaClientBalance;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Draws the player's Lambda (λ) balance next to the recipe book toggle
 * button on the survival inventory screen.
 */
public final class LambdaInventoryHud {
	// Vanilla survival InventoryScreen background size; HandledScreen centers
	// itself on the scaled window using these dimensions in HandledScreen#init().
	private static final int BACKGROUND_WIDTH = 176;
	private static final int BACKGROUND_HEIGHT = 166;

	private LambdaInventoryHud() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof InventoryScreen)) {
				return;
			}
			ScreenEvents.afterRender(screen).register((s, drawContext, mouseX, mouseY, tickDelta) ->
					renderBalance(drawContext, client));
		});
	}

	private static void renderBalance(DrawContext drawContext, MinecraftClient client) {
		if (!LambdaClientBalance.isKnown()) {
			return;
		}

		int windowWidth = client.getWindow().getScaledWidth();
		int windowHeight = client.getWindow().getScaledHeight();
		int x = (windowWidth - BACKGROUND_WIDTH) / 2;
		int y = (windowHeight - BACKGROUND_HEIGHT) / 2;

		// The vanilla recipe book toggle button sits at roughly (x + 104, y + 22)
		// on the survival inventory screen; render just to the right of it.
		int textX = x + 128;
		int textY = y + 27;

		String formatted = NumberFormat.getIntegerInstance(Locale.US).format(LambdaClientBalance.get());
		Text text = Text.literal("λ " + formatted).formatted(Formatting.GOLD);

		drawContext.drawTextWithShadow(client.textRenderer, text, textX, textY, 0xFFFFFF);
	}
}
