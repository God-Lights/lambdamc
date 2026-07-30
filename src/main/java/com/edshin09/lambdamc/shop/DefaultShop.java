package com.edshin09.lambdamc.shop;

import com.edshin09.lambdamc.config.LambdaConfig;
import com.edshin09.lambdamc.economy.LambdaBank;
import com.edshin09.lambdamc.item.LambdaItems;
import com.edshin09.lambdamc.network.LambdaNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.BooleanSupplier;

/**
 * The built-in "lambdasharp" shop: exchange (row 0), fixed offers (row 1),
 * and daily rotating stock (rows 2-4). All prices/weights come from
 * {@link LambdaConfig} so admins can tune it without recompiling.
 */
public final class DefaultShop {
	public static final String NAME = "lambdasharp";
	public static final int ROWS = 5;

	private DefaultShop() {
	}

	public static List<ShopOffer> buildOffers(MinecraftServer server) {
		LambdaConfig config = LambdaConfig.INSTANCE;
		ShopOffer[] offers = new ShopOffer[ROWS * 9];

		int col = 0;
		for (var entry : config.exchangeRates.entrySet()) {
			if (col >= 9) {
				break;
			}
			Item item = resolveItem(entry.getKey());
			if (item != null) {
				offers[col++] = sellOffer(new ItemStack(item), entry.getValue(), null, null);
			}
		}
		for (var entry : config.oneTimeExchangeRates.entrySet()) {
			if (col >= 9) {
				break;
			}
			Item item = resolveItem(entry.getKey());
			if (item != null) {
				LambdaBank bank = LambdaBank.get(server);
				offers[col++] = sellOffer(new ItemStack(item), entry.getValue(),
						bank::isDragonEggTraded, bank::markDragonEggTraded);
			}
		}

		offers[9] = buyOffer(edHead(), config.edHeadPrice, null, null);
		LambdaBank bank = LambdaBank.get(server);
		offers[10] = buyOffer(new ItemStack(LambdaItems.RESET_CARD), config.resetCardPrice,
				bank::isResetCardPurchased, bank::markResetCardPurchased);

		int index = 18;
		for (ShopOffer offer : rollDailyStock(LocalDate.now(), config.dailyStockCount)) {
			if (index >= offers.length) {
				break;
			}
			offers[index++] = offer;
		}

		return Arrays.asList(offers);
	}

	private static Item resolveItem(String id) {
		try {
			Item item = Registries.ITEM.get(new Identifier(id));
			return item == Items.AIR ? null : item;
		} catch (Exception e) {
			return null;
		}
	}

	private static ItemStack edHead() {
		ItemStack head = new ItemStack(Items.PLAYER_HEAD);
		head.getOrCreateNbt().putString("SkullOwner", "EdShin09");
		head.setCustomName(Text.literal("에드(EdShin09)의 머리").formatted(Formatting.LIGHT_PURPLE));
		return LambdaItems.markNoPlace(head);
	}

	private static List<ShopOffer> rollDailyStock(LocalDate date, int count) {
		LambdaConfig config = LambdaConfig.INSTANCE;
		long seed = date.toEpochDay() * 1_000_003L + NAME.hashCode();
		Random random = new Random(seed);
		List<ShopOffer> result = new ArrayList<>(count);
		int totalWeight = Math.max(1, config.normalWeight) + Math.max(1, config.epicWeight) + Math.max(1, config.mysticWeight);
		for (int i = 0; i < count; i++) {
			Rarity rarity = rollRarity(random, totalWeight, config);
			List<ShopItems.PricedEntry> pool = ShopItems.pool(rarity);
			ShopItems.PricedEntry entry = pool.get(random.nextInt(pool.size()));
			result.add(buyOffer(entry.fresh(), entry.price(), rarity, null, null));
		}
		return result;
	}

	private static Rarity rollRarity(Random random, int totalWeight, LambdaConfig config) {
		int roll = random.nextInt(totalWeight);
		int cumulative = Math.max(1, config.normalWeight);
		if (roll < cumulative) {
			return Rarity.NORMAL;
		}
		cumulative += Math.max(1, config.epicWeight);
		if (roll < cumulative) {
			return Rarity.EPIC;
		}
		return Rarity.MYSTIC;
	}

	// --- generic buy/sell offer builders, shared by fixed + rotating entries ---

