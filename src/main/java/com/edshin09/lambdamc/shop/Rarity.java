package com.edshin09.lambdamc.shop;

import net.minecraft.util.Formatting;

/** Rarity tiers for the daily rotating shop stock. */
public enum Rarity {
	NORMAL("노말", Formatting.WHITE, 60),
	EPIC("에픽", Formatting.LIGHT_PURPLE, 30),
	MYSTIC("미스틱", Formatting.AQUA, 10);

	public final String displayName;
	public final Formatting color;
	/** Relative weight used when rolling today's rotating stock. */
	public final int weight;

	Rarity(String displayName, Formatting color, int weight) {
		this.displayName = displayName;
		this.color = color;
		this.weight = weight;
	}
}
