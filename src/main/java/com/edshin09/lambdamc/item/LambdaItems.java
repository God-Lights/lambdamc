package com.edshin09.lambdamc.item;

import com.edshin09.lambdamc.LambdaMC;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Custom items added by LambdaMC. Most shop rewards (diamonds, netherite,
 * player heads, enchanted books, ...) are plain vanilla items; this class
 * only holds the items that have no vanilla equivalent.
 */
public final class LambdaItems {
	/** NBT flag set on item stacks that must never be placed as a block (e.g. Ed's head). */
	public static final String NO_PLACE_KEY = "LambdamcNoPlace";

	public static final Item RESET_CARD = new ResetCardItem(new Item.Settings().maxCount(1));

	private LambdaItems() {
	}

	public static void register() {
		register("reset_card", RESET_CARD);
	}

	private static void register(String path, Item item) {
		Registry.register(Registries.ITEM, new Identifier(LambdaMC.MOD_ID, path), item);
	}

	public static ItemStack markNoPlace(ItemStack stack) {
		NbtCompound nbt = stack.getOrCreateNbt();
		nbt.putBoolean(NO_PLACE_KEY, true);
		return stack;
	}

	public static boolean isNoPlace(ItemStack stack) {
		return stack.hasNbt() && stack.getNbt().getBoolean(NO_PLACE_KEY);
	}
}
