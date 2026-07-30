package com.edshin09.lambdamc.config;

import com.edshin09.lambdamc.LambdaMC;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Admin-editable server config, stored at {@code config/lambdamc/config.json}.
 * Reloaded on server start; a bad/missing file falls back to defaults and
 * (re)writes a fresh file so admins have something to edit.
 */
public final class LambdaConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static LambdaConfig INSTANCE = new LambdaConfig();

	// --- economy basics ---
	public long startingBalance = 2000L;

	// --- default shop: exchange (player sells item -> shop for lambda) ---
	public java.util.Map<String, Long> exchangeRates = new java.util.LinkedHashMap<>(java.util.Map.of(
			"minecraft:diamond", 100L,
			"minecraft:ancient_debris", 500L,
			"minecraft:netherite_ingot", 2000L
	));
	/** Special one-server-wide-only exchange: item id -> price. */
	public java.util.Map<String, Long> oneTimeExchangeRates = new java.util.LinkedHashMap<>(java.util.Map.of(
			"minecraft:dragon_egg", 10_000L
	));

	// --- default shop: fixed offers ---
	public long edHeadPrice = 0L;
	public long resetCardPrice = 100_000_000L;

	// --- default shop: daily rotating stock ---
	public int dailyStockCount = 18;
	public int normalWeight = 60;
	public int epicWeight = 30;
	public int mysticWeight = 10;

	// --- custom shops ---
	/** Fraction of every custom-shop transaction kept by the server, e.g. 0.05 = 5%. */
	public double customShopFeeRate = 0.05;
	public long minListingPrice = 1L;
	public long maxListingPrice = 10_000_000L;
	/** Total custom shops (each may freely mix sell/buy listings) a single player may own. */
	public int maxShopsPerPlayer = 2;
	/** Cap per listing kind (sell vs buy) within one shop; the browse/manage GUI has 20 slots per side. */
	public int maxListingsPerShop = 20;

	public static void load() {
		Path path = configPath();
		try {
			if (Files.exists(path)) {
				try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
					LambdaConfig loaded = GSON.fromJson(reader, LambdaConfig.class);
					if (loaded != null) {
						INSTANCE = loaded;
					}
				}
			}
		} catch (IOException | RuntimeException e) {
			LambdaMC.LOGGER.warn("LambdaMC config load failed, using defaults", e);
			INSTANCE = new LambdaConfig();
		}
		save();
	}

	public static void save() {
		Path path = configPath();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(INSTANCE, writer);
			}
		} catch (IOException e) {
			LambdaMC.LOGGER.warn("LambdaMC config save failed", e);
		}
	}

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("lambdamc").resolve("config.json");
	}
}
