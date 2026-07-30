package com.edshin09.lambdamc.shop;

import com.edshin09.lambdamc.config.LambdaConfig;
import com.edshin09.lambdamc.economy.LambdaBank;
import com.edshin09.lambdamc.network.LambdaNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Business logic for creating, managing, and trading through custom shops. */
public final class ShopManager {
	/** Grid layout shared by the browse screen and the management screen: sell listings on the left, buy on the right. */
	public static final int ROWS = 5;
	public static final int COLS = 9;
	public static final int SLOT_COUNT = ROWS * COLS;
	public static final int SIDE_COLS = 4;
	public static final int DIVIDER_COL = 4;
	public static final int SIDE_CAPACITY = ROWS * SIDE_COLS;

	public record Result(boolean success, String message) {
		public static Result ok(String message) {
			return new Result(true, message);
		}

		public static Result fail(String message) {
			return new Result(false, message);
		}
	}

	private ShopManager() {
	}

	/** Positions listings into the fixed 45-slot grid: SELLING listings left (cols 0-3), BUYING right (cols 5-8), col 4 empty. */
	public static Listing[] layoutListings(List<Listing> listings) {
		Listing[] grid = new Listing[SLOT_COUNT];
		int sellIndex = 0;
		int buyIndex = 0;
		for (Listing listing : listings) {
			if (listing.getKind() == ShopType.SELLING) {
				if (sellIndex >= SIDE_CAPACITY) {
					continue;
				}
				int row = sellIndex / SIDE_COLS;
				int col = sellIndex % SIDE_COLS;
				grid[row * COLS + col] = listing;
				sellIndex++;
			} else {
				if (buyIndex >= SIDE_CAPACITY) {
					continue;
				}
				int row = buyIndex / SIDE_COLS;
				int col = DIVIDER_COL + 1 + (buyIndex % SIDE_COLS);
				grid[row * COLS + col] = listing;
				buyIndex++;
			}
		}
		return grid;
	}

	public static Result createShop(ServerPlayerEntity player, String name) {
		String trimmed = name.trim();
		if (trimmed.isEmpty() || trimmed.length() > 24) {
			return Result.fail("상점 이름은 1~24자여야 합니다.");
		}

		MinecraftServer server = player.getServer();
		CustomShopData data = CustomShopData.get(server);
		if (data.nameTaken(trimmed)) {
			return Result.fail("이미 사용 중인 상점 이름입니다: " + trimmed);
		}

		LambdaConfig config = LambdaConfig.INSTANCE;
		if (data.countByOwner(player.getUuid()) >= config.maxShopsPerPlayer) {
			return Result.fail("이미 상점을 " + config.maxShopsPerPlayer + "개 만들었습니다.");
		}

		CustomShop shop = new CustomShop(UUID.randomUUID(), trimmed, player.getUuid(),
				player.getGameProfile().getName(), System.currentTimeMillis());
		data.add(shop);
		LambdaBank.get(server).log(player.getGameProfile().getName() + "이(가) 상점 '" + trimmed + "' 생성");
		return Result.ok("'" + trimmed + "' 상점을 만들었습니다. (상품 추가로 판매/구매 상품을 등록하세요)");
	}

	public static Result deleteShop(ServerPlayerEntity player, String name, boolean adminOverride) {
		MinecraftServer server = player.getServer();
		CustomShopData data = CustomShopData.get(server);
		Optional<CustomShop> found = data.findByName(name);
		if (found.isEmpty()) {
			return Result.fail("상점을 찾을 수 없습니다: " + name);
		}
		CustomShop shop = found.get();
		if (!adminOverride && !shop.getOwner().equals(player.getUuid())) {
			return Result.fail("본인의 상점만 삭제할 수 있습니다.");
		}

		if (player.getUuid().equals(shop.getOwner())) {
			returnAllStorage(player, shop);
		}

		data.remove(shop.getId());
		LambdaBank.get(server).log(player.getGameProfile().getName() + "이(가) 상점 '" + shop.getName() + "' 삭제"
				+ (adminOverride ? " (관리자)" : ""));
		return Result.ok("'" + shop.getName() + "' 상점을 삭제했습니다.");
	}

