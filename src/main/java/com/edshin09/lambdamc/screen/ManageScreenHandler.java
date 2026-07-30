package com.edshin09.lambdamc.screen;

import com.edshin09.lambdamc.shop.CustomShop;
import com.edshin09.lambdamc.shop.CustomShopData;
import com.edshin09.lambdamc.shop.Listing;
import com.edshin09.lambdamc.shop.ShopType;
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
 * Shop management GUI: shows the owner's current listings, one per slot in
 * list order (slot index == listing index, matching the 1-based index the
 * {@code /lambdamc shop setprice|removeproduct} commands take). All actual
 * editing (select/edit price/delete/add product/warehouse) happens through
 * client-side widgets in {@code ManageScreen} - clicking a slot here is
 * intercepted purely client-side (no vanilla item transfer is possible on
 * these slots), so this handler's own click logic is just a safe no-op.
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

	/** Re-syncs the listing grid from the current shop state; call after any server-side edit. */
	public void refresh() {
		if (server == null) {
			return;
		}
		Optional<CustomShop> found = CustomShopData.get(server).findByName(shopName);
		this.listings = found.map(CustomShop::getListings).orElse(List.of());

		for (int i = 0; i < SLOT_COUNT; i++) {
			if (i < listings.size()) {
				displayInventory.setStack(i, decorate(listings.get(i), i + 1));
			} else {
				displayInventory.setStack(i, ItemStack.EMPTY);
			}
		}
		this.sendContentUpdates();
	}

	private static ItemStack decorate(Listing listing, int number) {
		ItemStack stack = listing.getTemplate().copy();
		NbtCompound display = stack.getOrCreateSubNbt("display");
		NbtList lore = new NbtList();
		String kindTag = listing.getKind() == ShopType.SELLING ? "§a[판매]" : "§6[구매]";
		lore.add(NbtString.of(Text.Serializer.toJson(Text.literal("#" + number + " " + kindTag))));
		lore.add(NbtString.of(Text.Serializer.toJson(Text.literal("§7가격: §e" + format(listing.getPrice()) + " λ"))));
		lore.add(NbtString.of(Text.Serializer.toJson(Text.literal("§b클릭해서 선택 (아래에서 수정/삭제)"))));
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
		// Selection is handled entirely client-side (ManageScreen#mouseClicked);
		// these slots never accept vanilla item movement either way.
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
