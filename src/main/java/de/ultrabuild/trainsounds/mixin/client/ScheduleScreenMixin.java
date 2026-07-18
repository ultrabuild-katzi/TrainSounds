package de.ultrabuild.trainsounds.mixin.client;

import com.simibubi.create.content.trains.schedule.ScheduleScreen;
import de.ultrabuild.trainsounds.schedule.StationModeRequestInstruction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScheduleScreen.class)
public abstract class ScheduleScreenMixin {

    @Shadow
    private com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction editingDestination;

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void trainsounds$scrollStationMode(double mouseX, double mouseY, double amount, CallbackInfoReturnable<Boolean> cir) {
        if (!(editingDestination instanceof StationModeRequestInstruction stationModeRequestInstruction)) {
            return;
        }

        int width = ((ScreenAccessor) (Object) this).trainsounds$getWidth();
        int height = ((ScreenAccessor) (Object) this).trainsounds$getHeight();
        int left = (width - 256) / 2 + 54;
        int top = (height - 226) / 2 + 88;
        if (mouseX < left || mouseX > left + 18 || mouseY < top || mouseY > top + 18) {
            return;
        }

        if (amount > 0) {
            stationModeRequestInstruction.cycleMode(1);
        } else if (amount < 0) {
            stationModeRequestInstruction.cycleMode(-1);
        }

        cir.setReturnValue(true);
    }
}
