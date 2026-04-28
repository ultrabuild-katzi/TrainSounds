package de.ultrabuild.trainsounds.network.packet;

import de.ultrabuild.trainsounds.logic.EngineToggleCarrier;
import net.minecraft.network.PacketByteBuf;

public class ToggleCarriageEngineC2SPacket {

    private final int entityId;

    public ToggleCarriageEngineC2SPacket(int entityId) {
        this.entityId = entityId;
    }

    public ToggleCarriageEngineC2SPacket(PacketByteBuf buf) {
        this.entityId = buf.readInt();
    }

    public void write(PacketByteBuf buf) {
        buf.writeInt(entityId);
    }

    public int getEntityId() {
        return entityId;
    }
}

