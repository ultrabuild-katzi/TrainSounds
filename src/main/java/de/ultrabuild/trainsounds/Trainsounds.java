package de.ultrabuild.trainsounds;

import de.ultrabuild.trainsounds.item.EngineToggleItem;
import de.ultrabuild.trainsounds.logic.TrainEngineToggleHandler;
import de.ultrabuild.trainsounds.network.TrainSoundsNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class Trainsounds implements ModInitializer {

    public static final String MOD_ID = "trainsounds";
    public static final Identifier ELECTRIC_SOUND_ID = Identifier.of(MOD_ID, "electric");
    public static final SoundEvent ELECTRIC_SOUND_EVENT = SoundEvent.of(ELECTRIC_SOUND_ID);
    public static final Identifier DIESEL_SOUND_ID = Identifier.of(MOD_ID, "diesel");
    public static final SoundEvent DIESEL_SOUND_EVENT = SoundEvent.of(DIESEL_SOUND_ID);
    public static final Identifier ENGINE_TOGGLE_ITEM_ID = Identifier.of(MOD_ID, "engine_toggle_tool");
    public static final Item ENGINE_TOGGLE_ITEM = new EngineToggleItem(new Item.Settings().maxCount(1));

    @Override
    public void onInitialize() {
        // Keep explicit registration in code for future runtime sound logic.
        Registry.register(Registries.SOUND_EVENT, ELECTRIC_SOUND_ID, ELECTRIC_SOUND_EVENT);
        Registry.register(Registries.SOUND_EVENT, DIESEL_SOUND_ID, DIESEL_SOUND_EVENT);
        Registry.register(Registries.ITEM, ENGINE_TOGGLE_ITEM_ID, ENGINE_TOGGLE_ITEM);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(ENGINE_TOGGLE_ITEM));
        TrainEngineToggleHandler.register();
        TrainSoundsNetworking.register();
    }
}
