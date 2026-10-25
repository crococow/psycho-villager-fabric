package dev.vladk.psychovillagers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Simple JSON config stored in config/psychovillagers.json. */
public final class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("psychovillagers.json");

	public static Values INSTANCE = new Values();

	public static final class Values {
		/** Chance (0..1) that a newly seen villager is a hidden psycho. */
		public double psychoChance = 0.10;
		/** Psychos only hunt while it is dark outside. */
		public boolean onlyAtNight = true;
		/** Damage dealt per stab (2 = one heart). */
		public float attackDamage = 5.0F;
		/** Ticks between stabs. */
		public int attackCooldownTicks = 30;
		/** Radius in blocks a psycho scans for victims. */
		public int huntRadius = 20;
		/** Psychos may attack players (sleeping, wounded or alone in the dark). */
		public boolean attackPlayers = true;
		/** Psychos may attack lonely or baby villagers. */
		public boolean attackVillagers = true;
		/** Psycho pulls out a sword while hunting. */
		public boolean showWeapon = true;
		/** Heartbeat and other creepy sounds while being hunted. */
		public boolean horrorSounds = true;
		/** Chance that the notebook shows a false clue on an innocent villager. */
		public double falseClueChance = 0.10;
		/** Chance that the notebook shows a real clue on a psycho. */
		public double trueClueChance = 0.75;
	}

	private ModConfig() {}

	public static void load() {
		try {
			if (Files.exists(PATH)) {
				try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
					Values loaded = GSON.fromJson(reader, Values.class);
					if (loaded != null) INSTANCE = loaded;
				}
			}
			save();
		} catch (Exception e) {
			PsychoVillagers.LOGGER.error("Failed to load config, using defaults", e);
			INSTANCE = new Values();
		}
	}

	public static void save() {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(INSTANCE, writer);
			}
		} catch (IOException e) {
			PsychoVillagers.LOGGER.error("Failed to save config", e);
		}
	}
}
