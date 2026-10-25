package dev.vladk.psychovillagers;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

import java.util.UUID;

/**
 * Right-click a villager with the Detective's Notebook to write down an observation.
 * Psychos usually leave clues, but innocents occasionally look suspicious too.
 * The observation for a villager is stable (based on its UUID), so asking again won't help.
 */
public final class NotebookHandler {
	private static final int SUSPICIOUS_CLUES = 6;
	private static final int INNOCENT_CLUES = 6;

	private NotebookHandler() {}

	public static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity entity, EntityHitResult hitResult) {
		if (!(entity instanceof Villager villager)) return InteractionResult.PASS;
		if (!player.getItemInHand(hand).is(ModItems.NOTEBOOK)) return InteractionResult.PASS;
		if (player.isSpectator()) return InteractionResult.PASS;

		if (player instanceof ServerPlayer serverPlayer) {
			writeObservation(serverPlayer, villager);
		}
		// SUCCESS on both sides so the trading screen does not open.
		return InteractionResult.SUCCESS;
	}

	private static void writeObservation(ServerPlayer player, Villager villager) {
		ModConfig.Values cfg = ModConfig.INSTANCE;
		boolean psycho = PsychoData.isPsycho(villager);
		int kills = PsychoData.getKills(villager);

		UUID uuid = villager.getUUID();
		long seed = uuid.getMostSignificantBits() ^ (uuid.getLeastSignificantBits() * 31L);
		double roll = (Math.floorMod(seed, 10_000L)) / 10_000.0;
		int variant = (int) Math.floorMod(seed >>> 17, 1_000L);

		Component name = villager.getDisplayName();
		Component line;

		if (psycho && kills > 0) {
			line = Component.translatable("psychovillagers.clue.blood", name).withStyle(ChatFormatting.DARK_RED);
		} else {
			boolean suspicious = psycho ? roll < cfg.trueClueChance : roll < cfg.falseClueChance;
			if (suspicious) {
				line = Component.translatable("psychovillagers.clue.suspicious." + (variant % SUSPICIOUS_CLUES), name)
						.withStyle(ChatFormatting.RED);
			} else {
				line = Component.translatable("psychovillagers.clue.innocent." + (variant % INNOCENT_CLUES), name)
						.withStyle(ChatFormatting.GRAY);
			}
		}

		player.sendSystemMessage(Component.translatable("psychovillagers.notebook.header").withStyle(ChatFormatting.GOLD)
				.append(Component.literal(" ")).append(line));
		player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
	}
}
