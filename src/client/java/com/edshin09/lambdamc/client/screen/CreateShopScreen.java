package com.edshin09.lambdamc.client.screen;

import com.edshin09.lambdamc.shop.ShopType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** "상점 만들기": name text field + sell/buy toggle + confirm. */
public class CreateShopScreen extends Screen {
	private final Screen parent;
	private TextFieldWidget nameField;
	private ButtonWidget sellButton;
	private ButtonWidget buyButton;
	private ShopType selected = ShopType.SELLING;

	public CreateShopScreen(Screen parent) {
		super(Text.literal("상점 만들기"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = width / 2;
		int top = height / 2 - 50;

		this.nameField = new TextFieldWidget(textRenderer, centerX - 100, top, 200, 20, Text.literal("상점 이름"));
		this.nameField.setMaxLength(24);
		this.addDrawableChild(nameField);
		this.setInitialFocus(nameField);

		this.sellButton = ButtonWidget.builder(Text.literal("판매 상점"), b -> select(ShopType.SELLING))
				.dimensions(centerX - 100, top + 30, 96, 20).build();
		this.buyButton = ButtonWidget.builder(Text.literal("구매 상점"), b -> select(ShopType.BUYING))
				.dimensions(centerX + 4, top + 30, 96, 20).build();
		this.addDrawableChild(sellButton);
		this.addDrawableChild(buyButton);
		updateSelection();

		this.addDrawableChild(ButtonWidget.builder(Text.literal("확인"), b -> confirm())
				.dimensions(centerX - 100, top + 60, 96, 20).build());
		this.addDrawableChild(ButtonWidget.builder(Text.literal("취소"), b -> close())
				.dimensions(centerX + 4, top + 60, 96, 20).build());
	}

	private void select(ShopType type) {
		this.selected = type;
		updateSelection();
	}

	private void updateSelection() {
		sellButton.setMessage(Text.literal(selected == ShopType.SELLING ? "▶ 판매 상점" : "판매 상점"));
		buyButton.setMessage(Text.literal(selected == ShopType.BUYING ? "▶ 구매 상점" : "구매 상점"));
	}

	private void confirm() {
		String name = nameField.getText().trim();
		if (name.isEmpty() || client == null || client.player == null) {
			return;
		}
		String type = selected == ShopType.SELLING ? "sell" : "buy";
		client.player.networkHandler.sendChatCommand("lambdamc shop create " + type + " \"" + escape(name) + "\"");
		client.setScreen(parent);
	}

	private static String escape(String raw) {
		return raw.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	@Override
	public void close() {
		if (client != null) {
			client.setScreen(parent);
		}
	}

	@Override
	public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
		this.renderBackground(drawContext);
		super.render(drawContext, mouseX, mouseY, delta);
		drawContext.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 70, 0xFFFFFF);
		drawContext.drawCenteredTextWithShadow(textRenderer,
				Text.literal("이름은 서버 전체에서 고유해야 합니다").formatted(Formatting.GRAY),
				width / 2, height / 2 + 92, 0xAAAAAA);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
