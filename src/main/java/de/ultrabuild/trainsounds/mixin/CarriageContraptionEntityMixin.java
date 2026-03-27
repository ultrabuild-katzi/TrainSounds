package de.ultrabuild.trainsounds.mixin;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import de.ultrabuild.trainsounds.logic.EngineToggleCarrier;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CarriageContraptionEntity.class)
public abstract class CarriageContraptionEntityMixin implements EngineToggleCarrier {

    @Unique
    private static final String TRAINSOUNDS_ENGINE_NBT_KEY = "TrainSoundsEngineBuiltIn";

    @Unique
    private static final TrackedData<Boolean> TRAINSOUNDS_ENGINE_BUILT_IN =
            DataTracker.registerData(CarriageContraptionEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    @Unique
    private boolean trainsounds$engineStateLoadedFromNbt = false;

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void trainsounds$initEngineTracker(CallbackInfo ci) {
        CarriageContraptionEntity self = (CarriageContraptionEntity) (Object) this;
        self.getDataTracker().startTracking(TRAINSOUNDS_ENGINE_BUILT_IN, false);
    }

    @Inject(method = "setCarriage", at = @At("TAIL"))
    private void trainsounds$applyDefaultEngineState(Carriage carriage, CallbackInfo ci) {
        if (trainsounds$engineStateLoadedFromNbt) {
            return;
        }
        CarriageContraptionEntity self = (CarriageContraptionEntity) (Object) this;
        trainsounds$setEngineBuiltIn(self.carriageIndex == 0);
    }

    @Inject(method = "writeAdditional", at = @At("TAIL"))
    private void trainsounds$writeEngineState(NbtCompound compound, boolean spawnPacket, CallbackInfo ci) {
        compound.putBoolean(TRAINSOUNDS_ENGINE_NBT_KEY, trainsounds$isEngineBuiltIn());
    }

    @Inject(method = "readAdditional", at = @At("TAIL"))
    private void trainsounds$readEngineState(NbtCompound compound, boolean spawnPacket, CallbackInfo ci) {
        CarriageContraptionEntity self = (CarriageContraptionEntity) (Object) this;

        if (compound.contains(TRAINSOUNDS_ENGINE_NBT_KEY)) {
            trainsounds$setEngineBuiltIn(compound.getBoolean(TRAINSOUNDS_ENGINE_NBT_KEY));
            trainsounds$engineStateLoadedFromNbt = true;
            return;
        }

        trainsounds$setEngineBuiltIn(self.carriageIndex == 0);
    }

    @Override
    public boolean trainsounds$isEngineBuiltIn() {
        CarriageContraptionEntity self = (CarriageContraptionEntity) (Object) this;
        return self.getDataTracker().get(TRAINSOUNDS_ENGINE_BUILT_IN);
    }

    @Override
    public void trainsounds$setEngineBuiltIn(boolean enabled) {
        CarriageContraptionEntity self = (CarriageContraptionEntity) (Object) this;
        self.getDataTracker().set(TRAINSOUNDS_ENGINE_BUILT_IN, enabled);
    }
}

