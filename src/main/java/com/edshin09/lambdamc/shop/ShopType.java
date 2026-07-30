package com.edshin09.lambdamc.shop;

/**
 * What a single {@link Listing} does. A shop is just a named container of
 * listings, so the same shop can freely mix both kinds.
 */
public enum ShopType {
	/** Owner stocks the item; other players pay λ to buy it. */
	SELLING("판매"),
	/** Owner pays λ (from their own balance) to buy the item other players sell in. */
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
