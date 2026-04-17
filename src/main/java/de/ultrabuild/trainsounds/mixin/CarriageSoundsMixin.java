package de.ultrabuild.trainsounds.mixin;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageSounds;
import de.mrjulsen.paw.blockentity.PantographBlockEntity;
import de.ultrabuild.trainsounds.Trainsounds;
import de.ultrabuild.trainsounds.client.config.TrainSoundVolumeConfigManager;
import de.ultrabuild.trainsounds.logic.EngineToggleCarrier;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CarriageSounds.class)
public abstract class CarriageSoundsMixin {

    @Shadow
    CarriageContraptionEntity entity;

    @ModifyArg(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/entity/CarriageSounds;playIfMissing(Lnet/minecraft/client/MinecraftClient;Lcom/simibubi/create/content/trains/entity/CarriageSounds$LoopingSound;Lnet/minecraft/sound/SoundEvent;)Lcom/simibubi/create/content/trains/entity/CarriageSounds$LoopingSound;",
                    ordinal = 0
            ),
            index = 2
    )
    private SoundEvent trainsounds$muteMinecartLoop(SoundEvent original) {
        if (!trainsounds$shouldUseCustomEngineSound(entity)) {
            return original;
        }

        return SoundEvents.INTENTIONALLY_EMPTY;
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/AllSoundEvents$SoundEntry;playAt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/Vec3d;FFZ)V"
            )
    )
    private void trainsounds$muteVanillaSteam(
            AllSoundEvents.SoundEntry soundEntry,
            World world,
            Vec3d soundLocation,
            float volume,
            float pitch,
            boolean fade
    ) {
        if (soundEntry == AllSoundEvents.STEAM && trainsounds$shouldUseCustomEngineSound(entity)) {
            return;
        }

        soundEntry.playAt(world, soundLocation, volume, pitch, fade);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void trainsounds$playEnginePerDce(Carriage.DimensionalCarriageEntity dce, CallbackInfo ci) {
        CarriageContraptionEntity carriageEntity = trainsounds$resolveTargetCarriage(dce);
        if (carriageEntity == null || !carriageEntity.isAlive()) {
            return;
        }

        World world = carriageEntity.getWorld();
        if (world == null) {
            return;
        }

        if (!trainsounds$isEngineEnabledOnCarriage(carriageEntity)) {
            return;
        }

        if (!trainsounds$shouldUseCustomEngineSound(carriageEntity)) {
            return;
        }

        SoundEvent selectedSound = trainsounds$selectEngineSound(carriageEntity);
        if (selectedSound == null) {
            return;
        }

        String channel = trainsounds$resolveChannel(selectedSound);
        float userVolume = TrainSoundVolumeConfigManager.getVolumeMultiplier(channel);
        if (userVolume <= 0.0f) {
            return;
        }

        if (selectedSound == Trainsounds.ELECTRIC_SOUND_EVENT && !trainsounds$hasLivePantographOnTrain(carriageEntity)) {
            return;
        }

        Vec3d soundLocation = carriageEntity.getPos();
        double speedPerTick = trainsounds$getTrainSpeedPerTick(carriageEntity);
        float basePitch = trainsounds$dynamicPitchFromTrainSpeed(carriageEntity, 1.0f);
        float baseVolume = MathHelper.clamp((float) (speedPerTick * 18.0f), 0.20f, 2.5f) * userVolume;

        long pulseTime = world.getTime();
        int phaseOffset = Math.floorMod(carriageEntity.getId(), 7);

        if (speedPerTick >= 0.001) {
            if ((pulseTime + phaseOffset) % 3 == 0) {
                world.playSound(
                        soundLocation.x,
                        soundLocation.y,
                        soundLocation.z,
                        selectedSound,
                        SoundCategory.NEUTRAL,
                        MathHelper.clamp(baseVolume * 1.25f, 0.25f, 3.5f),
                        MathHelper.clamp(basePitch * 1.05f, 0.5f, 2.5f),
                        false
                );
            }

            if ((pulseTime + phaseOffset) % 9 == 0) {
                world.playSound(
                        soundLocation.x,
                        soundLocation.y,
                        soundLocation.z,
                        selectedSound,
                        SoundCategory.NEUTRAL,
                        MathHelper.clamp(baseVolume * 1.9f, 0.35f, 4.0f),
                        MathHelper.clamp(basePitch * 0.82f, 0.5f, 2.5f),
                        false
                );
            }
            return;
        }

        if ((pulseTime + phaseOffset) % 6 == 0) {
            world.playSound(
                    soundLocation.x,
                    soundLocation.y,
                    soundLocation.z,
                    selectedSound,
                    SoundCategory.NEUTRAL,
                    0.9f * userVolume,
                    0.45f,
                    false
            );
        }
    }

    @Unique
    private CarriageContraptionEntity trainsounds$resolveTargetCarriage(Carriage.DimensionalCarriageEntity dce) {
        if (dce != null && dce.entity != null) {
            CarriageContraptionEntity dceEntity = dce.entity.get();
            if (dceEntity != null) {
                return dceEntity;
            }
        }

        return entity;
    }

    @Unique
    private boolean trainsounds$isEngineEnabledOnCarriage(CarriageContraptionEntity carriageEntity) {
        if (carriageEntity instanceof EngineToggleCarrier carrier) {
            return carrier.trainsounds$isEngineBuiltIn();
        }

        // Keep compatibility with setups where the carrier mixin is temporarily unavailable.
        return true;
    }

    @Unique
    private boolean trainsounds$hasLivePantographContact(CarriageContraptionEntity carriageEntity) {
        Contraption contraption = carriageEntity.getContraption();
        if (contraption == null || contraption.presentBlockEntities == null || contraption.presentBlockEntities.isEmpty()) {
            return false;
        }

        for (var blockEntity : contraption.presentBlockEntities.values()) {
            if (blockEntity instanceof PantographBlockEntity pantographBlockEntity && pantographBlockEntity.isExpanded()) {
                return true;
            }
        }

        return false;
    }

    @Unique
    private boolean trainsounds$hasLivePantographOnTrain(CarriageContraptionEntity carriageEntity) {
        Carriage carriage = carriageEntity.getCarriage();
        World world = carriageEntity.getWorld();
        if (carriage == null || carriage.train == null || world == null) {
            return trainsounds$hasLivePantographContact(carriageEntity);
        }

        for (CarriageContraptionEntity candidate : world.getEntitiesByClass(
                CarriageContraptionEntity.class,
                carriageEntity.getBoundingBox().expand(512.0d),
                e -> e != null
                        && e.isAlive()
                        && e.getCarriage() != null
                        && e.getCarriage().train == carriage.train
        )) {
            if (trainsounds$hasLivePantographContact(candidate)) {
                return true;
            }
        }

        return trainsounds$hasLivePantographContact(carriageEntity);
    }

    @Unique
    private boolean trainsounds$shouldUseCustomEngineSound(CarriageContraptionEntity carriageEntity) {
        if (carriageEntity == null || carriageEntity.getCarriage() == null || carriageEntity.getCarriage().train == null
                || carriageEntity.getCarriage().train.icon == null) {
            return false;
        }

        String icon = carriageEntity.getCarriage().train.icon.getId().getPath();
        return "electric".equals(icon) || "modern".equals(icon);
    }

    @Unique
    private SoundEvent trainsounds$selectEngineSound(CarriageContraptionEntity carriageEntity) {
        if (carriageEntity == null || carriageEntity.getCarriage() == null || carriageEntity.getCarriage().train == null
                || carriageEntity.getCarriage().train.icon == null) {
            return null;
        }

        String icon = carriageEntity.getCarriage().train.icon.getId().getPath();

        return switch (icon) {
            case "electric" -> Trainsounds.ELECTRIC_SOUND_EVENT;
            case "modern" -> Trainsounds.DIESEL_SOUND_EVENT;
            default -> null;
        };
    }

    @Unique
    private String trainsounds$resolveChannel(SoundEvent selectedSound) {
        if (selectedSound == Trainsounds.DIESEL_SOUND_EVENT) {
            return "diesel";
        }

        if (selectedSound == Trainsounds.ELECTRIC_SOUND_EVENT) {
            return "electric";
        }

        return "aux";
    }

    @Unique
    private float trainsounds$dynamicPitchFromTrainSpeed(CarriageContraptionEntity carriageEntity, float basePitch) {
        double speedPerTick = trainsounds$getTrainSpeedPerTick(carriageEntity);
        if (carriageEntity.getCarriage() == null || carriageEntity.getCarriage().train == null) {
            return MathHelper.clamp(basePitch, 0.5f, 2.5f);
        }

        float maxSpeedPerTick = Math.max(carriageEntity.getCarriage().train.maxSpeed(), 0.001f);
        float normalizedSpeed = MathHelper.clamp((float) (speedPerTick / maxSpeedPerTick), 0.0f, 1.0f);
        float curved = (float) Math.pow(normalizedSpeed, 0.65f);
        float pitchScale = MathHelper.lerp(curved, 0.95f, 1.45f);
        return MathHelper.clamp(basePitch * pitchScale, 0.5f, 2.5f);
    }

    @Unique
    private double trainsounds$getTrainSpeedPerTick(CarriageContraptionEntity carriageEntity) {
        double positionDelta = carriageEntity.getPos().subtract(carriageEntity.getPrevPositionVec()).length();
        Carriage carriage = carriageEntity.getCarriage();
        if (carriage != null && carriage.train != null) {
            return Math.max(Math.abs(carriage.train.speed), positionDelta);
        }

        return positionDelta;
    }
}
