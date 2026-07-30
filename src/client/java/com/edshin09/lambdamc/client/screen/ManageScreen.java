package com.edshin09.lambdamc.client.screen;

import com.edshin09.lambdamc.screen.ManageScreenHandler;
import com.edshin09.lambdamc.shop.ShopType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Shop management GUI: a grid of the owner's current listings (click one to
 * select it - purely client-side, no item is ever moved) plus controls to
 * add a new product, edit the selected listing's price, delete it, open the
 * warehouse, or delete the whole shop.
 */
public class ManageScreen extends HandledScreen<ManageScreenHandler> {
	private static final int PANEL_COLOR = 0xC0101010;
	private static final int BORDER_COLOR = 0xFF3A3A3A;
	private static final int SLOT_COLOR = 0x60FFFFFF;
	private static final int SELECTED_COLOR = 0x9047A0FF;
	private static final long CONFIRM_WINDOW_MS = 5000L;

	private final String shopName;
	private TextFieldWidget priceField;
	private ButtonWidget deleteShopButton;
	private ButtonWidget addKindToggle;
	private long deleteArmedAt = -1;
	private ShopType addKind = ShopType.SELLING;
	private int selectedIndex = -1;

	public ManageScreen(ManageScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.shopName = title.getString().replaceFirst("^\\[관리\\] ", "");
		this.backgroundWidth = 176;
		this.backgroundHeight = 18 + ManageScreenHandler.ROWS * 18 + 16 + 100;
	}

