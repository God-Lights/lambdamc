package com.edshin09.lambdamc.item;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

/** Blocks placement of item stacks flagged as {@link LambdaItems#NO_PLACE_KEY}, such as Ed's shop head. */
public final class LambdaItemEvents {
	private LambdaItemEvents() {
	}

	public static void register() {
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			ItemStack stack = player.getStackInHand(hand);
			if (LambdaItems.isNoPlace(stack)) {
				return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});
	}
}
