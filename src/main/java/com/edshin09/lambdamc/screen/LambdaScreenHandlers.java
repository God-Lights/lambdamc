package com.edshin09.lambdamc.screen;

import com.edshin09.lambdamc.LambdaMC;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;

public final class LambdaScreenHandlers {
	public static final ScreenHandlerType<TradeScreenHandler> TRADE =
			new ScreenHandlerType<>(TradeScreenHandler::new, FeatureFlags.VANILLA_FEATURES);
	public static final ScreenHandlerType<ManageScreenHandler> MANAGE =
			new ScreenHandlerType<>(ManageScreenHandler::new, FeatureFlags.VANILLA_FEATURES);

	private LambdaScreenHandlers() {
	}

	public static void register() {
		Registry.register(Registries.SCREEN_HANDLER, LambdaMC.id("trade"), TRADE);
		Registry.register(Registries.SCREEN_HANDLER, LambdaMC.id("manage"), MANAGE);
	}
}
