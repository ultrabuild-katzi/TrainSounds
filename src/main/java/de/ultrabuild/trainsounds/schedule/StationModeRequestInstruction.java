package de.ultrabuild.trainsounds.schedule;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TrainIconType;
import com.simibubi.create.content.trains.graph.DiscoveredPath;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import de.ultrabuild.trainsounds.Trainsounds;
import net.createmod.catnip.data.Pair;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.List;

public class StationModeRequestInstruction extends DestinationInstruction {

    public StationModeRequestInstruction() {
        data.putInt("Value", 5);
        data.putInt("Mode", TrainMode.ELECTRIC.ordinal());
    }

    @Override
    public Pair<ItemStack, Text> getSummary() {
        return Pair.of(AllBlocks.TRACK_STATION.asStack(), Text.literal(getFilter()));
    }

    @Override
    public Identifier getId() {
        return Identifier.of(Trainsounds.MOD_ID, "station_mode_request");
    }

    @Override
    public boolean supportsConditions() {
        return false;
    }

    @Override
    public List<Text> getTitleAs(String type) {
        return ImmutableList.of(
            Text.translatable("schedule.instruction.station_mode_request.scheduled"),
            modeLabel().copy().formatted(Formatting.AQUA)
                .append(Text.literal(" ").formatted(Formatting.GRAY))
                .append(Text.literal(getFilter()).formatted(Formatting.WHITE))
                .append(Text.literal(" ").formatted(Formatting.GRAY))
                .append(formatTime())
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
        builder.addScrollInput(126, 31, (input, label) -> {
            input.withRange(1, 121)
                .withShiftStep(15)
                .titled(Text.translatable("schedule.instruction.station_mode_request.delay"));
            label.withSuffix("s");
        }, "Value");
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean renderSpecialIcon(DrawContext graphics, int x, int y) {
        TrainMode mode = getMode();
        TrainIconType.byId(Create.asResource(mode.iconId())).render(TrainIconType.ENGINE, graphics, x, y);
        return true;
    }

    @Override
    public List<Text> getSecondLineTooltip(int slot) {
        return ImmutableList.of(
            Text.translatable("schedule.instruction.station_mode_request.tooltip"),
            Text.translatable("schedule.instruction.station_mode_request.tooltip_1")
                .formatted(Formatting.GRAY)
        );
    }

    public void applyMode(World level, Train train) {
        TrainMode mode = getMode();
        Identifier iconId = Create.asResource(mode.iconId());
        train.icon = TrainIconType.byId(iconId);

        if (level.getServer() != null) {
            com.simibubi.create.AllPackets.getChannel().sendToClientsInServer(
                new com.simibubi.create.content.trains.station.TrainEditPacket.TrainEditReturnPacket(train.id, "", iconId, 0),
                level.getServer()
            );
        }
    }

    public int totalWaitTicks() {
        return getValue() * 20;
    }

    public int getValue() {
        return intData("Value");
    }

    public void cycleMode(int delta) {
        int size = TrainMode.values().length;
        int next = Math.floorMod(data.getInt("Mode") + delta, size);
        data.putInt("Mode", next);
    }

    public TrainMode getMode() {
        return TrainMode.values()[Math.floorMod(data.getInt("Mode"), TrainMode.values().length)];
    }

    private Text formatTime() {
        return Text.literal(getValue() + "s").formatted(Formatting.WHITE);
    }

    private Text modeLabel() {
        return Text.translatable("schedule.instruction.station_mode_request.mode." + getMode().name().toLowerCase());
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
    }
}
