package de.ultrabuild.trainsounds.mixin;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import de.ultrabuild.trainsounds.schedule.StationModeRequestInstruction;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScheduleRuntime.class)
public abstract class ScheduleRuntimeMixin {

    @Shadow
    public Train train;

    @Shadow
    public Schedule schedule;

    @Shadow
    public int currentEntry;

    @Shadow
    public ScheduleRuntime.State state;

    @Shadow
    public boolean displayLinkUpdateRequested;

    @Unique
    private int trainsounds$modeWaitTicks;

    @Unique
    private int trainsounds$modeWaitEntry = -1;

    @Inject(method = "destinationReached", at = @At("TAIL"))
    private void trainsounds$prepareModeWait(CallbackInfo ci) {
        StationModeRequestInstruction request = trainsounds$currentRequest();
        if (request == null) {
            trainsounds$modeWaitEntry = -1;
            trainsounds$modeWaitTicks = 0;
            return;
        }

        trainsounds$modeWaitEntry = currentEntry;
        trainsounds$modeWaitTicks = 0;
    }

    @Inject(method = "tickConditions", at = @At("HEAD"), cancellable = true)
    private void trainsounds$handleModeWait(World level, CallbackInfo ci) {
        StationModeRequestInstruction request = trainsounds$currentRequest();
        if (request == null || state != ScheduleRuntime.State.POST_TRANSIT) {
            return;
        }

        if (trainsounds$modeWaitEntry != currentEntry) {
            trainsounds$modeWaitEntry = currentEntry;
            trainsounds$modeWaitTicks = 0;
        }

        if (trainsounds$modeWaitTicks < request.totalWaitTicks()) {
            trainsounds$modeWaitTicks++;
            displayLinkUpdateRequested = true;
            ci.cancel();
            return;
        }

        request.applyMode(level, train);
        state = ScheduleRuntime.State.PRE_TRANSIT;
        currentEntry++;
        displayLinkUpdateRequested = true;
        trainsounds$modeWaitEntry = -1;
        trainsounds$modeWaitTicks = 0;
        ci.cancel();
    }

    @Inject(method = "estimateStayDuration", at = @At("HEAD"), cancellable = true)
    private void trainsounds$estimateModeWait(int index, CallbackInfoReturnable<Integer> cir) {
        if (schedule == null || index < 0 || index >= schedule.entries.size()) {
            return;
        }

        if (schedule.entries.get(index).instruction instanceof StationModeRequestInstruction request) {
            cir.setReturnValue(request.totalWaitTicks());
        }
    }

    @Inject(method = "getWaitingStatus", at = @At("HEAD"), cancellable = true)
    private void trainsounds$modeWaitingStatus(World level, CallbackInfoReturnable<Text> cir) {
        StationModeRequestInstruction request = trainsounds$currentRequest();
        if (request == null || state != ScheduleRuntime.State.POST_TRANSIT) {
            return;
        }

        int elapsedSeconds = trainsounds$modeWaitTicks / 20;
        cir.setReturnValue(Text.translatable(
            "schedule.instruction.station_mode_request.status",
            Text.translatable("schedule.instruction.station_mode_request.mode." + request.getMode().name().toLowerCase()),
            Text.literal(elapsedSeconds + "s/" + request.getValue() + "s")
        ));
    }

    @Unique
    private StationModeRequestInstruction trainsounds$currentRequest() {
        if (schedule == null || currentEntry < 0 || currentEntry >= schedule.entries.size()) {
            return null;
        }

        if (schedule.entries.get(currentEntry).instruction instanceof StationModeRequestInstruction request) {
            return request;
        }

        return null;
    }
}
