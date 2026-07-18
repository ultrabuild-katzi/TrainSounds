package de.ultrabuild.trainsounds.schedule;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPackets;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TrainIconType;
import com.simibubi.create.content.trains.schedule.condition.TimedWaitCondition;
import com.simibubi.create.content.trains.station.TrainEditPacket;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import net.createmod.catnip.data.Pair;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.List;

public class StationModeCondition extends TimedWaitCondition {

    public StationModeCondition() {
        data.putInt("Value", 5);
        data.putInt("TimeUnit", 1);
        data.putInt("Mode", TrainMode.ELECTRIC.ordinal());
    }

    @Override
    public Pair<ItemStack, Text> getSummary() {
        return Pair.of(AllBlocks.TRACK_STATION.asStack(), modeLabel());
    }

    @Override
    public Identifier getId() {
        return Create.asResource("station_mode");
    }

    @Override
    public List<Text> getTitleAs(String type) {
        return ImmutableList.of(
            Text.translatable("schedule.condition.station_mode.scheduled"),
            modeLabel().copy().formatted(Formatting.AQUA)
                .append(Text.literal(" ").formatted(Formatting.GRAY))
                .append(formatTime(false))
        );
    }

    @Override
    public ItemStack getSecondLineIcon() {
        return AllBlocks.TRACK_STATION.asStack();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
        super.initConfigurationWidgets(builder);
        builder.addSelectionScrollInput(126, 72, (input, label) -> {
            input.forOptions(TrainMode.translatedOptions())
                .calling(index -> data.putInt("Mode", index))
                .titled(Text.translatable("schedule.condition.station_mode.mode"));
        }, "Mode");
    }

    public void cycleMode(int delta) {
        int size = TrainMode.values().length;
        int next = Math.floorMod(data.getInt("Mode") + delta, size);
        data.putInt("Mode", next);
    }

    @Override
    public boolean tickCompletion(World level, Train train, net.minecraft.nbt.NbtCompound context) {
        if (train.getCurrentStation() == null) {
            return false;
        }

        if (!context.contains("Applied")) {
            applyMode(level, train);
            context.putBoolean("Applied", true);
        }

        int time = context.getInt("Time");
        if (time >= totalWaitTicks()) {
            return true;
        }

        context.putInt("Time", time + 1);
        requestDisplayIfNecessary(context, time);
        return false;
    }

    @Override
    public MutableText getWaitingStatus(World level, Train train, net.minecraft.nbt.NbtCompound tag) {
        return Text.translatable(
            "schedule.condition.station_mode.status",
            modeLabel().copy().formatted(Formatting.AQUA),
            formatTime(false)
        );
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean renderSpecialIcon(DrawContext graphics, int x, int y) {
        TrainMode mode = getMode();
        TrainIconType.byId(Create.asResource(mode.iconId())).render(TrainIconType.ENGINE, graphics, x, y);
        return true;
    }

    private void applyMode(World level, Train train) {
        TrainMode mode = getMode();
        Identifier iconId = Create.asResource(mode.iconId());
        train.icon = TrainIconType.byId(iconId);

        MinecraftServer server = level.getServer();
        if (server != null) {
            AllPackets.getChannel().sendToClientsInServer(
                new TrainEditPacket.TrainEditReturnPacket(train.id, "", iconId, 0),
                server
            );
        }
    }

    private TrainMode getMode() {
        return TrainMode.values()[Math.floorMod(data.getInt("Mode"), TrainMode.values().length)];
    }

    private MutableText modeLabel() {
        return Text.translatable("schedule.condition.station_mode.mode." + getMode().name().toLowerCase());
    }

    public enum TrainMode {
        TRADITIONAL("traditional"),
        ELECTRIC("electric"),
        MODERN("modern");

        private final String iconId;

        TrainMode(String iconId) {
            this.iconId = iconId;
        }

        public String iconId() {
            return iconId;
        }

        public static List<Text> translatedOptions() {
            return ImmutableList.of(
                Text.translatable("schedule.condition.station_mode.mode.traditional"),
                Text.translatable("schedule.condition.station_mode.mode.electric"),
                Text.translatable("schedule.condition.station_mode.mode.modern")
            );
        }
    }
}
