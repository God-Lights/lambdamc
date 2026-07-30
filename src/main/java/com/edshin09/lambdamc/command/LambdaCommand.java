package com.edshin09.lambdamc.command;

import com.edshin09.lambdamc.economy.LambdaBank;
import com.edshin09.lambdamc.screen.ShopScreenHandler;
import com.edshin09.lambdamc.shop.Shop;
import com.edshin09.lambdamc.shop.ShopRegistry;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class LambdaCommand {
	private LambdaCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(CommandManager.literal("lambdamc")
						.then(CommandManager.literal("shop")
								.executes(ctx -> listShops(ctx.getSource()))
								.then(CommandManager.argument("shopName", StringArgumentType.word())
										.suggests((ctx, builder) -> CommandSource.suggestMatching(ShopRegistry.getAll().keySet(), builder))
										.executes(ctx -> openShop(ctx.getSource(), StringArgumentType.getString(ctx, "shopName")))))
						.then(CommandManager.literal("rank")
								.executes(ctx -> showRank(ctx.getSource())))));
	}

	private static int listShops(ServerCommandSource source) {
		source.sendFeedback(() -> Text.translatable("lambdamc.shop.list.header").formatted(Formatting.GOLD), false);
		for (Shop shop : ShopRegistry.getAll().values()) {
			Text line = Text.translatable("lambdamc.shop.list.entry", shop.getDisplayName())
					.styled(style -> style
							.withColor(Formatting.YELLOW)
							.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lambdamc shop " + shop.getId()))
							.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("클릭해서 입장 / click to enter"))));
			source.sendFeedback(() -> line, false);
		}
		return 1;
	}

	private static int openShop(ServerCommandSource source, String shopId) {
		if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
			source.sendError(Text.literal("플레이어만 사용할 수 있는 명령어입니다."));
			return 0;
		}
		Optional<Shop> shopOpt = ShopRegistry.get(shopId);
		if (shopOpt.isEmpty()) {
			source.sendError(Text.translatable("lambdamc.shop.not_found", shopId));
			return 0;
		}

		LambdaBank bank = LambdaBank.get(source.getServer());
		ShopScreenHandler.open(player, shopOpt.get(), bank);
		return 1;
	}

	private static int showRank(ServerCommandSource source) {
		LambdaBank bank = LambdaBank.get(source.getServer());
		List<Map.Entry<UUID, Long>> entries = new ArrayList<>(bank.getBalances().entrySet());
		entries.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

		if (entries.isEmpty()) {
			source.sendFeedback(() -> Text.translatable("lambdamc.rank.empty"), false);
			return 0;
		}

		source.sendFeedback(() -> Text.translatable("lambdamc.rank.header").formatted(Formatting.GOLD), false);
		int limit = Math.min(10, entries.size());
		for (int i = 0; i < limit; i++) {
			Map.Entry<UUID, Long> entry = entries.get(i);
			String name = bank.getName(entry.getKey());
			String formatted = NumberFormat.getIntegerInstance(Locale.US).format(entry.getValue());
			int rank = i + 1;
			source.sendFeedback(() -> Text.translatable("lambdamc.rank.entry", rank, name, formatted), false);
		}
		return limit;
	}
}
