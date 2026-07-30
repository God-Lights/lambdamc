package com.edshin09.lambdamc.shop;

import net.minecraft.text.Text;

/** Outcome of executing a {@link ShopOffer}'s trade action. */
public record TradeResult(boolean success, Text message) {
	public static TradeResult ok(Text message) {
		return new TradeResult(true, message);
	}

	public static TradeResult fail(Text message) {
		return new TradeResult(false, message);
	}
}
