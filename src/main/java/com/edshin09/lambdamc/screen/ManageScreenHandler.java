package com.edshin09.lambdamc.screen;

import com.edshin09.lambdamc.shop.CustomShop;
import com.edshin09.lambdamc.shop.CustomShopData;
import com.edshin09.lambdamc.shop.Listing;
import com.edshin09.lambdamc.shop.ShopManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Shop management GUI: shows the owner's current listings; clicking one
 * removes it (see {@link ShopManager#removeProduct}). Adding a product,
 * deleting the whole shop, and opening the warehouse are triggered from
 * extra client-side widgets that issue {@code /lambdamc shop ...} commands.
 */
public class ManageScreenHandler extends ScreenHandler {
	public static final int ROWS = 5;
	public static final int SLOT_COUNT = ROWS * 9;

	private final Inventory displayInventory;
	private String shopName;
	private MinecraftServer server;
	private List<Listing> listings = List.of();

	public ManageScreenHandler(int syncId, PlayerInventory playerInventory) {
		super(LambdaScreenHandlers.MANAGE, syncId);
		this.displayInventory = new SimpleInventory(SLOT_COUNT);
		checkSize(displayInventory, SLOT_COUNT);
		displayInventory.onOpen(playerInventory.player);

		for (int row = 0; row < ROWS; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new DisplaySlot(displayInventory, col + row * 9, 8 + col * 18, 18 + row * 18));
			}
		}
	}

	public static void open(ServerPlayerEntity player, CustomShop shop) {
		player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, p) -> {
			ManageScreenHandler handler = new ManageScreenHandler(syncId, inv);
			handler.initServer(player.getServer(), shop.getName());
			return handler;
		}, Text.literal("[관리] " + shop.getName())));
	}

	private void initServer(MinecraftServer server, String shopName) {
		this.server = server;
		this.shopName = shopName;
		refresh();
	}

	private void refresh() {
		if (server == null) {
			return;
		}
		Optional<CustomShop> found = CustomShopData.get(server).findByName(shopName);
		this.listings = found.map(CustomShop::getListings).orElse(List.of());

		for (int i = 0; i < SLOT_COUNT; i++) {
			if (i < listings.size()) {
				displayInventory.setStack(i, decorate(listings.get(i)));
			} else {
				displayInventory.setStack(i, ItemStack.EMPTY);
			}
		}
		this.sendContentUpdates();
	}

	private static ItemStack decorate(Listing listing) {
		ItemStack stack = listing.getTemplate().copy();
		NbtCompound display = stack.getOrCreateSubNbt("display");
		NbtList lore = new NbtList();
		lore.add(NbtString.of(Text.Serializer.toJson(Text.literal("§7가격: §e" + format(listing.getPrice()) + " λ"))));
		lore.add(NbtString.of(Text.Serializer.toJson(Text.literal("§c클릭해서 이 상품 삭제"))));
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
		if (server == null) {
			return;
		}
		if (slotIndex < 0 || slotIndex >= listings.size()) {
			return;
		}
		if (!(player instanceof ServerPlayerEntity serverPlayer)) {
			return;
		}

		ShopManager.Result result = ShopManager.removeProduct(serverPlayer, shopName, slotIndex + 1);
		serverPlayer.sendMessage(Text.literal(result.message()), false);
		refresh();
	}

	private static String format(long value) {
		return NumberFormat.getIntegerInstance(Locale.US).format(value);
	}

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
