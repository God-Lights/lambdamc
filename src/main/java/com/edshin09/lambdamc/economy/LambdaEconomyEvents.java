package com.edshin09.lambdamc.economy;

import com.edshin09.lambdamc.LambdaMC;
import com.edshin09.lambdamc.network.LambdaNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

public final class LambdaEconomyEvents {
	private LambdaEconomyEvents() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			LambdaBank bank = LambdaBank.get(server);
			UUID uuid = player.getUuid();
			String name = player.getGameProfile().getName();

			if (!bank.hasAccount(uuid)) {
				bank.openAccount(uuid, name, LambdaMC.STARTING_BALANCE);
				player.sendMessage(Text.translatable("lambdamc.join.welcome"), false);
			} else {
				bank.updateName(uuid, name);
			}

			LambdaNetworking.sendBalance(player, bank.getBalance(uuid));
		});
	}
}
