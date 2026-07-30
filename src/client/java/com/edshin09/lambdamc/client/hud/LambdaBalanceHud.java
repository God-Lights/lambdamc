package com.edshin09.lambdamc.client.hud;

import com.edshin09.lambdamc.client.LambdaClientBalance;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.text.NumberFormat;
import java.util.Locale;

/** Always-on-screen λ balance display, shown in the top-right corner during normal gameplay. */
public final class LambdaBalanceHud {
	private LambdaBalanceHud() {
	}

	public static void register() {
		HudRenderCallback.EVENT.register(LambdaBalanceHud::render);
	}

	private static void render(DrawContext drawContext, float tickDelta) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.options.hudHidden || !LambdaClientBalance.isKnown()) {
			return;
		}

		String formatted = NumberFormat.getIntegerInstance(Locale.US).format(LambdaClientBalance.get());
		Text text = Text.literal("λ " + formatted).formatted(Formatting.GOLD, Formatting.BOLD);

		int width = client.getWindow().getScaledWidth();
		int textWidth = client.textRenderer.getWidth(text);
		int x = width - textWidth - 6;
		int y = 6;

		drawContext.fill(x - 4, y - 3, x + textWidth + 4, y + client.textRenderer.fontHeight + 3, 0x80000000);
		drawContext.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFFFFF);
	}
}
