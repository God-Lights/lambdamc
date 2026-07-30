package com.edshin09.lambdamc.network;

import com.edshin09.lambdamc.LambdaMC;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Server -> client packets used to keep the client-side balance cache
 * (shown in the inventory HUD and shop screens) in sync.
 */
public final class LambdaNetworking {
	/** Full balance sync, e.g. on join or after a purchase/sale. */
	public static final Identifier BALANCE_SYNC = LambdaMC.id("balance_sync");

	private LambdaNetworking() {
	}

	public static void registerCommon() {
		// No server-bound (client -> server) packets are required: all shop
		// actions are triggered through vanilla ScreenHandler slot clicks,
		// which are already routed to the server by vanilla networking.
	}

	public static void sendBalance(ServerPlayerEntity player, long balance) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeLong(balance);
		ServerPlayNetworking.send(player, BALANCE_SYNC, buf);
	}
}