	private static void returnAllStorage(ServerPlayerEntity player, CustomShop shop) {
		var storage = shop.getStorage();
		for (int i = 0; i < storage.size(); i++) {
			ItemStack stack = storage.getStack(i);
			if (stack.isEmpty()) {
				continue;
			}
			if (!player.getInventory().insertStack(stack)) {
				player.dropItem(stack, false);
			}
			storage.setStack(i, ItemStack.EMPTY);
		}
	}

	public static Result addProduct(ServerPlayerEntity player, String shopName, ShopType kind, long price) {
		if (kind == null) {
			return Result.fail("판매(sell) 또는 구매(buy) 중 하나를 선택하세요.");
		}
		MinecraftServer server = player.getServer();
		CustomShopData data = CustomShopData.get(server);
		Optional<CustomShop> found = data.findByName(shopName);
		if (found.isEmpty()) {
			return Result.fail("상점을 찾을 수 없습니다: " + shopName);
		}
		CustomShop shop = found.get();
		if (!shop.getOwner().equals(player.getUuid())) {
			return Result.fail("본인의 상점에만 상품을 추가할 수 있습니다.");
		}

		LambdaConfig config = LambdaConfig.INSTANCE;
		if (price < config.minListingPrice || price > config.maxListingPrice) {
			return Result.fail("가격은 " + format(config.minListingPrice) + " ~ " + format(config.maxListingPrice) + " λ 사이여야 합니다.");
		}

		ItemStack hand = player.getMainHandStack();
		if (hand.isEmpty()) {
			return Result.fail("추가할 아이템을 손(주손)에 들고 사용하세요.");
		}

		Optional<Listing> existing = shop.findListing(hand, kind);
		String itemName = hand.getName().getString();

		if (existing.isPresent()) {
			existing.get().setPrice(price);
		} else {
			if (shop.countListingsOfKind(kind) >= config.maxListingsPerShop) {
				return Result.fail(kind.displayName + " 상품은 최대 " + config.maxListingsPerShop + "개까지 등록할 수 있습니다.");
			}
			shop.getListings().add(new Listing(UUID.randomUUID(), hand, price, kind));
		}

		if (kind == ShopType.SELLING) {
			ItemStack deposit = hand.copy();
			player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, ItemStack.EMPTY);
			ItemStack leftover = shop.addStock(deposit);
			if (!leftover.isEmpty() && !player.getInventory().insertStack(leftover)) {
				player.dropItem(leftover, false);
			}
		}

