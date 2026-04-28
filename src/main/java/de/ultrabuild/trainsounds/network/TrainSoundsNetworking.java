package de.ultrabuild.trainsounds.network;

import de.ultrabuild.trainsounds.Trainsounds;
import de.ultrabuild.trainsounds.logic.EngineToggleCarrier;
import de.ultrabuild.trainsounds.network.packet.OpenCarriageGuiS2CPacket;
import de.ultrabuild.trainsounds.network.packet.ToggleCarriageEngineC2SPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

public class TrainSoundsNetworking {

    public static final Identifier OPEN_CARRIAGE_GUI = Identifier.of(Trainsounds.MOD_ID, "open_carriage_gui");
    public static final Identifier TOGGLE_ENGINE = Identifier.of(Trainsounds.MOD_ID, "toggle_engine");

    public static void register() {
        // C2S: Toggle Engine
        assert TOGGLE_ENGINE != null;
        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_ENGINE, (server, player, handler, buf, responseSender) -> {
            ToggleCarriageEngineC2SPacket packet = new ToggleCarriageEngineC2SPacket(buf);
            Entity entity = player.getWorld().getEntityById(packet.getEntityId());
            if (entity instanceof EngineToggleCarrier carrier) {
                carrier.trainsounds$toggleEngineBuiltIn();
            }
        });

        // S2C: Open GUI (just define here for reference)
        // The packet handling is client-side only
    }

    public static void sendOpenCarriageGuiPacket(net.minecraft.server.network.ServerPlayerEntity player, com.simibubi.create.content.trains.entity.CarriageContraptionEntity carriage) {
        OpenCarriageGuiS2CPacket packet = new OpenCarriageGuiS2CPacket(carriage.getId());
        net.minecraft.network.PacketByteBuf buf = new net.minecraft.network.PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        packet.write(buf);
        ServerPlayNetworking.send(player, OPEN_CARRIAGE_GUI, buf);
    }
}

