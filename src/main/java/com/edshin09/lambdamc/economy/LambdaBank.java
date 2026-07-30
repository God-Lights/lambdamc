package com.edshin09.lambdamc.economy;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-wide persistent storage for the Lambda (λ) currency: balances,
 * one-time-purchase flags, offline-sale notifications, and a bounded
 * transaction log for admins.
 */
public final class LambdaBank extends PersistentState {
	private static final String KEY = "lambdamc_bank";
	private static final int MAX_LOG_ENTRIES = 500;
	private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final Map<UUID, Long> balances = new LinkedHashMap<>();
	private final Map<UUID, String> knownNames = new LinkedHashMap<>();
	private final Map<UUID, List<String>> pendingNotifications = new LinkedHashMap<>();
	private final Deque<String> transactionLog = new ArrayDeque<>();

	private boolean dragonEggTraded = false;
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

	public void setBalance(UUID uuid, String name, long amount) {
		balances.put(uuid, Math.max(0L, amount));
		knownNames.put(uuid, name);
		markDirty();
	}

	public void add(UUID uuid, String name, long amount) {
		if (amount == 0) {
			return;
		}
		if (amount < 0) {
			subtract(uuid, -amount);
			return;
		}
		balances.merge(uuid, amount, Long::sum);
		knownNames.put(uuid, name);
		markDirty();
	}

	/** Attempts to subtract {@code amount} from the player's balance. Returns false if insufficient funds. */
	public boolean subtract(UUID uuid, long amount) {
		if (amount == 0) {
			return true;
		}
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

	// --- offline notifications ---

	public void notify(UUID uuid, String message) {
		pendingNotifications.computeIfAbsent(uuid, k -> new ArrayList<>()).add(message);
		markDirty();
	}

	public List<String> pollNotifications(UUID uuid) {
		List<String> messages = pendingNotifications.remove(uuid);
		if (messages != null) {
			markDirty();
		}
		return messages == null ? List.of() : messages;
	}

	// --- transaction log ---

	public void log(String summary) {
		transactionLog.addLast("[" + LOG_TIME.format(Instant.now()) + "] " + summary);
		while (transactionLog.size() > MAX_LOG_ENTRIES) {
			transactionLog.removeFirst();
		}
		markDirty();
	}

	public List<String> getRecentLog(int count) {
		List<String> all = new ArrayList<>(transactionLog);
		int from = Math.max(0, all.size() - count);
		return all.subList(from, all.size());
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

		NbtList notifyList = new NbtList();
		for (Map.Entry<UUID, List<String>> entry : pendingNotifications.entrySet()) {
			NbtCompound entryNbt = new NbtCompound();
			entryNbt.putUuid("uuid", entry.getKey());
			NbtList messages = new NbtList();
			for (String message : entry.getValue()) {
				messages.add(NbtString.of(message));
			}
			entryNbt.put("messages", messages);
			notifyList.add(entryNbt);
		}
		nbt.put("notifications", notifyList);

		NbtList logList = new NbtList();
		for (String entry : transactionLog) {
			logList.add(NbtString.of(entry));
		}
		nbt.put("log", logList);

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

		NbtList notifyList = nbt.getList("notifications", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < notifyList.size(); i++) {
			NbtCompound entryNbt = notifyList.getCompound(i);
			UUID uuid = entryNbt.getUuid("uuid");
			NbtList messages = entryNbt.getList("messages", NbtElement.STRING_TYPE);
			List<String> list = new ArrayList<>();
			for (int j = 0; j < messages.size(); j++) {
				list.add(messages.getString(j));
			}
			bank.pendingNotifications.put(uuid, list);
		}

		NbtList logList = nbt.getList("log", NbtElement.STRING_TYPE);
		for (int i = 0; i < logList.size(); i++) {
			bank.transactionLog.addLast(logList.getString(i));
		}

		return bank;
	}
}
