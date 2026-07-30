package com.edshin09.lambdamc.client.keybind;

import com.edshin09.lambdamc.client.screen.ShopMenuScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class LambdaKeyBindings {
	public static KeyBinding openShopMenu;

	private LambdaKeyBindings() {
	}

	public static void register() {
		openShopMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.lambdamc.open_shop_menu",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_M,
				"category.lambdamc"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openShopMenu.wasPressed()) {
				if (client.player != null && client.currentScreen == null) {
					client.setScreen(new ShopMenuScreen());
				}
			}
		});
	}
}
