package com.edshin09.lambdamc.client;

import com.edshin09.lambdamc.client.hud.LambdaBalanceHud;
import com.edshin09.lambdamc.client.keybind.LambdaKeyBindings;
import com.edshin09.lambdamc.client.screen.LambdaClientScreens;
import com.edshin09.lambdamc.client.screen.LambdaShopListCache;
import com.edshin09.lambdamc.network.LambdaNetworking;
import com.edshin09.lambdamc.shop.ShopSummary;
import com.edshin09.lambdamc.shop.ShopType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class LambdaMCClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		LambdaClientScreens.register();
		LambdaBalanceHud.register();
		LambdaKeyBindings.register();

		ClientPlayNetworking.registerGlobalReceiver(LambdaNetworking.BALANCE_SYNC, (client, handler, buf, responseSender) -> {
			long balance = buf.readLong();
			client.execute(() -> LambdaClientBalance.set(balance));
		});

		ClientPlayNetworking.registerGlobalReceiver(LambdaNetworking.SHOP_LIST_SYNC, (client, handler, buf, responseSender) -> {
			int count = buf.readVarInt();
			List<ShopSummary> shops = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				UUID id = buf.readUuid();
				String name = buf.readString();
				String ownerName = buf.readString();
				ShopType type = buf.readByte() == 0 ? ShopType.SELLING : ShopType.BUYING;
				int listingCount = buf.readVarInt();
				int totalSales = buf.readVarInt();
				long createdAt = buf.readLong();
				shops.add(new ShopSummary(id, name, ownerName, type, listingCount, totalSales, createdAt));
			}
			client.execute(() -> LambdaShopListCache.set(shops));
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			LambdaClientBalance.reset();
			LambdaShopListCache.set(List.of());
		});
	}
}
