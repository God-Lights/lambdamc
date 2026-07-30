package com.edshin09.lambdamc.screen;

import com.edshin09.lambdamc.economy.LambdaBank;
import com.edshin09.lambdamc.network.LambdaNetworking;
import com.edshin09.lambdamc.shop.Shop;
import com.edshin09.lambdamc.shop.ShopOffer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Server-authoritative shop GUI. Slots are pure display buttons: nothing
 * can be picked up, dropped, or shift-clicked. Every click is interpreted
 * server-side as a "buy" or "sell" action against {@link LambdaBank}.
 */
public class ShopScreenHandler extends ScreenHandler {
	public static final int ROWS = 5;
	public static final int SLOT_COUNT = ROWS * 9;

	private final Inventory displayInventory;

	// Only populated server-side via initServer(); left empty on the client.
	private List<ShopOffer> offers = List.of();
	private LambdaBank bank;

	public ShopScreenHandler(int syncId, PlayerInventory playerInventory) {
		super(LambdaScreenHandlers.SHOP, syncId);
		this.displayInventory = new SimpleInventory(SLOT_COUNT);
		checkSize(displayInventory, SLOT_COUNT);
		displayInventory.onOpen(playerInventory.player);

		for (int row = 0; row < ROWS; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new ShopSlot(displayInventory, col + row * 9, 8 + col * 18, 18 + row * 18));
			}
		}
	}

	public static void open(ServerPlayerEntity player, Shop shop, LambdaBank bank) {
		List<ShopOffer> offers = shop.buildOffers(bank);
		player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, p) -> {
			ShopScreenHandler handler = new ShopScreenHandler(syncId, inv);
			handler.initServer(offers, bank);
			return handler;
		}, Text.literal(shop.getDisplayName())));
	}

	private void initServer(List<ShopOffer> offers, LambdaBank bank) {
		this.offers = offers;
		this.bank = bank;
		for (int i = 0; i < offers.size(); i++) {
			ShopOffer offer = offers.get(i);
			displayInventory.setStack(i, offer == null ? ItemStack.EMPTY : decorate(offer));
		}
	}

	private static ItemStack decorate(ShopOffer offer) {
		ItemStack stack = offer.display().copy();
		NbtCompound display = stack.getOrCreateSubNbt("display");
		NbtList lore = new NbtList();

		String priceLine = offer.kind() == ShopOffer.OfferKind.BUY
				? "§7가격: §e" + format(offer.price()) + " λ"
				: "§7판매가: §e" + format(offer.price()) + " λ (교환)";
		lore.add(NbtString.of(Text.Serializer.toJson(Text.literal(priceLine))));

		if (offer.rarity() != null) {
			Text rarityText = Text.literal(offer.rarity().displayName).formatted(offer.rarity().color);
			lore.add(NbtString.of(Text.Serializer.toJson(rarityText)));
		}
		if (offer.oneTime() != null) {
			lore.add(NbtString.of(Text.Serializer.toJson(Text.literal("§c서버 전체 단 1회 한정"))));
		}

		display.put("Lore", lore);
		return stack;
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return true;
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
		if (bank == null || offers.isEmpty()) {
			// Client side: the server drives all state; nothing to do locally.
			return;
		}
		if (slotIndex < 0 || slotIndex >= offers.size()) {
			return;
		}
		ShopOffer offer = offers.get(slotIndex);
		if (offer == null) {
			return;
		}
		if (!(player instanceof ServerPlayerEntity serverPlayer)) {
			return;
		}

		if (offer.kind() == ShopOffer.OfferKind.BUY) {
			handleBuy(serverPlayer, offer, slotIndex);
		} else {
			handleSell(serverPlayer, offer, slotIndex);
		}
	}

	private void handleBuy(ServerPlayerEntity player, ShopOffer offer, int slotIndex) {
		if (offer.oneTime() != null && isOneTimeUsed(offer.oneTime())) {
			player.sendMessage(Text.translatable("lambdamc.shop.buy.sold_out"), false);
			return;
		}

		long balance = bank.getBalance(player.getUuid());
		if (balance < offer.price()) {
			player.sendMessage(Text.translatable("lambdamc.shop.buy.not_enough_money",
					format(offer.price()), format(balance)), false);
			return;
		}

		bank.subtract(player.getUuid(), offer.price());

		if (offer.oneTime() != null) {
			markOneTimeUsed(offer.oneTime());
			displayInventory.setStack(slotIndex, soldOutIcon());
		}

		ItemStack give = offer.display().copy();
		give.setCount(1);
		Text itemName = give.getName();
		if (!player.getInventory().insertStack(give)) {
			player.dropItem(give, false);
			player.sendMessage(Text.translatable("lambdamc.shop.buy.inventory_full"), false);
		}

		player.sendMessage(Text.translatable("lambdamc.shop.buy.success", itemName, format(offer.price())), false);
		LambdaNetworking.sendBalance(player, bank.getBalance(player.getUuid()));
		this.sendContentUpdates();
	}

	private void handleSell(ServerPlayerEntity player, ShopOffer offer, int slotIndex) {
		if (offer.oneTime() != null && isOneTimeUsed(offer.oneTime())) {
			player.sendMessage(Text.translatable("lambdamc.shop.sell.sold_out"), false);
			return;
		}

		Item required = offer.display().getItem();
		Text itemName = offer.display().getName();
		if (!removeOneMatching(player, required)) {
			player.sendMessage(Text.translatable("lambdamc.shop.sell.need_item", itemName), false);
			return;
		}

		if (offer.oneTime() != null) {
			markOneTimeUsed(offer.oneTime());
			displayInventory.setStack(slotIndex, soldOutIcon());
		}

		bank.add(player.getUuid(), player.getGameProfile().getName(), offer.price());
		player.sendMessage(Text.translatable("lambdamc.shop.sell.success", itemName, format(offer.price())), false);
		LambdaNetworking.sendBalance(player, bank.getBalance(player.getUuid()));
		this.sendContentUpdates();
	}

	private boolean removeOneMatching(ServerPlayerEntity player, Item item) {
		PlayerInventory inventory = player.getInventory();
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (!stack.isEmpty() && stack.getItem() == item) {
				stack.decrement(1);
				return true;
			}
		}
		return false;
	}

	private boolean isOneTimeUsed(ShopOffer.OneTimeFlag flag) {
		return switch (flag) {
			case DRAGON_EGG -> bank.isDragonEggTraded();
			case RESET_CARD -> bank.isResetCardPurchased();
		};
	}

	private void markOneTimeUsed(ShopOffer.OneTimeFlag flag) {
		switch (flag) {
			case DRAGON_EGG -> bank.markDragonEggTraded();
			case RESET_CARD -> bank.markResetCardPurchased();
		}
	}

	private static ItemStack soldOutIcon() {
		ItemStack stack = new ItemStack(Items.BARRIER);
		stack.setCustomName(Text.literal("품절 / SOLD OUT").formatted(Formatting.RED));
		return stack;
	}

	private static String format(long value) {
		return NumberFormat.getIntegerInstance(Locale.US).format(value);
	}

	/** A slot that is purely a display button: nothing can be inserted into or taken out of it directly. */
	private static class ShopSlot extends Slot {
		ShopSlot(Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return false;
		}

		@Override
		public boolean canTakeItems(PlayerEntity playerEntity) {
			return false;
		}
	}
}
