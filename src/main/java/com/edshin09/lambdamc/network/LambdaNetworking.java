package com.edshin09.lambdamc.network;

import com.edshin09.lambdamc.LambdaMC;
import com.edshin09.lambdamc.shop.ShopSummary;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Server -> client packets. All shop *actions* (create/open/manage/buy/...)
 * are triggered through the {@code /lambdamc shop ...} command tree instead
 * of dedicated C2S packets, so client GUIs simply issue chat commands
 * (see {@code ClientPlayNetworkHandler#sendChatCommand}); only structured
 * data that needs to come back down to the client is sent as a packet here.
 */
public final class LambdaNetworking {
	/** Full balance sync, e.g. on join, after a purchase/sale, or when another player's shop pays out. */
	public static final Identifier BALANCE_SYNC = LambdaMC.id("balance_sync");
	/** Sent in response to {@code /lambdamc shop list ...} so the M-key menu can render structured rows. */
	public static final Identifier SHOP_LIST_SYNC = LambdaMC.id("shop_list_sync");

	private LambdaNetworking() {
	}

	public static void registerCommon() {
		// No C2S packets are registered: see class javadoc.
	}

	public static void sendBalance(ServerPlayerEntity player, long balance) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeLong(balance);
		ServerPlayNetworking.send(player, BALANCE_SYNC, buf);
	}

	public static void sendShopList(ServerPlayerEntity player, List<ShopSummary> shops) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(shops.size());
		for (ShopSummary shop : shops) {
			buf.writeUuid(shop.id());
			buf.writeString(shop.name());
			buf.writeString(shop.ownerName());
			buf.writeVarInt(shop.sellCount());
			buf.writeVarInt(shop.buyCount());
			buf.writeVarInt(shop.totalSales());
			buf.writeLong(shop.createdAt());
		}
		ServerPlayNetworking.send(player, SHOP_LIST_SYNC, buf);
	}
}
