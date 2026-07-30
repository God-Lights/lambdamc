package com.edshin09.lambdamc.screen;

import com.edshin09.lambdamc.LambdaMC;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;

public final class LambdaScreenHandlers {
	public static final ScreenHandlerType<TradeScreenHandler> TRADE = new ScreenHandlerType<>(TradeScreenHandler::new);
	public static final ScreenHandlerType<ManageScreenHandler> MANAGE = new ScreenHandlerType<>(ManageScreenHandler::new);

	private LambdaScreenHandlers() {
	}

	public static void register() {
		Registry.register(Registries.SCREEN_HANDLER, LambdaMC.id("trade"), TRADE);
		Registry.register(Registries.SCREEN_HANDLER, LambdaMC.id("manage"), MANAGE);
	}
}
