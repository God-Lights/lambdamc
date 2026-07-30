package com.edshin09.lambdamc.client.screen;

import com.edshin09.lambdamc.shop.ShopSummary;

import java.util.List;

/** Client-side cache of the last {@code /lambdamc shop list} response, used to (re)populate {@link ShopMenuScreen}. */
public final class LambdaShopListCache {
	private static volatile List<ShopSummary> shops = List.of();
	private static Runnable listener;

	private LambdaShopListCache() {
	}

	public static List<ShopSummary> get() {
		return shops;
	}

	public static void set(List<ShopSummary> newShops) {
		shops = newShops;
		if (listener != null) {
			listener.run();
		}
	}

	/** Only one screen can listen at a time (the currently open shop menu, if any). */
	public static void setListener(Runnable runnable) {
		listener = runnable;
	}
}
