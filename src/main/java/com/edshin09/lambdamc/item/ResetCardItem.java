package com.edshin09.lambdamc.item;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * The (very expensive) "Server Reset Card". Pure troll item: it never wipes
 * any real server data. Using it broadcasts a scary-looking warning to the
 * whole server, immediately followed by a "just kidding" reveal, and briefly
 * nauseates the user for comedic effect. It is consumed on use.
 */
public final class ResetCardItem extends Item {
	public ResetCardItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient) {
			return TypedActionResult.success(stack, false);
		}

		if (user instanceof ServerPlayerEntity serverPlayer) {
			serverPlayer.getServer().getPlayerManager().broadcast(
					Text.translatable("lambdamc.reset_card.use", serverPlayer.getGameProfile().getName()), false);

			for (ServerPlayerEntity target : serverPlayer.getServer().getPlayerManager().getPlayerList()) {
				target.getWorld().playSound(null, target.getBlockPos(), SoundEvents.ENTITY_WITHER_SPAWN,
						SoundCategory.MASTER, 0.4f, 1.0f);
			}
			user.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 60, 0));
		}

		stack.decrement(1);
		return TypedActionResult.success(stack, false);
	}
}
