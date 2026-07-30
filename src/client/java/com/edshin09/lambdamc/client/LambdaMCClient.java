package com.edshin09.lambdamc.client;

import com.edshin09.lambdamc.client.screen.LambdaClientScreens;
import com.edshin09.lambdamc.client.screen.LambdaInventoryHud;
import com.edshin09.lambdamc.network.LambdaNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class LambdaMCClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		LambdaClientScreens.register();
		LambdaInventoryHud.register();

		ClientPlayNetworking.registerGlobalReceiver(LambdaNetworking.BALANCE_SYNC, (client, handler, buf, responseSender) -> {
			long balance = buf.readLong();
			client.execute(() -> LambdaClientBalance.set(balance));
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> LambdaClientBalance.reset());
	}
}
