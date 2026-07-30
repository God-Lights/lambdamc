package com.edshin09.lambdamc.shop;

import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

/** Static item/price pools used to roll the default shop's daily rotating stock. */
public final class ShopItems {
	public record PricedEntry(ItemStack template, long price) {
		public ItemStack fresh() {
			return template.copy();
		}
	}

	private ShopItems() {
	}

	private static PricedEntry item(net.minecraft.item.Item item, long price) {
		return new PricedEntry(new ItemStack(item), price);
	}

	private static PricedEntry book(net.minecraft.enchantment.Enchantment enchantment, int level, long price) {
		return new PricedEntry(EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(enchantment, level)), price);
	}

	private static PricedEntry enchanted(net.minecraft.item.Item item, net.minecraft.enchantment.Enchantment enchantment, int level, long price) {
		ItemStack stack = new ItemStack(item);
		stack.addEnchantment(enchantment, level);
		return new PricedEntry(stack, price);
	}

	/** 기본적인 무기/도구, 약한 마법 부여 책, 다이아몬드·네더라이트를 제외한 금속류. */
	public static final List<PricedEntry> NORMAL = List.of(
			item(Items.WOODEN_SWORD, 20),
			item(Items.STONE_SWORD, 40),
			item(Items.IRON_SWORD, 120),
			item(Items.GOLDEN_SWORD, 90),
			item(Items.WOODEN_AXE, 20),
			item(Items.STONE_AXE, 40),
			item(Items.IRON_AXE, 120),
			item(Items.WOODEN_PICKAXE, 20),
			item(Items.STONE_PICKAXE, 40),
			item(Items.IRON_PICKAXE, 120),
			item(Items.WOODEN_SHOVEL, 15),
			item(Items.STONE_SHOVEL, 30),
			item(Items.IRON_SHOVEL, 90),
			item(Items.WOODEN_HOE, 15),
			item(Items.STONE_HOE, 30),
			item(Items.BOW, 100),
			item(Items.ARROW, 5),
			item(Items.IRON_INGOT, 30),
			item(Items.GOLD_INGOT, 35),
			item(Items.COPPER_INGOT, 15),
			item(Items.RAW_IRON, 20),
			item(Items.RAW_GOLD, 25),
			item(Items.RAW_COPPER, 10),
			book(Enchantments.SHARPNESS, 1, 80),
			book(Enchantments.PROTECTION, 1, 80),
			book(Enchantments.EFFICIENCY, 1, 70),
			book(Enchantments.UNBREAKING, 1, 70),
			book(Enchantments.POWER, 1, 80),
			book(Enchantments.FEATHER_FALLING, 1, 70),
			book(Enchantments.FIRE_ASPECT, 1, 90)
	);

	/** 좀 더 강한 무기/도구. */
	public static final List<PricedEntry> EPIC = List.of(
			item(Items.DIAMOND_SWORD, 600),
			item(Items.DIAMOND_AXE, 600),
			item(Items.DIAMOND_PICKAXE, 550),
			item(Items.DIAMOND_SHOVEL, 400),
			item(Items.DIAMOND_HOE, 400),
			item(Items.SHIELD, 350),
			item(Items.CROSSBOW, 450),
			item(Items.TRIDENT, 700),
			enchanted(Items.IRON_SWORD, Enchantments.SHARPNESS, 2, 350),
			enchanted(Items.IRON_PICKAXE, Enchantments.EFFICIENCY, 2, 350),
			enchanted(Items.BOW, Enchantments.POWER, 2, 350),
			enchanted(Items.IRON_CHESTPLATE, Enchantments.PROTECTION, 2, 500),
			item(Items.GOLDEN_APPLE, 300)
	);

	/** 강한 마법 부여 책들. */
	public static final List<PricedEntry> MYSTIC = List.of(
			book(Enchantments.SHARPNESS, 5, 2200),
			book(Enchantments.PROTECTION, 4, 2000),
			book(Enchantments.EFFICIENCY, 5, 1500),
			book(Enchantments.UNBREAKING, 3, 900),
			book(Enchantments.MENDING, 1, 2500),
			book(Enchantments.FORTUNE, 3, 1800),
			book(Enchantments.SILK_TOUCH, 1, 1600),
			book(Enchantments.LOOTING, 3, 1500),
			book(Enchantments.INFINITY, 1, 1800),
			book(Enchantments.SWEEPING, 3, 1400),
			book(Enchantments.POWER, 5, 1800),
			book(Enchantments.PUNCH, 2, 900),
			book(Enchantments.FLAME, 1, 900),
			book(Enchantments.FROST_WALKER, 2, 1000),
			book(Enchantments.DEPTH_STRIDER, 3, 1000),
			book(Enchantments.RESPIRATION, 3, 900),
			book(Enchantments.AQUA_AFFINITY, 1, 700)
	);

	public static List<PricedEntry> pool(Rarity rarity) {
		return switch (rarity) {
			case NORMAL -> NORMAL;
			case EPIC -> EPIC;
			case MYSTIC -> MYSTIC;
		};
	}
}
