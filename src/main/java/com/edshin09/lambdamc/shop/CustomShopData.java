package com.edshin09.lambdamc.shop;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** Persistent registry of all player-created shops. */
public final class CustomShopData extends PersistentState {
	private static final String KEY = "lambdamc_shops";

	private final Map<UUID, CustomShop> shops = new LinkedHashMap<>();

	public static CustomShopData get(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager()
				.getOrCreate(CustomShopData::fromNbt, CustomShopData::new, KEY);
	}

	public static final String DEFAULT_SHOP_NAME = "lambdasharp";

	public java.util.Collection<CustomShop> all() {
		return shops.values();
	}

	public Optional<CustomShop> findByName(String name) {
		return shops.values().stream().filter(s -> s.getName().equalsIgnoreCase(name)).findFirst();
	}

	public boolean nameTaken(String name) {
		return name.equalsIgnoreCase(DEFAULT_SHOP_NAME) || findByName(name).isPresent();
	}

	public long countByOwner(UUID owner) {
		return shops.values().stream().filter(s -> s.getOwner().equals(owner)).count();
	}

	public void add(CustomShop shop) {
		shops.put(shop.getId(), shop);
		markDirty();
	}

	public void remove(UUID id) {
		shops.remove(id);
		markDirty();
	}

	public List<CustomShop> search(String query, Comparator sort) {
		return shops.values().stream()
				.filter(s -> query == null || query.isBlank() || s.getName().toLowerCase().contains(query.toLowerCase()))
				.sorted(sort.comparator())
				.collect(Collectors.toList());
	}

	public void touch() {
		markDirty();
	}

	public enum Comparator {
		RECENT((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt())),
		POPULAR((a, b) -> Integer.compare(b.getTotalSales(), a.getTotalSales())),
		NAME((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

		private final java.util.Comparator<CustomShop> comparator;

		Comparator(java.util.Comparator<CustomShop> comparator) {
			this.comparator = comparator;
		}

		public java.util.Comparator<CustomShop> comparator() {
			return comparator;
		}

		public static Comparator parse(String value) {
			if (value == null) {
				return RECENT;
			}
			return switch (value.toLowerCase()) {
				case "popular", "인기순" -> POPULAR;
				case "name", "이름순" -> NAME;
				default -> RECENT;
			};
		}
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		NbtList list = new NbtList();
		for (CustomShop shop : shops.values()) {
			list.add(shop.writeNbt());
		}
		nbt.put("shops", list);
		return nbt;
	}

	public static CustomShopData fromNbt(NbtCompound nbt) {
		CustomShopData data = new CustomShopData();
		NbtList list = nbt.getList("shops", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < list.size(); i++) {
			CustomShop shop = CustomShop.fromNbt(list.getCompound(i));
			data.shops.put(shop.getId(), shop);
		}
		return data;
	}
}
