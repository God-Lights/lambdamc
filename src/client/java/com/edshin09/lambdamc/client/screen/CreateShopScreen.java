package com.edshin09.lambdamc.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** "상점 만들기": just a name - each shop can freely mix sell and buy listings once created. */
public class CreateShopScreen extends Screen {
	private final Screen parent;
	private TextFieldWidget nameField;

	public CreateShopScreen(Screen parent) {
		super(Text.literal("상점 만들기"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = width / 2;
		int top = height / 2 - 30;

		this.nameField = new TextFieldWidget(textRenderer, centerX - 100, top, 200, 20, Text.literal("상점 이름"));
		this.nameField.setMaxLength(24);
		this.addDrawableChild(nameField);
		this.setInitialFocus(nameField);

		this.addDrawableChild(ButtonWidget.builder(Text.literal("확인"), b -> confirm())
				.dimensions(centerX - 100, top + 30, 96, 20).build());
		this.addDrawableChild(ButtonWidget.builder(Text.literal("취소"), b -> close())
				.dimensions(centerX + 4, top + 30, 96, 20).build());
	}

	private void confirm() {
		String name = nameField.getText().trim();
		if (name.isEmpty() || client == null || client.player == null) {
			return;
		}
		client.player.networkHandler.sendChatCommand("lambdamc shop create \"" + escape(name) + "\"");
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
		drawContext.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 50, 0xFFFFFF);
		drawContext.drawCenteredTextWithShadow(textRenderer,
				Text.literal("이름은 서버 전체에서 고유해야 합니다. 상품은 만든 뒤 관리 화면에서 추가하세요.").formatted(Formatting.GRAY),
				width / 2, height / 2 + 62, 0xAAAAAA);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