		data.touch();
		LambdaBank.get(server).log(player.getGameProfile().getName() + "이(가) '" + shop.getName() + "'에 "
				+ itemName + " " + kind.displayName + " 상품 등록/수정 (" + price + "λ)");
		return Result.ok(itemName + " (" + kind.displayName + ") 상품을 " + format(price) + " λ 가격으로 등록했습니다.");
	}

	/** Updates an existing listing's price without needing to hold the item again (used by the management GUI). */
	public static Result setPrice(ServerPlayerEntity player, String shopName, int index, long price) {
		MinecraftServer server = player.getServer();
		CustomShopData data = CustomShopData.get(server);
		Optional<CustomShop> found = data.findByName(shopName);
		if (found.isEmpty()) {
			return Result.fail("상점을 찾을 수 없습니다: " + shopName);
		}
		CustomShop shop = found.get();
		if (!shop.getOwner().equals(player.getUuid())) {
			return Result.fail("본인의 상점에서만 가격을 수정할 수 있습니다.");
		}
		LambdaConfig config = LambdaConfig.INSTANCE;
		if (price < config.minListingPrice || price > config.maxListingPrice) {
			return Result.fail("가격은 " + format(config.minListingPrice) + " ~ " + format(config.maxListingPrice) + " λ 사이여야 합니다.");
		}
		if (index < 1 || index > shop.getListings().size()) {
			return Result.fail("잘못된 상품 번호입니다. (1~" + shop.getListings().size() + ")");
		}

		Listing listing = shop.getListings().get(index - 1);
		listing.setPrice(price);
		data.touch();
		LambdaBank.get(server).log(player.getGameProfile().getName() + "이(가) '" + shop.getName() + "'의 "
				+ listing.getTemplate().getName().getString() + " 가격을 " + price + "λ로 변경");
		return Result.ok(listing.getTemplate().getName().getString() + " 가격을 " + format(price) + " λ로 변경했습니다.");
	}

	public static Result removeProduct(ServerPlayerEntity player, String shopName, int index) {
		MinecraftServer server = player.getServer();
		CustomShopData data = CustomShopData.get(server);
		Optional<CustomShop> found = data.findByName(shopName);
		if (found.isEmpty()) {
			return Result.fail("상점을 찾을 수 없습니다: " + shopName);
		}
		CustomShop shop = found.get();
		if (!shop.getOwner().equals(player.getUuid())) {
			return Result.fail("본인의 상점에서만 상품을 삭제할 수 있습니다.");
		}
		if (index < 1 || index > shop.getListings().size()) {
			return Result.fail("잘못된 상품 번호입니다. (1~" + shop.getListings().size() + ")");
		}

		Listing listing = shop.getListings().remove(index - 1);
		if (listing.getKind() == ShopType.SELLING) {
			int stock = shop.countStock(listing.getTemplate());
			if (stock > 0) {
				shop.takeStock(listing.getTemplate(), stock);
				ItemStack refund = listing.getTemplate().copy();
				refund.setCount(stock);
				if (!player.getInventory().insertStack(refund)) {
					player.dropItem(refund, false);
				}
			}
		}

		data.touch();
		LambdaBank.get(server).log(player.getGameProfile().getName() + "이(가) '" + shop.getName() + "'에서 "
				+ listing.getTemplate().getName().getString() + " 상품 삭제");
		return Result.ok(listing.getTemplate().getName().getString() + " 상품을 삭제했습니다.");
	}

	public static boolean canOpenWarehouse(ServerPlayerEntity player, CustomShop shop) {
		return shop.getOwner().equals(player.getUuid());
	}

	/** Builds the browsable trade offers for a custom shop (visiting players and the owner alike), positioned in the shared grid. */
	public static List<ShopOffer> buildOffers(MinecraftServer server, CustomShop shop) {
		Listing[] grid = layoutListings(shop.getListings());
		ShopOffer[] offers = new ShopOffer[SLOT_COUNT];
		for (int i = 0; i < SLOT_COUNT; i++) {
			Listing listing = grid[i];
			if (listing == null) {
				continue;
			}
			offers[i] = listing.getKind() == ShopType.SELLING
					? buildSellingOffer(server, shop, listing)
					: buildBuyingOffer(server, shop, listing);
		}
		return Arrays.asList(offers);
	}

	private static ShopOffer buildSellingOffer(MinecraftServer server, CustomShop shop, Listing listing) {
		int stock = shop.countStock(listing.getTemplate());
		ItemStack display = decorate(listing, stock);

		return new ShopOffer(display, listing.getPrice(), ShopOffer.OfferKind.BUY, null, player -> {
			int currentStock = shop.countStock(listing.getTemplate());
			if (currentStock <= 0) {
				return TradeResult.fail(Text.literal("품절입니다."));
			}
			LambdaBank bank = LambdaBank.get(server);
			long price = listing.getPrice();
			long balance = bank.getBalance(player.getUuid());
			if (balance < price) {
				return TradeResult.fail(Text.translatable("lambdamc.shop.buy.not_enough_money", format(price), format(balance)));
			}

			bank.subtract(player.getUuid(), price);
			shop.takeStock(listing.getTemplate(), 1);
			ItemStack give = listing.getTemplate().copy();
			give.setCount(1);
			if (!player.getInventory().insertStack(give)) {
				player.dropItem(give, false);
			}

			long fee = Math.round(price * LambdaConfig.INSTANCE.customShopFeeRate);
			long payout = Math.max(0, price - fee);
			bank.add(shop.getOwner(), shop.getOwnerName(), payout);
			shop.incrementSales();
			CustomShopData.get(server).touch();

			String itemName = give.getName().getString();
			bank.log(player.getGameProfile().getName() + "이(가) '" + shop.getName() + "'에서 " + itemName + "을(를) " + price + "λ에 구매 (지급 " + payout + "λ)");
			notifyOwner(server, shop, itemName + " 이(가) " + format(payout) + " λ에 팔렸습니다. (상점: " + shop.getName() + ")");
			LambdaNetworking.sendBalance(player, bank.getBalance(player.getUuid()));
			return TradeResult.ok(Text.translatable("lambdamc.shop.buy.success", give.getName(), format(price)));
		});
	}

	private static ShopOffer buildBuyingOffer(MinecraftServer server, CustomShop shop, Listing listing) {
		ItemStack display = decorate(listing, -1);

		return new ShopOffer(display, listing.getPrice(), ShopOffer.OfferKind.SELL, null, player -> {
			LambdaBank bank = LambdaBank.get(server);
			long price = listing.getPrice();

			if (bank.getBalance(shop.getOwner()) < price) {
				return TradeResult.fail(Text.literal("상점 주인의 잔액이 부족합니다."));
			}
			if (!hasMatching(player, listing.getTemplate())) {
				return TradeResult.fail(Text.translatable("lambdamc.shop.sell.need_item", listing.getTemplate().getName()));
			}

			ItemStack toStore = listing.getTemplate().copy();
			toStore.setCount(1);
			ItemStack leftover = shop.addStock(toStore);
			if (!leftover.isEmpty()) {
				return TradeResult.fail(Text.literal("상점 창고가 가득 찼습니다."));
			}

			removeOneMatching(player, listing.getTemplate());
			bank.subtract(shop.getOwner(), price);
			long fee = Math.round(price * LambdaConfig.INSTANCE.customShopFeeRate);
			long payout = Math.max(0, price - fee);
			bank.add(player.getUuid(), player.getGameProfile().getName(), payout);
			shop.incrementSales();
			CustomShopData.get(server).touch();

			String itemName = listing.getTemplate().getName().getString();
			bank.log(player.getGameProfile().getName() + "이(가) '" + shop.getName() + "'에 " + itemName + "을(를) " + price + "λ에 판매 (지급 " + payout + "λ)");
			notifyOwner(server, shop, itemName + " 을(를) " + format(price) + " λ에 매입했습니다. (구매자: " + player.getGameProfile().getName() + ")");
			LambdaNetworking.sendBalance(player, bank.getBalance(player.getUuid()));

			ServerPlayerEntity ownerOnline = server.getPlayerManager().getPlayer(shop.getOwner());
			if (ownerOnline != null) {
				LambdaNetworking.sendBalance(ownerOnline, bank.getBalance(shop.getOwner()));
			}

			return TradeResult.ok(Text.translatable("lambdamc.shop.sell.success", listing.getTemplate().getName(), format(payout)));
		});
	}

	private static void notifyOwner(MinecraftServer server, CustomShop shop, String message) {
		ServerPlayerEntity owner = server.getPlayerManager().getPlayer(shop.getOwner());
		if (owner != null) {
			owner.sendMessage(Text.literal("[" + shop.getName() + "] " + message).formatted(Formatting.YELLOW), false);
		} else {
			LambdaBank.get(server).notify(shop.getOwner(), "[" + shop.getName() + "] " + message);
		}
	}

	private static boolean hasMatching(PlayerEntity player, ItemStack template) {
		PlayerInventory inventory = player.getInventory();
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (!stack.isEmpty() && Listing.sameItem(stack, template)) {
				return true;
			}
		}
		return false;
	}

	private static void removeOneMatching(PlayerEntity player, ItemStack template) {
		PlayerInventory inventory = player.getInventory();
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (!stack.isEmpty() && Listing.sameItem(stack, template)) {
				stack.decrement(1);
				return;
			}
		}
	}

	/** Decorates a listing's icon with price/stock/kind lore. {@code stock} < 0 means "not applicable" (buy listings). */
	static ItemStack decorate(Listing listing, int stock) {
		ItemStack template = listing.getTemplate();
		long price = listing.getPrice();
		ShopType kind = listing.getKind();

		if (stock == 0) {
			ItemStack soldOut = new ItemStack(Items.BARRIER);
			soldOut.setCustomName(Text.literal("품절 / SOLD OUT").formatted(Formatting.RED));
			return soldOut;
		}

		ItemStack stack = template.copy();
		net.minecraft.nbt.NbtCompound display = stack.getOrCreateSubNbt("display");
		net.minecraft.nbt.NbtList lore = new net.minecraft.nbt.NbtList();

		String kindTag = kind == ShopType.SELLING ? "§a[판매]" : "§6[구매]";
		lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(Text.literal(kindTag))));

		String priceLine = kind == ShopType.SELLING
				? "§7가격: §e" + format(price) + " λ"
				: "§7매입가: §e" + format(price) + " λ";
		lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(Text.literal(priceLine))));

		if (stock >= 0) {
			lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(Text.literal("§7재고: §f" + stock))));
		}

		display.put("Lore", lore);
		return stack;
	}

	static String format(long value) {
		return NumberFormat.getIntegerInstance(Locale.US).format(value);
	}
}