	private static ShopOffer buyOffer(ItemStack display, long price, BooleanSupplier soldOut, Runnable markSold) {
		return buyOffer(display, price, null, soldOut, markSold);
	}

	private static ShopOffer buyOffer(ItemStack display, long price, Rarity rarity, BooleanSupplier soldOut, Runnable markSold) {
		boolean isSoldOut = soldOut != null && soldOut.getAsBoolean();
		return new ShopOffer(decorate(display, price, ShopOffer.OfferKind.BUY, rarity, isSoldOut), price,
				ShopOffer.OfferKind.BUY, rarity, player -> {
			MinecraftServer server = player.getServer();
			LambdaBank bank = LambdaBank.get(server);
			if (soldOut != null && soldOut.getAsBoolean()) {
				return TradeResult.fail(Text.translatable("lambdamc.shop.buy.sold_out"));
			}
			long balance = bank.getBalance(player.getUuid());
			if (balance < price) {
				return TradeResult.fail(Text.translatable("lambdamc.shop.buy.not_enough_money", format(price), format(balance)));
			}
			bank.subtract(player.getUuid(), price);
			if (markSold != null) {
				markSold.run();
			}
			ItemStack give = display.copy();
			give.setCount(1);
			Text itemName = give.getName();
			if (!player.getInventory().insertStack(give)) {
				player.dropItem(give, false);
			}
			bank.log(player.getGameProfile().getName() + "이(가) 기본 상점에서 " + itemName.getString() + "을(를) " + price + "λ에 구매");
			LambdaNetworking.sendBalance(player, bank.getBalance(player.getUuid()));
			return TradeResult.ok(Text.translatable("lambdamc.shop.buy.success", itemName, format(price)));
		});
	}

	private static ShopOffer sellOffer(ItemStack display, long price, BooleanSupplier soldOut, Runnable markSold) {
		boolean isSoldOut = soldOut != null && soldOut.getAsBoolean();
		return new ShopOffer(decorate(display, price, ShopOffer.OfferKind.SELL, null, isSoldOut), price,
				ShopOffer.OfferKind.SELL, null, player -> {
			MinecraftServer server = player.getServer();
			LambdaBank bank = LambdaBank.get(server);
			if (soldOut != null && soldOut.getAsBoolean()) {
				return TradeResult.fail(Text.translatable("lambdamc.shop.sell.sold_out"));
			}
			Text itemName = display.getName();
			if (!removeOneMatching(player, display.getItem())) {
				return TradeResult.fail(Text.translatable("lambdamc.shop.sell.need_item", itemName));
			}
			if (markSold != null) {
				markSold.run();
			}
			bank.add(player.getUuid(), player.getGameProfile().getName(), price);
			bank.log(player.getGameProfile().getName() + "이(가) 기본 상점에 " + itemName.getString() + "을(를) " + price + "λ에 판매");
			LambdaNetworking.sendBalance(player, bank.getBalance(player.getUuid()));
			return TradeResult.ok(Text.translatable("lambdamc.shop.sell.success", itemName, format(price)));
		});
	}

	private static boolean removeOneMatching(ServerPlayerEntity player, Item item) {
		var inventory = player.getInventory();
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (!stack.isEmpty() && stack.getItem() == item) {
				stack.decrement(1);
				return true;
			}
		}
		return false;
	}

	private static ItemStack decorate(ItemStack template, long price, ShopOffer.OfferKind kind, Rarity rarity, boolean soldOut) {
		ItemStack stack = template.copy();
		if (soldOut) {
			stack = new ItemStack(Items.BARRIER);
			stack.setCustomName(Text.literal("품절 / SOLD OUT").formatted(Formatting.RED));
			return stack;
		}
		net.minecraft.nbt.NbtCompound display = stack.getOrCreateSubNbt("display");
		net.minecraft.nbt.NbtList lore = new net.minecraft.nbt.NbtList();

		String priceLine = kind == ShopOffer.OfferKind.BUY
				? "§7가격: §e" + format(price) + " λ"
				: "§7판매가: §e" + format(price) + " λ (교환)";
		lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(Text.literal(priceLine))));

		if (rarity != null) {
			Text rarityText = Text.literal(rarity.displayName).formatted(rarity.color);
			lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(rarityText)));
		}

		display.put("Lore", lore);
		return stack;
	}

	private static String format(long value) {
		return NumberFormat.getIntegerInstance(Locale.US).format(value);
	}
}
