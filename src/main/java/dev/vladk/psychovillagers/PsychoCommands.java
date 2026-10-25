package dev.vladk.psychovillagers;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * /psychovillagers spawn   - spawn a psycho villager at your position
 * /psychovillagers reveal  - make psychos within 64 blocks glow for 10 seconds
 * /psychovillagers reload  - reload config/psychovillagers.json
 */
public final class PsychoCommands {
	private PsychoCommands() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection selection) {
		dispatcher.register(Commands.literal("psychovillagers")
				.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
				.then(Commands.literal("spawn").executes(PsychoCommands::spawn))
				.then(Commands.literal("reveal").executes(PsychoCommands::reveal))
				.then(Commands.literal("reload").executes(PsychoCommands::reload)));
	}

	private static int spawn(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack source = ctx.getSource();
		ServerLevel level = source.getLevel();
		Vec3 pos = source.getPosition();

		Villager villager = new Villager(EntityTypes.VILLAGER, level);
		villager.snapTo(pos);
		PsychoData.setPsycho(villager, true);
		level.addFreshEntity(villager);
		PsychoAI.track(villager);

		source.sendSuccess(() -> Component.translatable("psychovillagers.command.spawned"), true);
		return 1;
	}

	private static int reveal(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack source = ctx.getSource();
		ServerLevel level = source.getLevel();
		AABB area = AABB.ofSize(source.getPosition(), 128, 128, 128);

		List<Villager> psychos = level.getEntitiesOfClass(Villager.class, area, PsychoData::isPsycho);
		for (Villager villager : psychos) {
			villager.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 10));
		}

		int count = psychos.size();
		source.sendSuccess(() -> Component.translatable("psychovillagers.command.revealed", count), false);
		return count;
	}

	private static int reload(CommandContext<CommandSourceStack> ctx) {
		ModConfig.load();
		ctx.getSource().sendSuccess(() -> Component.translatable("psychovillagers.command.reloaded"), true);
		return 1;
	}
}
