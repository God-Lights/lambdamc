package com.edshin09.lambdamc.shop;

/** What a custom shop does. */
public enum ShopType {
	/** Owner stocks items; other players pay λ to buy them. */
	SELLING("판매"),
	/** Owner pays λ (from their own balance) to buy items other players sell in. */
	BUYING("구매");

	public final String displayName;

	ShopType(String displayName) {
		this.displayName = displayName;
	}

	public static ShopType parse(String value) {
		return switch (value.toLowerCase()) {
			case "sell", "selling", "판매" -> SELLING;
			case "buy", "buying", "구매" -> BUYING;
			default -> null;
		};
	}
}
