package com.edshin09.lambdamc.item;

import com.edshin09.lambdamc.LambdaMC;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Custom items added by LambdaMC. Most shop rewards (diamonds, netherite,
 * player heads, enchanted books, ...) are plain vanilla items; this class
 * only holds the items that have no vanilla equivalent.
 */
public final class LambdaItems {
	/** NBT flag set on item stacks that must never be placed as a block (e.g. Ed's head). */
	public static final String NO_PLACE_KEY = "LambdamcNoPlace";

	public static final Item RESET_CARD = new ResetCardItem(new Item.Settings().maxCount(1));
	public static final Item LAMBDA_VOUCHER = new LambdaVoucherItem(new Item.Settings().maxCount(64));

	private LambdaItems() {
	}

	public static void register() {
		register("reset_card", RESET_CARD);
		register("lambda_voucher", LAMBDA_VOUCHER);
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

	/** Creates a single λ-redemption voucher for the given amount, with its name/lore baked in. */
	public static ItemStack createVoucher(long amount) {
		ItemStack stack = new ItemStack(LAMBDA_VOUCHER);
		stack.getOrCreateNbt().putLong(LambdaVoucherItem.AMOUNT_KEY, amount);
		stack.setCustomName(Text.literal("람다 교환권(λ " + formatAmount(amount) + ")").formatted(Formatting.GOLD));

		NbtCompound display = stack.getOrCreateSubNbt("display");
		NbtList lore = new NbtList();
		lore.add(NbtString.of(Text.Serializer.toJson(Text.literal("§7우클릭을 통해 교환하세요!"))));
		display.put("Lore", lore);

		return stack;
	}

	public static String formatAmount(long value) {
		return NumberFormat.getIntegerInstance(Locale.US).format(value);
	}
}
