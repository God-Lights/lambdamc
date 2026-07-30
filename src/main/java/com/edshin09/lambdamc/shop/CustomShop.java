package com.edshin09.lambdamc.shop;

import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** A player-created shop: either sells items to other players, or buys items from them. */
public final class CustomShop {
	public static final int STORAGE_SIZE = 27;

	private final UUID id;
	private String name;
	private final UUID owner;
	private String ownerName;
	private final ShopType type;
	private final long createdAt;
	private int totalSales;
	private final List<Listing> listings = new ArrayList<>();
	private final SimpleInventory storage = new SimpleInventory(STORAGE_SIZE);

	public CustomShop(UUID id, String name, UUID owner, String ownerName, ShopType type, long createdAt) {
		this.id = id;
		this.name = name;
		this.owner = owner;
		this.ownerName = ownerName;
		this.type = type;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public UUID getOwner() {
		return owner;
	}

	public String getOwnerName() {
		return ownerName;
	}

	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

	public ShopType getType() {
		return type;
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public int getTotalSales() {
		return totalSales;
	}

	public void incrementSales() {
		totalSales++;
	}

	public List<Listing> getListings() {
		return listings;
	}

	public Optional<Listing> findListing(UUID listingId) {
		return listings.stream().filter(l -> l.getId().equals(listingId)).findFirst();
	}

	public Optional<Listing> findListingByItem(ItemStack stack) {
		return listings.stream().filter(l -> l.matches(stack)).findFirst();
	}

	public SimpleInventory getStorage() {
		return storage;
	}

	/** How many of the given item this shop currently has physically stocked. */
	public int countStock(ItemStack template) {
		int count = 0;
		for (int i = 0; i < storage.size(); i++) {
			ItemStack stack = storage.getStack(i);
			if (!stack.isEmpty() && Listing.sameItem(stack, template)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	/** Attempts to remove {@code amount} of the given item from storage. Returns false if not enough stock. */
	public boolean takeStock(ItemStack template, int amount) {
		if (countStock(template) < amount) {
			return false;
		}
		int remaining = amount;
		for (int i = 0; i < storage.size() && remaining > 0; i++) {
			ItemStack stack = storage.getStack(i);
			if (stack.isEmpty() || !Listing.sameItem(stack, template)) {
				continue;
			}
			int take = Math.min(remaining, stack.getCount());
			stack.decrement(take);
			remaining -= take;
		}
		storage.markDirty();
		return true;
	}

	/** Attempts to add the given stack into storage; returns leftover that didn't fit (empty if all fit). */
	public ItemStack addStock(ItemStack stack) {
		ItemStack remainder = stack.copy();
		for (int i = 0; i < storage.size() && !remainder.isEmpty(); i++) {
			ItemStack existing = storage.getStack(i);
			if (existing.isEmpty()) {
				storage.setStack(i, remainder);
				remainder = ItemStack.EMPTY;
				break;
			}
			if (Listing.sameItem(existing, remainder) && existing.getCount() < existing.getMaxCount()) {
				int space = existing.getMaxCount() - existing.getCount();
				int move = Math.min(space, remainder.getCount());
				existing.increment(move);
				remainder.decrement(move);
			}
		}
		storage.markDirty();
		return remainder;
	}

	public NbtCompound writeNbt() {
		NbtCompound nbt = new NbtCompound();
		nbt.putUuid("id", id);
		nbt.putString("name", name);
		nbt.putUuid("owner", owner);
		nbt.putString("ownerName", ownerName);
		nbt.putString("type", type.name());
		nbt.putLong("createdAt", createdAt);
		nbt.putInt("totalSales", totalSales);

		NbtList listingList = new NbtList();
		for (Listing listing : listings) {
			listingList.add(listing.writeNbt());
		}
		nbt.put("listings", listingList);

		NbtList storageList = new NbtList();
		for (int i = 0; i < storage.size(); i++) {
			ItemStack stack = storage.getStack(i);
			if (stack.isEmpty()) {
				continue;
			}
			NbtCompound slotNbt = stack.writeNbt(new NbtCompound());
			slotNbt.putInt("Slot", i);
			storageList.add(slotNbt);
		}
		nbt.put("storage", storageList);

		return nbt;
	}

	public static CustomShop fromNbt(NbtCompound nbt) {
		UUID id = nbt.getUuid("id");
		String name = nbt.getString("name");
		UUID owner = nbt.getUuid("owner");
		String ownerName = nbt.getString("ownerName");
		ShopType type = ShopType.valueOf(nbt.getString("type"));
		long createdAt = nbt.getLong("createdAt");

		CustomShop shop = new CustomShop(id, name, owner, ownerName, type, createdAt);
		shop.totalSales = nbt.getInt("totalSales");

		NbtList listingList = nbt.getList("listings", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < listingList.size(); i++) {
			shop.listings.add(Listing.fromNbt(listingList.getCompound(i)));
		}

		NbtList storageList = nbt.getList("storage", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < storageList.size(); i++) {
			NbtCompound slotNbt = storageList.getCompound(i);
			int slot = slotNbt.getInt("Slot");
			if (slot >= 0 && slot < shop.storage.size()) {
				shop.storage.setStack(slot, ItemStack.fromNbt(slotNbt));
			}
		}

		return shop;
	}
}
