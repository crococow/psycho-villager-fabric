package dev.vladk.psychovillagers;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModItems {
	public static final ResourceKey<Item> NOTEBOOK_KEY =
			ResourceKey.create(Registries.ITEM, PsychoVillagers.id("notebook"));

	public static final Item NOTEBOOK = Registry.register(
			BuiltInRegistries.ITEM,
			NOTEBOOK_KEY,
			new Item(new Item.Properties().setId(NOTEBOOK_KEY).stacksTo(1))
	);

	private ModItems() {}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
				.register(output -> output.accept(NOTEBOOK));
	}
}
