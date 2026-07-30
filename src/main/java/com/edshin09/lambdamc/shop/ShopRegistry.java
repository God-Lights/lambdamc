package com.edshin09.lambdamc.shop;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ShopRegistry {
	private static final Map<String, Shop> SHOPS = new LinkedHashMap<>();

	private ShopRegistry() {
	}

	public static void register(Shop shop) {
		SHOPS.put(shop.getId().toLowerCase(), shop);
	}

	public static Optional<Shop> get(String id) {
		return Optional.ofNullable(SHOPS.get(id.toLowerCase()));
	}

	public static Map<String, Shop> getAll() {
		return SHOPS;
	}

	public static void bootstrap() {
		register(new LambdaSharpShop());
	}
}
