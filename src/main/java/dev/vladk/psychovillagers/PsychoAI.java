package dev.vladk.psychovillagers;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Server-side brain of the psychos. Psychos behave like normal villagers during the day.
 * At night they look for a vulnerable victim, pull out a weapon, stalk it and strike.
 */
public final class PsychoAI {
	private static final Set<Villager> LOADED_PSYCHOS = Collections.newSetFromMap(new WeakHashMap<>());
	private static final Map<Villager, Hunt> HUNTS = new WeakHashMap<>();

	private static final int SCAN_INTERVAL = 20;
	private static final int MAX_HUNT_TICKS = 20 * 45;
	private static final double STRIKE_DISTANCE_SQR = 2.2 * 2.2;
	private static final float HUNT_SPEED = 0.7F;

	private static final class Hunt {
		final WeakReference<LivingEntity> target;
		int ticks;
		int cooldown;
		boolean gaveWeapon;

		Hunt(LivingEntity target) {
			this.target = new WeakReference<>(target);
		}
	}

	private PsychoAI() {}

	// ---------------------------------------------------------------- lifecycle

	public static void onEntityLoad(Entity entity, ServerLevel level) {
		if (!(entity instanceof Villager villager)) return;

		if (!PsychoData.isRolled(villager)) {
			boolean psycho = level.getRandom().nextDouble() < ModConfig.INSTANCE.psychoChance;
			PsychoData.setPsycho(villager, psycho);
		}

		if (PsychoData.isPsycho(villager)) {
			LOADED_PSYCHOS.add(villager);
		}
	}

	public static void onEntityUnload(Entity entity, ServerLevel level) {
		if (entity instanceof Villager villager) {
			LOADED_PSYCHOS.remove(villager);
			Hunt hunt = HUNTS.remove(villager);
			if (hunt != null) putAwayWeapon(villager, hunt);
		}
	}

	/** Called by the spawn command and when a villager is turned into a psycho at runtime. */
	public static void track(Villager villager) {
		if (PsychoData.isPsycho(villager)) LOADED_PSYCHOS.add(villager);
	}

	// ---------------------------------------------------------------- ticking

	public static void onLevelTick(ServerLevel level) {
		if (LOADED_PSYCHOS.isEmpty()) return;

		List<Villager> psychos = new ArrayList<>(LOADED_PSYCHOS);
		for (Villager villager : psychos) {
			if (villager.level() != level) continue;
			if (!villager.isAlive() || villager.isRemoved()) {
				LOADED_PSYCHOS.remove(villager);
				HUNTS.remove(villager);
				continue;
			}
			if (villager.isBaby()) continue; // kids are innocent... for now
			tickPsycho(level, villager);
		}
	}

