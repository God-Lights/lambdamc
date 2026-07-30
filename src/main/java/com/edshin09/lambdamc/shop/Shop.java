package com.edshin09.lambdamc.shop;

import com.edshin09.lambdamc.economy.LambdaBank;

import java.util.List;

/** A single shop that can be entered via {@code /lambdamc shop <name>}. */
public interface Shop {
	String getId();

	String getDisplayName();

	/** Number of 9-wide inventory rows the shop GUI should have. */
	int getRows();

	/**
	 * Builds the current list of offers for this shop, sized exactly
	 * {@code getRows() * 9}. Entries may be {@code null} for empty/filler
	 * slots. Recomputed every time the shop is opened so daily rotation and
	 * one-time-purchase state are always current.
	 */
	List<ShopOffer> buildOffers(LambdaBank bank);
}
