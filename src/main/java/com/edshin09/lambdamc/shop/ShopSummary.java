package com.edshin09.lambdamc.shop;

import java.util.UUID;

/** Lightweight, network-friendly summary of a custom shop for the shop-list menu. */
public record ShopSummary(UUID id, String name, String ownerName, int sellCount, int buyCount, int totalSales, long createdAt) {
	public static ShopSummary of(CustomShop shop) {
		int sellCount = (int) shop.countListingsOfKind(ShopType.SELLING);
		int buyCount = (int) shop.countListingsOfKind(ShopType.BUYING);
		return new ShopSummary(shop.getId(), shop.getName(), shop.getOwnerName(), sellCount, buyCount,
				shop.getTotalSales(), shop.getCreatedAt());
	}

	public int listingCount() {
		return sellCount + buyCount;
	}
}
