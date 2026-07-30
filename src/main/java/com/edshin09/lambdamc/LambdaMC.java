package com.edshin09.lambdamc;

import com.edshin09.lambdamc.command.LambdaCommand;
import com.edshin09.lambdamc.economy.LambdaEconomyEvents;
import com.edshin09.lambdamc.item.LambdaItemEvents;
import com.edshin09.lambdamc.item.LambdaItems;
import com.edshin09.lambdamc.network.LambdaNetworking;
import com.edshin09.lambdamc.screen.LambdaScreenHandlers;
import com.edshin09.lambdamc.shop.ShopRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LambdaMC implements ModInitializer {
	public static final String MOD_ID = "lambdamc";
	public static final Logger LOGGER = LoggerFactory.getLogger("LambdaMC");

	/** Starting balance granted the first time a player ever joins the server. */
	public static final long STARTING_BALANCE = 2000L;

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		LambdaItems.register();
		LambdaItemEvents.register();
		LambdaScreenHandlers.register();
		LambdaNetworking.registerCommon();
		ShopRegistry.bootstrap();
		LambdaCommand.register();
		LambdaEconomyEvents.register();

		LOGGER.info("LambdaMC 초기화 완료 (currency: Lambda / λ)");
	}
}
