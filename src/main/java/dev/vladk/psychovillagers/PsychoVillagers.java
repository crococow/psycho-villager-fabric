package dev.vladk.psychovillagers;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PsychoVillagers implements ModInitializer {
	public static final String MOD_ID = "psychovillagers";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModConfig.load();
		PsychoData.init();
		ModItems.init();

		ServerEntityEvents.ENTITY_LOAD.register(PsychoAI::onEntityLoad);
		ServerEntityEvents.ENTITY_UNLOAD.register(PsychoAI::onEntityUnload);
		ServerTickEvents.END_LEVEL_TICK.register(PsychoAI::onLevelTick);
		UseEntityCallback.EVENT.register(NotebookHandler::onUseEntity);
		CommandRegistrationCallback.EVENT.register(PsychoCommands::register);

		LOGGER.info("Psycho Villagers loaded. Sleep with one eye open.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
