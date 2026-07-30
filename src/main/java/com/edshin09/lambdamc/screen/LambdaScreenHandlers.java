package com.edshin09.lambdamc.screen;

import com.edshin09.lambdamc.LambdaMC;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;

public final class LambdaScreenHandlers {
	public static final ScreenHandlerType<ShopScreenHandler> SHOP = new ScreenHandlerType<>(ShopScreenHandler::new);

	private LambdaScreenHandlers() {
	}

	public static void register() {
		Registry.register(Registries.SCREEN_HANDLER, LambdaMC.id("shop"), SHOP);
	}
}
