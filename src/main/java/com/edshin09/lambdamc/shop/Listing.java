package com.edshin09.lambdamc.shop;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.UUID;

/**
 * A single product line in a custom shop: an item (as a 1-count template)
 * and its price. For a {@link ShopType#SELLING} shop the actual sellable
 * stock lives in the shop's storage inventory, keyed by matching item; for a
 * {@link ShopType#BUYING} shop this only records what the owner is willing
 * to pay for the item.
 */
public final class Listing {
	private final UUID id;
	private final ItemStack template;
	private long price;

	public Listing(UUID id, ItemStack template, long price) {
		this.id = id;
		ItemStack copy = template.copy();
		copy.setCount(1);
		this.template = copy;
		this.price = price;
	}

	public UUID getId() {
		return id;
	}

	public ItemStack getTemplate() {
		return template;
	}

	public long getPrice() {
		return price;
	}

	public void setPrice(long price) {
		this.price = price;
	}

	/** True if two listings refer to the exact same item (id + NBT), ignoring count. */
	public boolean matches(ItemStack stack) {
		return ItemStack.areItemsEqual(template, stack) && ItemStack.areNbtEqual(template, stack);
	}

	public NbtCompound writeNbt() {
		NbtCompound nbt = new NbtCompound();
		nbt.putUuid("id", id);
		nbt.put("template", template.writeNbt(new NbtCompound()));
		nbt.putLong("price", price);
		return nbt;
	}

	public static Listing fromNbt(NbtCompound nbt) {
		UUID id = nbt.getUuid("id");
		ItemStack template = ItemStack.fromNbt(nbt.getCompound("template"));
		long price = nbt.getLong("price");
		return new Listing(id, template, price);
	}
}
