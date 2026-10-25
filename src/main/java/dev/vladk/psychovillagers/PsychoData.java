package dev.vladk.psychovillagers;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.npc.villager.Villager;

/** Persistent per-villager data, saved with the entity via Fabric Data Attachments. */
public final class PsychoData {
	/** Absent = not rolled yet, true = psycho, false = innocent. */
	public static final AttachmentType<Boolean> PSYCHO =
			AttachmentRegistry.createPersistent(PsychoVillagers.id("psycho"), Codec.BOOL);

	/** Number of victims this villager has killed. */
	public static final AttachmentType<Integer> KILLS =
			AttachmentRegistry.createPersistent(PsychoVillagers.id("kills"), Codec.INT);

	private PsychoData() {}

	public static void init() {
		// Class loading registers the attachment types.
	}

	public static boolean isRolled(Villager villager) {
		return villager.hasAttached(PSYCHO);
	}

	public static boolean isPsycho(Villager villager) {
		return Boolean.TRUE.equals(villager.getAttached(PSYCHO));
	}

	public static void setPsycho(Villager villager, boolean psycho) {
		villager.setAttached(PSYCHO, psycho);
	}

	public static int getKills(Villager villager) {
		return villager.getAttachedOrElse(KILLS, 0);
	}

	public static void addKill(Villager villager) {
		villager.setAttached(KILLS, getKills(villager) + 1);
	}
}
