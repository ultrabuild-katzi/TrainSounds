package de.ultrabuild.trainsounds.network.handler;

import de.ultrabuild.trainsounds.logic.EngineToggleCarrier;
import net.minecraft.entity.Entity;

public class ToggleCarriageEngineHandler {

    public static void toggleCarriage(int entityId, Entity contextEntity) {
        Entity entity = contextEntity.getWorld().getEntityById(entityId);
        if (entity instanceof EngineToggleCarrier carrier) {
            carrier.trainsounds$toggleEngineBuiltIn();
        }
    }
}

