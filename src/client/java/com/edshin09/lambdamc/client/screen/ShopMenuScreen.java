package com.edshin09.lambdamc.client.screen;

import com.edshin09.lambdamc.shop.ShopSummary;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** {@code M} key menu: search/sort custom shops, create a shop, or jump into the default shop. */
public class ShopMenuScreen extends Screen {
	private static final int PAGE_SIZE = 6;
	private static final String[] SORT_MODES = {"recent", "popular", "name"};
	private static final String[] SORT_LABELS = {"최신순", "인기순", "이름순"};

	private TextFieldWidget searchField;
	private int sortIndex = 0;
	private boolean mineOnly = false;
	private int page = 0;

	public ShopMenuScreen() {
		super(Text.literal("상점 목록"));
	}

	@Override
	protected void init() {
		LambdaShopListCache.setListener(this::rebuildWidgets);
		rebuildWidgets();
		requestList();
	}

	@Override
	public void removed() {
		LambdaShopListCache.setListener(null);
	}

	private void requestList() {
		if (client == null || client.player == null) {
			return;
		}
		StringBuilder cmd = new StringBuilder("lambdamc shop list ").append(SORT_MODES[sortIndex]);
		String query = searchField != null ? searchField.getText().trim() : "";
		if (!query.isEmpty()) {
			cmd.append(' ').append(query);
		}
		client.player.networkHandler.sendChatCommand(cmd.toString());
	}

	private void rebuildWidgets() {
		this.clearChildren();
		int centerX = width / 2;
		int top = 32;

		String previousQuery = searchField != null ? searchField.getText() : "";
		searchField = new TextFieldWidget(textRenderer, centerX - 100, top, 140, 20, Text.literal("검색"));
		searchField.setMaxLength(32);
		searchField.setText(previousQuery);
		searchField.setChangedListener(s -> {
			page = 0;
			requestList();
		});
		addDrawableChild(searchField);

		addDrawableChild(ButtonWidget.builder(Text.literal(SORT_LABELS[sortIndex]), b -> {
			sortIndex = (sortIndex + 1) % SORT_MODES.length;
			b.setMessage(Text.literal(SORT_LABELS[sortIndex]));
			requestList();
		}).dimensions(centerX + 44, top, 80, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal(mineOnly ? "▶ 내 상점만" : "내 상점만"), b -> {
			mineOnly = !mineOnly;
			b.setMessage(Text.literal(mineOnly ? "▶ 내 상점만" : "내 상점만"));
			page = 0;
		}).dimensions(centerX - 100, top + 24, 224, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("상점 만들기"), b -> {
			if (client != null) {
				client.setScreen(new CreateShopScreen(this));
			}
		}).dimensions(centerX - 100, top + 48, 108, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("기본 상점 (LambdaSharp)"), b -> openDefaultShop())
				.dimensions(centerX + 16, top + 48, 108, 20).build());

		List<ShopSummary> filtered = filtered();
		int totalPages = Math.max(1, (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE);
		page = Math.max(0, Math.min(page, totalPages - 1));

		int rowsTop = top + 78;
		int from = page * PAGE_SIZE;
		int to = Math.min(filtered.size(), from + PAGE_SIZE);
		for (int i = from; i < to; i++) {
			ShopSummary shop = filtered.get(i);
			String label = shop.name() + " (" + shop.ownerName() + ") 판매" + shop.sellCount() + "/구매" + shop.buyCount();
			addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> onShopClicked(shop))
					.dimensions(centerX - 100, rowsTop + (i - from) * 22, 200, 20).build());
		}

		int navY = rowsTop + PAGE_SIZE * 22 + 6;
		int currentPage = page;
		int finalTotalPages = totalPages;
		addDrawableChild(ButtonWidget.builder(Text.literal("◀"), b -> {
			if (currentPage > 0) {
				page--;
				rebuildWidgets();
			}
		}).dimensions(centerX - 100, navY, 40, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("▶"), b -> {
			if (currentPage < finalTotalPages - 1) {
				page++;
				rebuildWidgets();
			}
		}).dimensions(centerX + 60, navY, 40, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("닫기"), b -> close())
				.dimensions(centerX - 100, navY + 26, 200, 20).build());
	}

	private void onShopClicked(ShopSummary shop) {
		if (client == null || client.player == null) {
			return;
		}
		String action = mineOnly ? "manage" : "open";
		client.player.networkHandler.sendChatCommand("lambdamc shop " + action + " \"" + escape(shop.name()) + "\"");
		client.setScreen(null);
	}

	private void openDefaultShop() {
		if (client != null && client.player != null) {
			client.player.networkHandler.sendChatCommand("lambdamc shop open \"lambdasharp\"");
			client.setScreen(null);
		}
	}

	private List<ShopSummary> filtered() {
		List<ShopSummary> all = LambdaShopListCache.get();
		if (!mineOnly || client == null || client.player == null) {
			return all;
		}
		String myName = client.player.getGameProfile().getName();
		return all.stream().filter(s -> s.ownerName().equalsIgnoreCase(myName)).toList();
	}

	private static String escape(String raw) {
		return raw.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	@Override
	public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
		this.renderBackground(drawContext);
		super.render(drawContext, mouseX, mouseY, delta);
		drawContext.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);
		if (filtered().isEmpty()) {
			drawContext.drawCenteredTextWithShadow(textRenderer,
					Text.literal("표시할 상점이 없습니다").formatted(Formatting.GRAY), width / 2, 110, 0xAAAAAA);
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
