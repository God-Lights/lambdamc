package com.edshin09.lambdamc.shop;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.UUID;

/**
 * A single product line in a custom shop: an item (as a 1-count template),
 * a price, and a {@link ShopType} kind. For {@link ShopType#SELLING} the
 * actual sellable stock lives in the shop's storage inventory, keyed by
 * matching item; for {@link ShopType#BUYING} this only records what the
 * owner is willing to pay for the item. A shop can hold both kinds, and
 * even both kinds for the same item, at once.
 */
public final class Listing {
	private final UUID id;
	private final ItemStack template;
	private final ShopType kind;
	private long price;

	public Listing(UUID id, ItemStack template, long price, ShopType kind) {
		this.id = id;
		ItemStack copy = template.copy();
		copy.setCount(1);
		this.template = copy;
		this.price = price;
		this.kind = kind;
	}

	public UUID getId() {
		return id;
	}

	public ItemStack getTemplate() {
		return template;
	}

	public ShopType getKind() {
		return kind;
	}

	public long getPrice() {
		return price;
	}

	public void setPrice(long price) {
		this.price = price;
	}

	/** True if this listing is the same kind and refers to the same item (ignoring count) as the given stack. */
	public boolean matches(ItemStack stack, ShopType wantedKind) {
		return kind == wantedKind && sameItem(template, stack);
	}

	/** True if two stacks are the same item with the same NBT, ignoring count. */
	public static boolean sameItem(ItemStack a, ItemStack b) {
		return ItemStack.areItemsEqual(a, b) && java.util.Objects.equals(a.getNbt(), b.getNbt());
	}

	public NbtCompound writeNbt() {
		NbtCompound nbt = new NbtCompound();
		nbt.putUuid("id", id);
		nbt.put("template", template.writeNbt(new NbtCompound()));
		nbt.putLong("price", price);
		nbt.putString("kind", kind.name());
		return nbt;
	}

	public static Listing fromNbt(NbtCompound nbt) {
		UUID id = nbt.getUuid("id");
		ItemStack template = ItemStack.fromNbt(nbt.getCompound("template"));
		long price = nbt.getLong("price");
		ShopType kind = nbt.contains("kind") ? ShopType.valueOf(nbt.getString("kind")) : ShopType.SELLING;
		return new Listing(id, template, price, kind);
	}
}
