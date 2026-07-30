package com.edshin09.lambdamc.client.screen;

import com.edshin09.lambdamc.screen.ManageScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/** Shop management GUI: grid of current listings (click to remove) plus add/delete/warehouse controls. */
public class ManageScreen extends HandledScreen<ManageScreenHandler> {
	private static final int PANEL_COLOR = 0xC0101010;
	private static final int BORDER_COLOR = 0xFF3A3A3A;
	private static final int SLOT_COLOR = 0x60FFFFFF;
	private static final long CONFIRM_WINDOW_MS = 5000L;

	private final String shopName;
	private TextFieldWidget priceField;
	private ButtonWidget deleteButton;
	private long deleteArmedAt = -1;

	public ManageScreen(ManageScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.shopName = title.getString().replaceFirst("^\\[관리\\] ", "");
		this.backgroundWidth = 176;
		this.backgroundHeight = 18 + ManageScreenHandler.ROWS * 18 + 16 + 58;
	}

	@Override
	protected void init() {
		super.init();
		int controlsY = y + 17 + ManageScreenHandler.ROWS * 18 + 8;

		this.priceField = new TextFieldWidget(textRenderer, x + 8, controlsY, 80, 20, Text.literal("가격"));
		this.priceField.setMaxLength(10);
		this.addDrawableChild(priceField);

		this.addDrawableChild(ButtonWidget.builder(Text.literal("손의 아이템 등록"), b -> addProduct())
				.dimensions(x + 92, controlsY, 76, 20).build());

		this.addDrawableChild(ButtonWidget.builder(Text.literal("창고 열기"), b -> runCommand("warehouse"))
				.dimensions(x + 8, controlsY + 24, 80, 20).build());

		this.deleteButton = ButtonWidget.builder(Text.literal("상점 삭제"), b -> onDeleteClicked())
				.dimensions(x + 92, controlsY + 24, 76, 20).build();
		this.addDrawableChild(deleteButton);
	}

	private void addProduct() {
		String raw = priceField.getText().trim();
		try {
			long price = Long.parseLong(raw);
			if (price < 0) {
				return;
			}
			if (client != null && client.player != null) {
				client.player.networkHandler.sendChatCommand(
						"lambdamc shop addproduct \"" + escape(shopName) + "\" " + price);
			}
		} catch (NumberFormatException ignored) {
			// invalid price typed; just ignore the click
		}
	}

	private void onDeleteClicked() {
		long now = System.currentTimeMillis();
		if (deleteArmedAt > 0 && now - deleteArmedAt < CONFIRM_WINDOW_MS) {
			runCommand("delete");
			if (client != null) {
				client.setScreen(null);
			}
			return;
		}
		deleteArmedAt = now;
		deleteButton.setMessage(Text.literal("정말요? 다시 클릭"));
	}

	@Override
	protected void handledScreenTick() {
		super.handledScreenTick();
		if (deleteArmedAt > 0 && System.currentTimeMillis() - deleteArmedAt > CONFIRM_WINDOW_MS) {
			deleteArmedAt = -1;
			deleteButton.setMessage(Text.literal("상점 삭제"));
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
				int slotX = x + 7 + col * 18;
				int slotY = y + 17 + row * 18;
				drawContext.fill(slotX, slotY, slotX + 18, slotY + 18, SLOT_COLOR);
			}
		}
	}

	@Override
	protected void drawForeground(DrawContext drawContext, int mouseX, int mouseY) {
		drawContext.drawText(textRenderer, title, 8, 6, 0xFFFFFF, false);
	}
}
