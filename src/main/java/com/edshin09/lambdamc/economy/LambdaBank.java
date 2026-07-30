package com.edshin09.lambdamc.economy;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-wide persistent storage for the Lambda (λ) currency.
 * Saved as part of the overworld's level data.
 */
public final class LambdaBank extends PersistentState {
	private static final String KEY = "lambdamc_bank";

	private final Map<UUID, Long> balances = new LinkedHashMap<>();
	private final Map<UUID, String> knownNames = new LinkedHashMap<>();

	/** True once the single server-wide Dragon Egg trade has been used. */
	private boolean dragonEggTraded = false;
	/** True once the single server-wide Reset Card has been purchased. */
	private boolean resetCardPurchased = false;

	public static LambdaBank get(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager()
				.getOrCreate(LambdaBank::fromNbt, LambdaBank::new, KEY);
	}

	public boolean hasAccount(UUID uuid) {
		return balances.containsKey(uuid);
	}

	public long getBalance(UUID uuid) {
		return balances.getOrDefault(uuid, 0L);
	}

	public void setBalance(UUID uuid, String name, long amount) {
		balances.put(uuid, Math.max(0L, amount));
		knownNames.put(uuid, name);
		markDirty();
	}

	public void openAccount(UUID uuid, String name, long startingBalance) {
		balances.put(uuid, startingBalance);
		knownNames.put(uuid, name);
		markDirty();
	}

	public void updateName(UUID uuid, String name) {
		knownNames.put(uuid, name);
		markDirty();
	}

	public String getName(UUID uuid) {
		return knownNames.getOrDefault(uuid, uuid.toString());
	}

	public boolean add(UUID uuid, String name, long amount) {
		if (amount < 0) {
			return subtract(uuid, -amount);
		}
		balances.merge(uuid, amount, Long::sum);
		knownNames.put(uuid, name);
		markDirty();
		return true;
	}

	/** Attempts to subtract {@code amount} from the player's balance. Returns false if insufficient funds. */
	public boolean subtract(UUID uuid, long amount) {
		long current = getBalance(uuid);
		if (current < amount) {
			return false;
		}
		balances.put(uuid, current - amount);
		markDirty();
		return true;
	}

	public Map<UUID, Long> getBalances() {
		return balances;
	}

	public boolean isDragonEggTraded() {
		return dragonEggTraded;
	}

	public void markDragonEggTraded() {
		this.dragonEggTraded = true;
		markDirty();
	}

	public boolean isResetCardPurchased() {
		return resetCardPurchased;
	}

	public void markResetCardPurchased() {
		this.resetCardPurchased = true;
		markDirty();
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		NbtList playerList = new NbtList();
		for (Map.Entry<UUID, Long> entry : balances.entrySet()) {
			NbtCompound playerNbt = new NbtCompound();
			playerNbt.putUuid("uuid", entry.getKey());
			playerNbt.putString("name", knownNames.getOrDefault(entry.getKey(), ""));
			playerNbt.putLong("balance", entry.getValue());
			playerList.add(playerNbt);
		}
		nbt.put("players", playerList);
		nbt.putBoolean("dragonEggTraded", dragonEggTraded);
		nbt.putBoolean("resetCardPurchased", resetCardPurchased);
		return nbt;
	}

	public static LambdaBank fromNbt(NbtCompound nbt) {
		LambdaBank bank = new LambdaBank();
		NbtList playerList = nbt.getList("players", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < playerList.size(); i++) {
			NbtCompound playerNbt = playerList.getCompound(i);
			UUID uuid = playerNbt.getUuid("uuid");
			bank.balances.put(uuid, playerNbt.getLong("balance"));
			bank.knownNames.put(uuid, playerNbt.getString("name"));
		}
		bank.dragonEggTraded = nbt.getBoolean("dragonEggTraded");
		bank.resetCardPurchased = nbt.getBoolean("resetCardPurchased");
		return bank;
	}
}
