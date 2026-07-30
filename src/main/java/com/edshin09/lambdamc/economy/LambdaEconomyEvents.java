package com.edshin09.lambdamc.economy;

import com.edshin09.lambdamc.config.LambdaConfig;
import com.edshin09.lambdamc.network.LambdaNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
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
				bank.openAccount(uuid, name, LambdaConfig.INSTANCE.startingBalance);
				player.sendMessage(Text.translatable("lambdamc.join.welcome"), false);
			} else {
				bank.updateName(uuid, name);
			}

			LambdaNetworking.sendBalance(player, bank.getBalance(uuid));

			List<String> notifications = bank.pollNotifications(uuid);
			if (!notifications.isEmpty()) {
				player.sendMessage(Text.translatable("lambdamc.notify.header").formatted(Formatting.GOLD), false);
				for (String message : notifications) {
					player.sendMessage(Text.literal(message).formatted(Formatting.YELLOW), false);
				}
			}
		});
	}
}
