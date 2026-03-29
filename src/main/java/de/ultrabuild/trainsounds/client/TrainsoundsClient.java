package de.ultrabuild.trainsounds.client;

import net.fabricmc.api.ClientModInitializer;

public class TrainsoundsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Engine sound routing is handled per carriage in CarriageSoundsMixin.
    }
}
