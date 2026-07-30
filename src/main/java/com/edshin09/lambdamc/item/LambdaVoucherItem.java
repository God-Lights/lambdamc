package com.edshin09.lambdamc.item;

import com.edshin09.lambdamc.economy.LambdaBank;
import com.edshin09.lambdamc.network.LambdaNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * A one-time-use voucher redeemable (right-click) for a fixed amount of λ
 * baked into its NBT at creation time. Unlike admin give/set, this is meant
 * to be handed out as a physical, tradeable/droppable reward (events,
 * competitions, ...) rather than a direct balance edit.
 */
public final class LambdaVoucherItem extends Item {
	public static final String AMOUNT_KEY = "LambdaVoucherAmount";

	public LambdaVoucherItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient) {
			return TypedActionResult.success(stack, false);
		}
		if (!(user instanceof ServerPlayerEntity player)) {
			return TypedActionResult.pass(stack);
		}

		MinecraftServer server = player.getServer();
		if (server == null || !server.isDedicated()) {
			player.sendMessage(Text.translatable("lambdamc.dedicated_only"), false);
			return TypedActionResult.fail(stack);
		}

		long amount = stack.hasNbt() ? stack.getNbt().getLong(AMOUNT_KEY) : 0;
		if (amount <= 0) {
			player.sendMessage(Text.literal("사용할 수 없는 교환권입니다."), false);
			return TypedActionResult.fail(stack);
		}

		LambdaBank bank = LambdaBank.get(server);
		bank.add(player.getUuid(), player.getGameProfile().getName(), amount);
		bank.log(player.getGameProfile().getName() + "이(가) 람다 교환권(λ " + amount + ") 사용");
		LambdaNetworking.sendBalance(player, bank.getBalance(player.getUuid()));

		player.sendMessage(Text.translatable("lambdamc.voucher.redeemed", LambdaItems.formatAmount(amount)), false);
		player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP,
				SoundCategory.PLAYERS, 0.6f, 1.2f);

		stack.decrement(1);
		return TypedActionResult.success(stack, false);
	}
}
