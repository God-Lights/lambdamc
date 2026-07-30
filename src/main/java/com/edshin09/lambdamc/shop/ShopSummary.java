package com.edshin09.lambdamc.shop;

import java.util.UUID;

/** Lightweight, network-friendly summary of a custom shop for the shop-list menu. */
public record ShopSummary(UUID id, String name, String ownerName, ShopType type, int listingCount, int totalSales, long createdAt) {
	public static ShopSummary of(CustomShop shop) {
		return new ShopSummary(shop.getId(), shop.getName(), shop.getOwnerName(), shop.getType(),
				shop.getListings().size(), shop.getTotalSales(), shop.getCreatedAt());
	}
}
