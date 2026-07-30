package com.edshin09.lambdamc.client.screen;

import com.edshin09.lambdamc.screen.LambdaScreenHandlers;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public final class LambdaClientScreens {
	private LambdaClientScreens() {
	}

	public static void register() {
		HandledScreens.register(LambdaScreenHandlers.TRADE, TradeScreen::new);
		HandledScreens.register(LambdaScreenHandlers.MANAGE, ManageScreen::new);
	}
}
