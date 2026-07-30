package com.edshin09.lambdamc.shop;

import net.minecraft.item.ItemStack;

/**
 * A single line in a shop: either something the player can buy from the
 * shop for λ, or something the shop will buy from the player for λ.
 * {@code rarity} and {@code oneTime} may be null.
 */
public record ShopOffer(
		ItemStack display,
		long price,
		OfferKind kind,
		Rarity rarity,
		OneTimeFlag oneTime
) {
	public enum OfferKind {
		/** Player pays λ, receives the item. */
		BUY,
		/** Player gives up the item, receives λ. */
		SELL
	}

	/** Tracks server-wide one-time-only trades/purchases. */
	public enum OneTimeFlag {
		DRAGON_EGG,
		RESET_CARD
	}

	public static ShopOffer buy(ItemStack display, long price) {
		return new ShopOffer(display, price, OfferKind.BUY, null, null);
	}

	public static ShopOffer buy(ItemStack display, long price, Rarity rarity) {
		return new ShopOffer(display, price, OfferKind.BUY, rarity, null);
	}

	public static ShopOffer buyOnce(ItemStack display, long price, OneTimeFlag flag) {
		return new ShopOffer(display, price, OfferKind.BUY, null, flag);
	}

	public static ShopOffer sell(ItemStack display, long price) {
		return new ShopOffer(display, price, OfferKind.SELL, null, null);
	}

	public static ShopOffer sellOnce(ItemStack display, long price, OneTimeFlag flag) {
		return new ShopOffer(display, price, OfferKind.SELL, null, flag);
	}
}
