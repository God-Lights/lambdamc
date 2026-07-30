package com.edshin09.lambdamc.shop;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * A single line rendered in a {@code TradeScreenHandler}. All business logic
 * (bank access, storage, fees, one-time flags) lives in {@link #action()},
 * so the same GUI code can drive the default shop and any custom shop.
 */
public record ShopOffer(ItemStack display, long price, OfferKind kind, Rarity rarity, TradeAction action) {
	public enum OfferKind {
		/** Player pays λ, receives the item. */
		BUY,
		/** Player gives up the item, receives λ. */
		SELL
	}

	@FunctionalInterface
	public interface TradeAction {
		TradeResult execute(ServerPlayerEntity player);
	}
}
