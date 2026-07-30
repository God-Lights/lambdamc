package com.edshin09.lambdamc.shop;

import com.edshin09.lambdamc.economy.LambdaBank;
import com.edshin09.lambdamc.item.LambdaItems;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * The default shop, "lambdasharp":
 * <ul>
 *   <li>Row 0: exchange - player sells raw materials to the shop for λ.</li>
 *   <li>Row 1: fixed offers - Ed's head (free) and the troll reset card.</li>
 *   <li>Rows 2-4: daily rotating stock (normal/epic/mystic), re-rolled once per real-world day.</li>
 * </ul>
 */
public final class LambdaSharpShop implements Shop {
	public static final String ID = "lambdasharp";
	private static final int ROWS = 5;
	private static final int DAILY_STOCK_COUNT = 18;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDisplayName() {
		return "LambdaSharp";
	}

	@Override
	public int getRows() {
		return ROWS;
	}

	@Override
	public List<ShopOffer> buildOffers(LambdaBank bank) {
		ShopOffer[] offers = new ShopOffer[ROWS * 9];

		offers[0] = ShopOffer.sell(new ItemStack(Items.DIAMOND), 100);
		offers[1] = ShopOffer.sell(new ItemStack(Items.ANCIENT_DEBRIS), 500);
		offers[2] = ShopOffer.sell(new ItemStack(Items.NETHERITE_INGOT), 2000);
		offers[3] = bank.isDragonEggTraded()
				? ShopOffer.sellOnce(soldOutIcon("드래곤 알 (이미 거래됨)"), 10_000, ShopOffer.OneTimeFlag.DRAGON_EGG)
				: ShopOffer.sellOnce(new ItemStack(Items.DRAGON_EGG), 10_000, ShopOffer.OneTimeFlag.DRAGON_EGG);

		offers[9] = ShopOffer.buy(edHead(), 0);
		offers[10] = bank.isResetCardPurchased()
				? ShopOffer.buyOnce(soldOutIcon("서버 초기화 카드 (품절)"), 100_000_000, ShopOffer.OneTimeFlag.RESET_CARD)
				: ShopOffer.buyOnce(new ItemStack(LambdaItems.RESET_CARD), 100_000_000, ShopOffer.OneTimeFlag.RESET_CARD);

		int index = 18;
		for (ShopOffer offer : rollDailyStock(LocalDate.now(), DAILY_STOCK_COUNT)) {
			offers[index++] = offer;
		}

		return Arrays.asList(offers);
	}

	private static ItemStack edHead() {
		ItemStack head = new ItemStack(Items.PLAYER_HEAD);
		head.getOrCreateNbt().putString("SkullOwner", "EdShin09");
		head.setCustomName(Text.literal("에드(EdShin09)의 머리").formatted(Formatting.LIGHT_PURPLE));
		return LambdaItems.markNoPlace(head);
	}

	private static ItemStack soldOutIcon(String name) {
		ItemStack stack = new ItemStack(Items.BARRIER);
		stack.setCustomName(Text.literal(name).formatted(Formatting.RED));
		return stack;
	}

	private static List<ShopOffer> rollDailyStock(LocalDate date, int count) {
		long seed = date.toEpochDay() * 1_000_003L + ID.hashCode();
		Random random = new Random(seed);
		List<ShopOffer> result = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			Rarity rarity = rollRarity(random);
			List<ShopItems.PricedEntry> pool = ShopItems.pool(rarity);
			ShopItems.PricedEntry entry = pool.get(random.nextInt(pool.size()));
			result.add(ShopOffer.buy(entry.fresh(), entry.price(), rarity));
		}
		return result;
	}

	private static Rarity rollRarity(Random random) {
		int totalWeight = Rarity.NORMAL.weight + Rarity.EPIC.weight + Rarity.MYSTIC.weight;
		int roll = random.nextInt(totalWeight);
		int cumulative = 0;
		for (Rarity rarity : Rarity.values()) {
			cumulative += rarity.weight;
			if (roll < cumulative) {
				return rarity;
			}
		}
		return Rarity.NORMAL;
	}
}
