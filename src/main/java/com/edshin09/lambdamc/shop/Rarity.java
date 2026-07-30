package com.edshin09.lambdamc.shop;

import net.minecraft.util.Formatting;

/** Rarity tiers for the default shop's daily rotating stock. */
public enum Rarity {
	NORMAL("노말", Formatting.WHITE),
	EPIC("에픽", Formatting.LIGHT_PURPLE),
	MYSTIC("미스틱", Formatting.AQUA);

	public final String displayName;
	public final Formatting color;

	Rarity(String displayName, Formatting color) {
		this.displayName = displayName;
		this.color = color;
	}
}