	@Override
	protected void init() {
		super.init();
		int controlsY = y + 17 + ManageScreenHandler.ROWS * 18 + 20;

		this.priceField = new TextFieldWidget(textRenderer, x + 8, controlsY, 60, 20, Text.literal("가격"));
		this.priceField.setMaxLength(10);
		this.addDrawableChild(priceField);

		this.addKindToggle = ButtonWidget.builder(Text.literal(addKind.displayName + "로"), b -> {
			addKind = addKind == ShopType.SELLING ? ShopType.BUYING : ShopType.SELLING;
			b.setMessage(Text.literal(addKind.displayName + "로"));
		}).dimensions(x + 72, controlsY, 44, 20).build();
		this.addDrawableChild(addKindToggle);

		this.addDrawableChild(ButtonWidget.builder(Text.literal("손의 아이템 등록"), b -> addProduct())
				.dimensions(x + 120, controlsY, 52, 20).build());

		this.addDrawableChild(ButtonWidget.builder(Text.literal("선택 가격 수정"), b -> editSelectedPrice())
				.dimensions(x + 8, controlsY + 24, 84, 20).build());
		this.addDrawableChild(ButtonWidget.builder(Text.literal("선택 삭제"), b -> deleteSelected())
				.dimensions(x + 96, controlsY + 24, 76, 20).build());

		this.addDrawableChild(ButtonWidget.builder(Text.literal("창고 열기"), b -> runCommand("warehouse"))
				.dimensions(x + 8, controlsY + 48, 84, 20).build());
		this.deleteShopButton = ButtonWidget.builder(Text.literal("상점 삭제"), b -> onDeleteShopClicked())
				.dimensions(x + 96, controlsY + 48, 76, 20).build();
		this.addDrawableChild(deleteShopButton);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int gridLeft = x + 7;
		int gridTop = y + 17;
		int gridRight = gridLeft + 9 * 18;
		int gridBottom = gridTop + ManageScreenHandler.ROWS * 18;

		if (mouseX >= gridLeft && mouseX < gridRight && mouseY >= gridTop && mouseY < gridBottom) {
			int col = (int) ((mouseX - gridLeft) / 18);
			int row = (int) ((mouseY - gridTop) / 18);
			int index = row * 9 + col;
			if (index >= 0 && index < ManageScreenHandler.SLOT_COUNT) {
				ItemStack stack = getScreenHandler().getSlot(index).getStack();
				if (!stack.isEmpty()) {
					selectedIndex = index;
					priceField.setText("");
					this.setFocused(priceField);
				}
			}
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private void addProduct() {
		long price = parsePrice();
		if (price < 0 || client == null || client.player == null) {
			return;
		}
		String kind = addKind == ShopType.SELLING ? "sell" : "buy";
		client.player.networkHandler.sendChatCommand(
				"lambdamc shop addproduct \"" + escape(shopName) + "\" " + kind + " " + price);
	}

	private void editSelectedPrice() {
		if (selectedIndex < 0) {
			return;
		}
		long price = parsePrice();
		if (price < 0 || client == null || client.player == null) {
			return;
		}
		client.player.networkHandler.sendChatCommand(
				"lambdamc shop setprice \"" + escape(shopName) + "\" " + (selectedIndex + 1) + " " + price);
	}

	private void deleteSelected() {
		if (selectedIndex < 0 || client == null || client.player == null) {
			return;
		}
		client.player.networkHandler.sendChatCommand(
				"lambdamc shop removeproduct \"" + escape(shopName) + "\" " + (selectedIndex + 1));
		selectedIndex = -1;
	}

	private long parsePrice() {
		try {
			long price = Long.parseLong(priceField.getText().trim());
			return price < 0 ? -1 : price;
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private void onDeleteShopClicked() {
		long now = System.currentTimeMillis();
		if (deleteArmedAt > 0 && now - deleteArmedAt < CONFIRM_WINDOW_MS) {
			runCommand("delete");
			if (client != null) {
				client.setScreen(null);
			}
			return;
		}
		deleteArmedAt = now;
		deleteShopButton.setMessage(Text.literal("정말요? 다시 클릭"));
	}

	@Override
	protected void handledScreenTick() {
		super.handledScreenTick();
		if (deleteArmedAt > 0 && System.currentTimeMillis() - deleteArmedAt > CONFIRM_WINDOW_MS) {
			deleteArmedAt = -1;
			deleteShopButton.setMessage(Text.literal("상점 삭제"));
		}
		if (selectedIndex >= 0 && getScreenHandler().getSlot(selectedIndex).getStack().isEmpty()) {
			selectedIndex = -1;
		}
	}

	private void runCommand(String action) {
		if (client != null && client.player != null) {
			client.player.networkHandler.sendChatCommand("lambdamc shop " + action + " \"" + escape(shopName) + "\"");
		}
	}

	private static String escape(String raw) {
		return raw.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	@Override
	protected void drawBackground(DrawContext drawContext, float delta, int mouseX, int mouseY) {
		drawContext.fill(x, y, x + backgroundWidth, y + backgroundHeight, PANEL_COLOR);
		drawContext.drawBorder(x, y, backgroundWidth, backgroundHeight, BORDER_COLOR);

		for (int row = 0; row < ManageScreenHandler.ROWS; row++) {
			for (int col = 0; col < 9; col++) {
				int index = row * 9 + col;
				int slotX = x + 7 + col * 18;
				int slotY = y + 17 + row * 18;
				int color = index == selectedIndex ? SELECTED_COLOR : SLOT_COLOR;
				drawContext.fill(slotX, slotY, slotX + 18, slotY + 18, color);
			}
		}

		int controlsY = y + 17 + ManageScreenHandler.ROWS * 18 + 20;
		drawContext.fill(x + 4, controlsY - 12, x + backgroundWidth - 4, controlsY - 8, BORDER_COLOR);
	}

	@Override
	protected void drawForeground(DrawContext drawContext, int mouseX, int mouseY) {
		drawContext.drawText(textRenderer, title, 8, 6, 0xFFFFFF, false);

		int controlsY = 17 + ManageScreenHandler.ROWS * 18 + 20;
		Text status = selectedIndex < 0
				? Text.literal("상품을 클릭해 선택하세요").formatted(Formatting.GRAY)
				: Text.literal("선택됨: #" + (selectedIndex + 1)).formatted(Formatting.AQUA);
		drawContext.drawText(textRenderer, status, 8, controlsY - 9, 0xFFFFFF, false);
	}
}
