package com.edshin09.lambdamc.screen;

import com.edshin09.lambdamc.shop.ShopOffer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Supplier;

/**
 * Generic, read-mostly shop GUI: every slot is a display button whose click
 * runs the bound {@link ShopOffer}'s {@code action}. Used both for the
 * default shop and for browsing/trading through any custom shop - all the
 * buy/sell/fee/stock logic lives in the offer itself, not here.
 */
public class TradeScreenHandler extends ScreenHandler {
	public static final int ROWS = 5;
	public static final int SLOT_COUNT = ROWS * 9;

	private final Inventory displayInventory;
	private Supplier<List<ShopOffer>> offersSupplier;
	private List<ShopOffer> offers = List.of();

	public TradeScreenHandler(int syncId, PlayerInventory playerInventory) {
		super(LambdaScreenHandlers.TRADE, syncId);
		this.displayInventory = new SimpleInventory(SLOT_COUNT);
		checkSize(displayInventory, SLOT_COUNT);
		displayInventory.onOpen(playerInventory.player);

		for (int row = 0; row < ROWS; row++) {
			for (int col = 0; col < 9; col++) {
				// Extra 10px top margin (28 instead of 18) leaves room for a subtitle row above the grid.
				this.addSlot(new DisplaySlot(displayInventory, col + row * 9, 8 + col * 18, 28 + row * 18));
			}
		}
	}

	public static void open(ServerPlayerEntity player, String title, Supplier<List<ShopOffer>> offersSupplier) {
		player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, p) -> {
			TradeScreenHandler handler = new TradeScreenHandler(syncId, inv);
			handler.initServer(offersSupplier);
			return handler;
		}, Text.literal(title)));
	}

	private void initServer(Supplier<List<ShopOffer>> offersSupplier) {
		this.offersSupplier = offersSupplier;
		refresh();
	}

	private void refresh() {
		if (offersSupplier == null) {
			return;
		}
		this.offers = offersSupplier.get();
		for (int i = 0; i < SLOT_COUNT; i++) {
			ShopOffer offer = i < offers.size() ? offers.get(i) : null;
			displayInventory.setStack(i, offer == null ? ItemStack.EMPTY : offer.display().copy());
		}
		this.sendContentUpdates();
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
		if (offersSupplier == null) {
			// Client side: the server drives all state; nothing to do locally.
			return;
		}
		if (slotIndex < 0 || slotIndex >= offers.size()) {
			return;
		}
		ShopOffer offer = offers.get(slotIndex);
		if (offer == null || !(player instanceof ServerPlayerEntity serverPlayer)) {
			return;
		}

		var result = offer.action().execute(serverPlayer);
		if (result.message() != null) {
			serverPlayer.sendMessage(result.message(), false);
		}
		refresh();
	}

	/** A slot that is purely a display button: nothing can be inserted into or taken out of it directly. */
	private static class DisplaySlot extends Slot {
		DisplaySlot(Inventory inventory, int index, int x, int y) {
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
