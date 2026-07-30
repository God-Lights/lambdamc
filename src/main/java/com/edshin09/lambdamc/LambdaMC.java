package com.edshin09.lambdamc;

import com.edshin09.lambdamc.command.LambdaCommand;
import com.edshin09.lambdamc.config.LambdaConfig;
import com.edshin09.lambdamc.economy.LambdaEconomyEvents;
import com.edshin09.lambdamc.item.LambdaItemEvents;
import com.edshin09.lambdamc.item.LambdaItems;
import com.edshin09.lambdamc.network.LambdaNetworking;
import com.edshin09.lambdamc.screen.LambdaScreenHandlers;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LambdaMC implements ModInitializer {
	public static final String MOD_ID = "lambdamc";
	public static final Logger LOGGER = LoggerFactory.getLogger("LambdaMC");

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		LambdaConfig.load();
		LambdaItems.register();
		LambdaItemEvents.register();
		LambdaScreenHandlers.register();
		LambdaNetworking.registerCommon();
		LambdaCommand.register();
		LambdaEconomyEvents.register();

		LOGGER.info("LambdaMC 초기화 완료 (currency: Lambda / λ)");
	}
}
