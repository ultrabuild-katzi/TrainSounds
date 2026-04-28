package de.ultrabuild.trainsounds.network.packet;

import net.minecraft.network.PacketByteBuf;

public class OpenCarriageGuiS2CPacket {

    private final int carriageEntityId;

    public OpenCarriageGuiS2CPacket(int carriageEntityId) {
        this.carriageEntityId = carriageEntityId;
    }

    public OpenCarriageGuiS2CPacket(PacketByteBuf buf) {
        this.carriageEntityId = buf.readInt();
    }

    public void write(PacketByteBuf buf) {
        buf.writeInt(carriageEntityId);
    }

    public int getCarriageEntityId() {
        return carriageEntityId;
    }
}

