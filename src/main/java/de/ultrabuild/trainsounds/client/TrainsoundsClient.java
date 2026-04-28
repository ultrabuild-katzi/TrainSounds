package de.ultrabuild.trainsounds.client;

import de.ultrabuild.trainsounds.client.config.TrainSoundVolumeConfigManager;
import net.fabricmc.api.ClientModInitializer;

public class TrainsoundsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        TrainSoundVolumeConfigManager.ensureLoaded();
    }
}
