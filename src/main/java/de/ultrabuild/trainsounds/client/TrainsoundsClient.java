package de.ultrabuild.trainsounds.client;

import com.simibubi.create.AllSoundEvents;
import de.ultrabuild.trainsounds.Trainsounds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public class TrainsoundsClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("TrainSounds/Client");

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> overrideCreateSteamSound());
    }

    private static void overrideCreateSteamSound() {
        try {
            Object steamEntry = AllSoundEvents.STEAM;
            Field eventField = steamEntry.getClass().getDeclaredField("event");
            eventField.setAccessible(true);
            eventField.set(steamEntry, Trainsounds.ELECTRIC_SOUND_EVENT);
            LOGGER.info("Replaced Create steam sound event with {}", Trainsounds.ELECTRIC_SOUND_ID);
        } catch (ReflectiveOperationException exception) {
            LOGGER.error("Failed to replace Create steam sound event", exception);
        }
    }
}

