package com.edshin09.lambdamc.command;

import com.edshin09.lambdamc.economy.LambdaBank;
import com.edshin09.lambdamc.item.LambdaItems;
import com.edshin09.lambdamc.network.LambdaNetworking;
import com.edshin09.lambdamc.screen.ManageScreenHandler;
import com.edshin09.lambdamc.screen.TradeScreenHandler;
import com.edshin09.lambdamc.shop.CustomShop;
import com.edshin09.lambdamc.shop.CustomShopData;
import com.edshin09.lambdamc.shop.DefaultShop;
import com.edshin09.lambdamc.shop.ShopManager;
import com.edshin09.lambdamc.shop.ShopSummary;
import com.edshin09.lambdamc.shop.ShopType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
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
import java.util.stream.Collectors;

public final class LambdaCommand {
	private LambdaCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(CommandManager.literal("lambdamc")
						.then(CommandManager.literal("shop")
								.executes(ctx -> listShops(ctx.getSource(), null, null))
								.then(CommandManager.literal("list")
										.executes(ctx -> listShops(ctx.getSource(), null, null))
										.then(CommandManager.argument("sort", StringArgumentType.word())
												.executes(ctx -> listShops(ctx.getSource(), StringArgumentType.getString(ctx, "sort"), null))
												.then(CommandManager.argument("search", StringArgumentType.greedyString())
														.executes(ctx -> listShops(ctx.getSource(),
																StringArgumentType.getString(ctx, "sort"),
																StringArgumentType.getString(ctx, "search"))))))
								.then(CommandManager.literal("open")
										.then(CommandManager.argument("name", StringArgumentType.string())
												.suggests(LambdaCommand::suggestAllShops)
												.executes(ctx -> openShop(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
								.then(CommandManager.literal("create")
										.then(CommandManager.argument("name", StringArgumentType.string())
												.executes(ctx -> createShop(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
								.then(CommandManager.literal("manage")
										.then(CommandManager.argument("name", StringArgumentType.string())
												.suggests(LambdaCommand::suggestOwnShops)
												.executes(ctx -> manageShop(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
								.then(CommandManager.literal("addproduct")
										.then(CommandManager.argument("name", StringArgumentType.string())
												.suggests(LambdaCommand::suggestOwnShops)
												.then(CommandManager.argument("kind", StringArgumentType.word())
														.then(CommandManager.argument("price", IntegerArgumentType.integer(0))
																.executes(ctx -> addProduct(ctx.getSource(),
																		StringArgumentType.getString(ctx, "name"),
																		StringArgumentType.getString(ctx, "kind"),
																		IntegerArgumentType.getInteger(ctx, "price")))))))
								.then(CommandManager.literal("setprice")
										.then(CommandManager.argument("name", StringArgumentType.string())
												.suggests(LambdaCommand::suggestOwnShops)
												.then(CommandManager.argument("index", IntegerArgumentType.integer(1))
														.then(CommandManager.argument("price", IntegerArgumentType.integer(0))
																.executes(ctx -> setPrice(ctx.getSource(),
																		StringArgumentType.getString(ctx, "name"),
																		IntegerArgumentType.getInteger(ctx, "index"),
																		IntegerArgumentType.getInteger(ctx, "price")))))))
								.then(CommandManager.literal("removeproduct")
										.then(CommandManager.argument("name", StringArgumentType.string())
												.suggests(LambdaCommand::suggestOwnShops)
												.then(CommandManager.argument("index", IntegerArgumentType.integer(1))
														.executes(ctx -> removeProduct(ctx.getSource(),
																StringArgumentType.getString(ctx, "name"),
																IntegerArgumentType.getInteger(ctx, "index"))))))
								.then(CommandManager.literal("delete")
										.then(CommandManager.argument("name", StringArgumentType.string())
												.suggests(LambdaCommand::suggestOwnShops)
												.executes(ctx -> deleteShop(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
								.then(CommandManager.literal("warehouse")
										.then(CommandManager.argument("name", StringArgumentType.string())
												.suggests(LambdaCommand::suggestOwnShops)
												.executes(ctx -> openWarehouse(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))))
						.then(CommandManager.literal("rank")
								.executes(ctx -> showRank(ctx.getSource())))
						.then(CommandManager.literal("admin")
								.requires(source -> source.hasPermissionLevel(2))
								.then(CommandManager.literal("give")
										.then(CommandManager.argument("player", StringArgumentType.word())
												.then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
														.executes(ctx -> adminGive(ctx.getSource(),
																StringArgumentType.getString(ctx, "player"),
																IntegerArgumentType.getInteger(ctx, "amount"))))))
								.then(CommandManager.literal("set")
										.then(CommandManager.argument("player", StringArgumentType.word())
												.then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
														.executes(ctx -> adminSet(ctx.getSource(),
																StringArgumentType.getString(ctx, "player"),
																IntegerArgumentType.getInteger(ctx, "amount"))))))
								.then(CommandManager.literal("voucher")
										.then(CommandManager.argument("player", StringArgumentType.word())
												.then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
														.executes(ctx -> adminVoucher(ctx.getSource(),
																StringArgumentType.getString(ctx, "player"),
																IntegerArgumentType.getInteger(ctx, "amount"), 1))
														.then(CommandManager.argument("count", IntegerArgumentType.integer(1, 64))
																.executes(ctx -> adminVoucher(ctx.getSource(),
																		StringArgumentType.getString(ctx, "player"),
																		IntegerArgumentType.getInteger(ctx, "amount"),
																		IntegerArgumentType.getInteger(ctx, "count")))))))
								.then(CommandManager.literal("stats")
										.executes(ctx -> adminStats(ctx.getSource())))
								.then(CommandManager.literal("deleteshop")
										.then(CommandManager.argument("name", StringArgumentType.string())
												.suggests(LambdaCommand::suggestAllShops)
												.executes(ctx -> adminDeleteShop(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
								.then(CommandManager.literal("log")
										.executes(ctx -> adminLog(ctx.getSource(), 20))
										.then(CommandManager.argument("count", IntegerArgumentType.integer(1, 500))
												.executes(ctx -> adminLog(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count"))))))));
	}

	// ---------------------------------------------------------------------
	// dedicated-server gate: this mod's economy/shops are multiplayer-server
	// content only, so singleplayer/LAN worlds never accumulate any of it.
	// ---------------------------------------------------------------------

	private static boolean requireDedicated(ServerCommandSource source) {
		if (!source.getServer().isDedicated()) {
			source.sendError(Text.translatable("lambdamc.dedicated_only"));
			return false;
		}
		return true;
	}

	// ---------------------------------------------------------------------
	// shop list / open
	// ---------------------------------------------------------------------

	private static int listShops(ServerCommandSource source, String sort, String search) {
		if (!requireDedicated(source)) {
			return 0;
		}
		MinecraftServer server = source.getServer();
		CustomShopData data = CustomShopData.get(server);
		CustomShopData.Comparator comparator = CustomShopData.Comparator.parse(sort);
		List<CustomShop> shops = data.search(search, comparator);

		source.sendFeedback(() -> Text.translatable("lambdamc.shop.list.header").formatted(Formatting.GOLD), false);
		Text defaultLine = Text.literal("» LambdaSharp (기본 상점)")
				.styled(style -> style.withColor(Formatting.AQUA)
						.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lambdamc shop open \"" + DefaultShop.NAME + "\""))
						.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("클릭해서 입장"))));
		source.sendFeedback(() -> defaultLine, false);

		for (CustomShop shop : shops) {
			Text line = Text.literal("» " + shop.getName() + " (" + shop.getOwnerName() + ")")
					.styled(style -> style.withColor(Formatting.YELLOW)
							.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lambdamc shop open \"" + escape(shop.getName()) + "\""))
							.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("클릭해서 입장"))));
			source.sendFeedback(() -> line, false);
		}

		if (source.getEntity() instanceof ServerPlayerEntity player) {
			List<ShopSummary> summaries = shops.stream().map(ShopSummary::of).collect(Collectors.toList());
			LambdaNetworking.sendShopList(player, summaries);
		}

		return shops.size();
	}

	private static int openShop(ServerCommandSource source, String name) {
		if (!requireDedicated(source)) {
			return 0;
		}
		if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
			source.sendError(Text.literal("플레이어만 사용할 수 있는 명령어입니다."));
			return 0;
		}
		MinecraftServer server = source.getServer();

		if (name.equalsIgnoreCase(DefaultShop.NAME)) {
			TradeScreenHandler.open(player, "LambdaSharp", () -> DefaultShop.buildOffers(server));
			return 1;
		}

		Optional<CustomShop> found = CustomShopData.get(server).findByName(name);
		if (found.isEmpty()) {
			source.sendError(Text.translatable("lambdamc.shop.not_found", name));
			return 0;
		}
		CustomShop shop = found.get();
		TradeScreenHandler.open(player, shop.getName(), () -> ShopManager.buildOffers(server, shop));
		return 1;
	}

	// ---------------------------------------------------------------------
	// custom shop CRUD
	// ---------------------------------------------------------------------

	private static int createShop(ServerCommandSource source, String name) {
		if (!requireDedicated(source)) {
			return 0;
		}
		if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
			source.sendError(Text.literal("플레이어만 사용할 수 있는 명령어입니다."));
			return 0;
		}
		ShopManager.Result result = ShopManager.createShop(player, name);
		if (result.success()) {
			source.sendFeedback(() -> Text.literal(result.message()).formatted(Formatting.GREEN), false);
			return 1;
		} else {
			source.sendError(Text.literal(result.message()));
			return 0;
		}
	}

	private static int manageShop(ServerCommandSource source, String name) {
		if (!requireDedicated(source)) {
			return 0;
		}
		if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
			source.sendError(Text.literal("플레이어만 사용할 수 있는 명령어입니다."));
			return 0;
		}
		Optional<CustomShop> found = CustomShopData.get(source.getServer()).findByName(name);
		if (found.isEmpty()) {
			source.sendError(Text.translatable("lambdamc.shop.not_found", name));
			return 0;
		}
		CustomShop shop = found.get();
		if (!shop.getOwner().equals(player.getUuid())) {
			source.sendError(Text.literal("본인의 상점만 관리할 수 있습니다."));
			return 0;
		}
		ManageScreenHandler.open(player, shop);
		return 1;
	}

	private static int addProduct(ServerCommandSource source, String name, String kindRaw, int price) {
		if (!requireDedicated(source)) {
			return 0;
		}
		if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
			return 0;
		}
		ShopType kind = ShopType.parse(kindRaw);
		ShopManager.Result result = ShopManager.addProduct(player, name, kind, price);
		source.sendFeedback(() -> Text.literal(result.message()).formatted(result.success() ? Formatting.GREEN : Formatting.RED), false);
		refreshIfManaging(player);
		return result.success() ? 1 : 0;
	}

	private static int setPrice(ServerCommandSource source, String name, int index, int price) {
		if (!requireDedicated(source)) {
			return 0;
		}
		if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
			return 0;
		}
		ShopManager.Result result = ShopManager.setPrice(player, name, index, price);
		source.sendFeedback(() -> Text.literal(result.message()).formatted(result.success() ? Formatting.GREEN : Formatting.RED), false);
		refreshIfManaging(player);
		return result.success() ? 1 : 0;
	}

	private static int removeProduct(ServerCommandSource source, String name, int index) {
		if (!requireDedicated(source)) {
			return 0;
		}
		if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
			return 0;
		}
		ShopManager.Result result = ShopManager.removeProduct(player, name, index);
		source.sendFeedback(() -> Text.literal(result.message()).formatted(result.success() ? Formatting.GREEN : Formatting.RED), false);
		refreshIfManaging(player);
		return result.success() ? 1 : 0;
	}

	/** Re-syncs the player's currently open shop-management screen, if any, after a command-driven edit. */
	private static void refreshIfManaging(ServerPlayerEntity player) {
		if (player.currentScreenHandler instanceof ManageScreenHandler manageScreenHandler) {
			manageScreenHandler.refresh();
		}
	}

	private static int deleteShop(ServerCommandSource source, String name) {
		if (!requireDedicated(source)) {
			return 0;
		}
		if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
			return 0;
		}
		ShopManager.Result result = ShopManager.deleteShop(player, name, false);
		source.sendFeedback(() -> Text.literal(result.message()).formatted(result.success() ? Formatting.GREEN : Formatting.RED), false);
		return result.success() ? 1 : 0;
	}

	private static int openWarehouse(ServerCommandSource source, String name) {
		if (!requireDedicated(source)) {
			return 0;
		}
		if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
			return 0;
		}
		Optional<CustomShop> found = CustomShopData.get(source.getServer()).findByName(name);
		if (found.isEmpty()) {
			source.sendError(Text.translatable("lambdamc.shop.not_found", name));
			return 0;
		}
		CustomShop shop = found.get();
		if (!ShopManager.canOpenWarehouse(player, shop)) {
			source.sendError(Text.literal("본인의 상점 창고만 열 수 있습니다."));
			return 0;
		}
		player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
				(syncId, inv, p) -> GenericContainerScreenHandler.createGeneric9x3(syncId, inv, shop.getStorage()),
				Text.literal("[창고] " + shop.getName())));
		return 1;
	}

	// ---------------------------------------------------------------------
	// rank / admin
	// ---------------------------------------------------------------------

	private static int showRank(ServerCommandSource source) {
		if (!requireDedicated(source)) {
			return 0;
		}
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
			String formatted = format(entry.getValue());
			int rank = i + 1;
			source.sendFeedback(() -> Text.translatable("lambdamc.rank.entry", rank, name, formatted), false);
		}
		return limit;
	}

	private static int adminGive(ServerCommandSource source, String playerName, int amount) {
		if (!requireDedicated(source)) {
			return 0;
		}
		MinecraftServer server = source.getServer();
		Optional<com.mojang.authlib.GameProfile> profile = server.getUserCache() == null
				? Optional.empty() : server.getUserCache().findByName(playerName);
		if (profile.isEmpty()) {
			source.sendError(Text.literal("플레이어를 찾을 수 없습니다: " + playerName));
			return 0;
		}
		UUID uuid = profile.get().getId();
		LambdaBank bank = LambdaBank.get(server);
		bank.add(uuid, playerName, amount);
		bank.log(source.getName() + "이(가) " + playerName + "에게 " + amount + "λ 지급 (관리자)");
		ServerPlayerEntity online = server.getPlayerManager().getPlayer(uuid);
		if (online != null) {
			LambdaNetworking.sendBalance(online, bank.getBalance(uuid));
		}
		source.sendFeedback(() -> Text.literal(playerName + "에게 " + format(amount) + " λ 지급했습니다."), true);
		return 1;
	}

	private static int adminSet(ServerCommandSource source, String playerName, int amount) {
		if (!requireDedicated(source)) {
			return 0;
		}
		MinecraftServer server = source.getServer();
		Optional<com.mojang.authlib.GameProfile> profile = server.getUserCache() == null
				? Optional.empty() : server.getUserCache().findByName(playerName);
		if (profile.isEmpty()) {
			source.sendError(Text.literal("플레이어를 찾을 수 없습니다: " + playerName));
			return 0;
		}
		UUID uuid = profile.get().getId();
		LambdaBank bank = LambdaBank.get(server);
		bank.setBalance(uuid, playerName, amount);
		bank.log(source.getName() + "이(가) " + playerName + "의 잔액을 " + amount + "λ로 설정 (관리자)");
		ServerPlayerEntity online = server.getPlayerManager().getPlayer(uuid);
		if (online != null) {
			LambdaNetworking.sendBalance(online, bank.getBalance(uuid));
		}
		source.sendFeedback(() -> Text.literal(playerName + "의 잔액을 " + format(amount) + " λ로 설정했습니다."), true);
		return 1;
	}

	/** Gives an online player physical, redeemable λ voucher item(s) instead of editing their balance directly. */
	private static int adminVoucher(ServerCommandSource source, String playerName, int amount, int count) {
		if (!requireDedicated(source)) {
			return 0;
		}
		MinecraftServer server = source.getServer();
		ServerPlayerEntity target = server.getPlayerManager().getPlayer(playerName);
		if (target == null) {
			source.sendError(Text.literal("접속 중인 플레이어만 교환권을 받을 수 있습니다: " + playerName));
			return 0;
		}

		net.minecraft.item.ItemStack voucher = LambdaItems.createVoucher(amount);
		voucher.setCount(count);
		if (!target.getInventory().insertStack(voucher)) {
			target.dropItem(voucher, false);
		}

		LambdaBank.get(server).log(source.getName() + "이(가) " + playerName + "에게 람다 교환권(λ " + amount + ") x" + count + " 지급 (관리자)");
		int finalCount = count;
		source.sendFeedback(() -> Text.literal(playerName + "에게 람다 교환권(λ " + format(amount) + ") x" + finalCount + "을(를) 지급했습니다."), true);
		return 1;
	}

	private static int adminStats(ServerCommandSource source) {
		if (!requireDedicated(source)) {
			return 0;
		}
		LambdaBank bank = LambdaBank.get(source.getServer());
		long total = bank.getBalances().values().stream().mapToLong(Long::longValue).sum();
		int players = bank.getBalances().size();
		int shops = CustomShopData.get(source.getServer()).all().size();

		source.sendFeedback(() -> Text.literal("=== LambdaMC 경제 통계 ===").formatted(Formatting.GOLD), false);
		source.sendFeedback(() -> Text.literal("총 유통량: " + format(total) + " λ"), false);
		source.sendFeedback(() -> Text.literal("계좌 보유 플레이어 수: " + players), false);
		source.sendFeedback(() -> Text.literal("커스텀 상점 수: " + shops), false);
		return 1;
	}

	private static int adminDeleteShop(ServerCommandSource source, String name) {
		if (!requireDedicated(source)) {
			return 0;
		}
		if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
			// Console/command-block callers: perform deletion without an owner to receive returned stock.
			MinecraftServer server = source.getServer();
			Optional<CustomShop> found = CustomShopData.get(server).findByName(name);
			if (found.isEmpty()) {
				source.sendError(Text.translatable("lambdamc.shop.not_found", name));
				return 0;
			}
			CustomShopData.get(server).remove(found.get().getId());
			LambdaBank.get(server).log(source.getName() + "이(가) 상점 '" + name + "' 강제 삭제 (콘솔, 창고 내용물 소실)");
			source.sendFeedback(() -> Text.literal("'" + name + "' 상점을 삭제했습니다. (창고 내용물은 소실됩니다)"), true);
			return 1;
		}
		ShopManager.Result result = ShopManager.deleteShop(player, name, true);
		source.sendFeedback(() -> Text.literal(result.message()).formatted(result.success() ? Formatting.GREEN : Formatting.RED), true);
		return result.success() ? 1 : 0;
	}

	private static int adminLog(ServerCommandSource source, int count) {
		if (!requireDedicated(source)) {
			return 0;
		}
		LambdaBank bank = LambdaBank.get(source.getServer());
		List<String> lines = bank.getRecentLog(count);
		if (lines.isEmpty()) {
			source.sendFeedback(() -> Text.literal("거래 로그가 없습니다."), false);
			return 0;
		}
		source.sendFeedback(() -> Text.literal("=== 최근 거래 로그 (" + lines.size() + "건) ===").formatted(Formatting.GOLD), false);
		for (String line : lines) {
			source.sendFeedback(() -> Text.literal(line).formatted(Formatting.GRAY), false);
		}
		return lines.size();
	}

	// ---------------------------------------------------------------------
	// helpers
	// ---------------------------------------------------------------------

	private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestAllShops(
			com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx,
			com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
		List<String> names = new ArrayList<>();
		names.add(DefaultShop.NAME);
		CustomShopData.get(ctx.getSource().getServer()).all().forEach(s -> names.add(s.getName()));
		return CommandSource.suggestMatching(names, builder);
	}

	private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestOwnShops(
			com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx,
			com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
		List<String> names = new ArrayList<>();
		if (ctx.getSource().getEntity() instanceof ServerPlayerEntity player) {
			CustomShopData.get(ctx.getSource().getServer()).all().stream()
					.filter(s -> s.getOwner().equals(player.getUuid()))
					.forEach(s -> names.add(s.getName()));
		}
		return CommandSource.suggestMatching(names, builder);
	}

	private static String escape(String raw) {
		return raw.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private static String format(long value) {
		return NumberFormat.getIntegerInstance(Locale.US).format(value);
	}
}