	private static void tickPsycho(ServerLevel level, Villager villager) {
		ModConfig.Values cfg = ModConfig.INSTANCE;
		boolean canHunt = !cfg.onlyAtNight || level.isDarkOutside();
		Hunt hunt = HUNTS.get(villager);

		if (hunt == null) {
			if (!canHunt) return;
			if ((level.getGameTime() + villager.getId()) % SCAN_INTERVAL != 0) return;

			LivingEntity victim = findVictim(level, villager);
			if (victim == null) return;

			hunt = new Hunt(victim);
			HUNTS.put(villager, hunt);
			if (villager.isSleeping()) villager.stopSleeping();
			if (cfg.showWeapon && villager.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
				villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
				hunt.gaveWeapon = true;
			}
		}

		LivingEntity target = hunt.target.get();
		hunt.ticks++;
		if (hunt.cooldown > 0) hunt.cooldown--;

		double maxDist = cfg.huntRadius * 1.6;
		if (!canHunt
				|| target == null
				|| !target.isAlive()
				|| target.isRemoved()
				|| target.level() != level
				|| hunt.ticks > MAX_HUNT_TICKS
				|| villager.distanceToSqr(target) > maxDist * maxDist
				|| (target instanceof Player p && (p.isCreative() || p.isSpectator()))) {
			stopHunt(villager, hunt);
			return;
		}

		// Keep walking towards the victim; the villager brain does the pathfinding (and opens doors).
		if (hunt.ticks % 5 == 1) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, HUNT_SPEED, 1));
		}

		if (cfg.horrorSounds && target instanceof Player && hunt.ticks % 40 == 0) {
			level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 0.8F, 0.7F);
		}

		if (hunt.cooldown == 0 && villager.distanceToSqr(target) <= STRIKE_DISTANCE_SQR) {
			strike(level, villager, target, hunt);
		}
	}

	private static void strike(ServerLevel level, Villager villager, LivingEntity target, Hunt hunt) {
		ModConfig.Values cfg = ModConfig.INSTANCE;
		hunt.cooldown = cfg.attackCooldownTicks;

		boolean hurt = target.hurtServer(level, level.damageSources().mobAttack(villager), cfg.attackDamage);
		if (!hurt) return;

		BlockPos pos = target.blockPosition();
		level.playSound(null, pos, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.0F, 0.6F);

		if (!target.isAlive()) {
			PsychoData.addKill(villager);
			if (cfg.horrorSounds) {
				level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.HOSTILE, 1.0F, 0.5F);
			}
			stopHunt(villager, hunt);
		}
	}

	private static void stopHunt(Villager villager, Hunt hunt) {
		HUNTS.remove(villager);
		putAwayWeapon(villager, hunt);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
	}

	private static void putAwayWeapon(Villager villager, Hunt hunt) {
		if (hunt.gaveWeapon && villager.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.IRON_SWORD)) {
			villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		}
	}

	// ---------------------------------------------------------------- victim selection

	private static LivingEntity findVictim(ServerLevel level, Villager psycho) {
		ModConfig.Values cfg = ModConfig.INSTANCE;
		AABB area = psycho.getBoundingBox().inflate(cfg.huntRadius, 6, cfg.huntRadius);

		LivingEntity best = null;
		double bestDist = Double.MAX_VALUE;

		for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != psycho && e.isAlive())) {
			boolean vulnerable;
			if (candidate instanceof Player player) {
				vulnerable = cfg.attackPlayers && isVulnerablePlayer(level, player);
			} else if (candidate instanceof Villager other) {
				vulnerable = cfg.attackVillagers && isVulnerableVillager(level, psycho, other);
			} else {
				vulnerable = false;
			}
			if (!vulnerable) continue;

			double dist = psycho.distanceToSqr(candidate);
			if (dist < bestDist) {
				bestDist = dist;
				best = candidate;
			}
		}
		return best;
	}

	private static boolean isVulnerablePlayer(ServerLevel level, Player player) {
		if (player.isCreative() || player.isSpectator()) return false;
		if (player.isSleeping()) return true;
		if (player.getHealth() <= player.getMaxHealth() * 0.5F) return true;

		// Alone in the dark: no golem and no other player nearby.
		AABB around = player.getBoundingBox().inflate(16);
		boolean golemNear = !level.getEntitiesOfClass(IronGolem.class, around, IronGolem::isAlive).isEmpty();
		boolean friendNear = !level.getEntitiesOfClass(Player.class, around, p -> p != player && !p.isSpectator()).isEmpty();
		return !golemNear && !friendNear;
	}

	private static boolean isVulnerableVillager(ServerLevel level, Villager psycho, Villager victim) {
		if (PsychoData.isPsycho(victim)) return false; // psychos don't hunt each other

		// Never kill in front of witnesses.
		AABB witnessArea = victim.getBoundingBox().inflate(12);
		if (!level.getEntitiesOfClass(Player.class, witnessArea, p -> !p.isSpectator()).isEmpty()) return false;
		if (!level.getEntitiesOfClass(IronGolem.class, victim.getBoundingBox().inflate(14), IronGolem::isAlive).isEmpty()) return false;

		if (victim.isBaby() || victim.isSleeping()) return true;

		// Lonely villager: nobody else around except the psycho.
		AABB close = victim.getBoundingBox().inflate(8);
		return level.getEntitiesOfClass(Villager.class, close, v -> v != victim && v != psycho && v.isAlive()).isEmpty();
	}
}
