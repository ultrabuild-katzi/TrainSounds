package de.ultrabuild.trainsounds.mixin;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageSounds;
import de.ultrabuild.trainsounds.logic.EngineToggleCarrier;
import de.ultrabuild.trainsounds.Trainsounds;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@Mixin(CarriageSounds.class)
public abstract class CarriageSoundsMixin {

    @Unique
    private static final Logger TRAINSOUNDS_LOGGER = LoggerFactory.getLogger("TrainSounds/Debug");

    @Unique
    private static final String PAW_PANTOGRAPH_BE_CLASS = "de.mrjulsen.paw.blockentity.PantographBlockEntity";

    @Shadow
    CarriageContraptionEntity entity;

    @Shadow
    int tick;

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
        // Mute the first loop (minecart-esque train bed), keep bogey + seated loops untouched.
        return SoundEvents.INTENTIONALLY_EMPTY;
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/AllSoundEvents$SoundEntry;playAt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/Vec3d;FFZ)V"
            )
    )
    private void trainsounds$replaceTrainSteam(
            AllSoundEvents.SoundEntry soundEntry,
            World world,
            Vec3d soundLocation,
            float volume,
            float pitch,
            boolean fade
    ) {
        // Only replace the train steam puffs; all other Create sounds keep their original behavior.
        if (soundEntry == AllSoundEvents.STEAM) {
            if (!trainsounds$isEngineBuiltInOnThisCarriage()) {
                trainsounds$debug("steam_redirect_skip", "engine_built_in=false");
                return;
            }

            SoundEvent selectedSound = trainsounds$selectSteamLikeSound();
            SoundEvent vanillaSteamSound = AllSoundEvents.STEAM.getMainEvent();
            boolean isElectric = selectedSound == Trainsounds.ELECTRIC_SOUND_EVENT;
            boolean isVanillaSteam = selectedSound == vanillaSteamSound;

            // Electric only on wagons that actually have active pantograph wire contact.
            if (isElectric && !trainsounds$hasLivePantographContact()) {
                trainsounds$debug("steam_redirect_skip", "electric_without_pantograph_contact");
                return;
            }

            if (isVanillaSteam) {
                trainsounds$debug("steam_redirect_passthrough", "vanilla_steam_event");
                soundEntry.playAt(world, soundLocation, volume, pitch, fade);
                return;
            }

            double speedPerTick = trainsounds$getTrainSpeedPerTick();
            float dynamicPitch = trainsounds$dynamicPitchFromTrainSpeed(pitch);
            // Create liefert hier oft sehr kleine Basislautstaerken; fuer Custom-Engine-Sounds staerker boosten.
            float boostedVolume = MathHelper.clamp(volume * 24.0f, 0.22f, 4.0f);

            trainsounds$debug(
                    "steam_redirect_play",
                    "sound=" + selectedSound.getId() +
                            ", speed=" + String.format("%.4f", speedPerTick) +
                            ", volume=" + String.format("%.4f", boostedVolume) +
                            ", pitch=" + String.format("%.4f", dynamicPitch)
            );
            
            world.playSound(
                    soundLocation.x,
                    soundLocation.y,
                    soundLocation.z,
                    selectedSound,
                    SoundCategory.NEUTRAL,
                    boostedVolume,
                    dynamicPitch,
                    fade
            );
            
            // When completely stopped, also play an idle hum
            if (speedPerTick < 0.001) {
                world.playSound(
                        soundLocation.x,
                        soundLocation.y,
                        soundLocation.z,
                        selectedSound,
                        SoundCategory.NEUTRAL,
                        1.0f,   // loud idle hum
                        0.45f,  // very low pitch
                        false
                );
            }
            return;
        }

        soundEntry.playAt(world, soundLocation, volume, pitch, fade);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void trainsounds$playEngineForNonLeadingCarriages(Carriage.DimensionalCarriageEntity dce, CallbackInfo ci) {
        Carriage rootCarriage = entity.getCarriage();
        if (rootCarriage == null || rootCarriage.train == null) {
            trainsounds$debug("non_leading_skip", "root_train_missing");
            return;
        }

        World world = entity.getWorld();
        long pulseTime = trainsounds$getSoundPulseTime(world);

        List<CarriageContraptionEntity> trainEntities = world.getEntitiesByClass(
                CarriageContraptionEntity.class,
                entity.getBoundingBox().expand(256.0d),
                candidate -> candidate != null
                        && candidate.isAlive()
                        && candidate.getCarriage() != null
                        && candidate.getCarriage().train == rootCarriage.train
        );

        if (trainEntities.isEmpty()) {
            trainsounds$debug("non_leading_skip", "no_train_entities_found");
            return;
        }

        for (CarriageContraptionEntity carriageEntity : trainEntities) {
            if (carriageEntity.carriageIndex <= 0) {
                continue;
            }

            if (!trainsounds$isEngineBuiltInOnCarriage(carriageEntity)) {
                continue;
            }

            SoundEvent selectedSound = trainsounds$selectSteamLikeSound(carriageEntity);
            boolean isElectric = selectedSound == Trainsounds.ELECTRIC_SOUND_EVENT;
            if (isElectric && !trainsounds$hasLivePantographContact(carriageEntity)) {
                trainsounds$debug("non_leading_skip", "carriage=" + carriageEntity.carriageIndex + ", electric_without_pantograph_contact");
                continue;
            }

            Vec3d soundLocation = carriageEntity.getPos();
            double speedPerTick = trainsounds$getTrainSpeedPerTick(carriageEntity);
            float basePitch = trainsounds$dynamicPitchFromTrainSpeed(carriageEntity, 1.0f);
            float baseVolume = MathHelper.clamp((float) (speedPerTick * 18.0f), 0.20f, 2.5f);

            trainsounds$debug(
                    "non_leading_tick",
                    "carriage=" + carriageEntity.carriageIndex +
                            ", sound=" + selectedSound.getId() +
                            ", speed=" + String.format("%.4f", speedPerTick) +
                            ", pulseTime=" + pulseTime +
                            ", baseVolume=" + String.format("%.4f", baseVolume) +
                            ", basePitch=" + String.format("%.4f", basePitch)
            );

            int phaseOffset = Math.floorMod(carriageEntity.getId(), 7);
            if (speedPerTick >= 0.001) {
                if ((pulseTime + phaseOffset) % 3 == 0) {
                    world.playSound(soundLocation.x, soundLocation.y, soundLocation.z, selectedSound, SoundCategory.NEUTRAL,
                            MathHelper.clamp(baseVolume * 1.25f, 0.25f, 3.5f), MathHelper.clamp(basePitch * 1.05f, 0.5f, 2.5f), false);
                }

                if ((pulseTime + phaseOffset) % 9 == 0) {
                    world.playSound(soundLocation.x, soundLocation.y, soundLocation.z, selectedSound, SoundCategory.NEUTRAL,
                            MathHelper.clamp(baseVolume * 1.9f, 0.35f, 4.0f), MathHelper.clamp(basePitch * 0.82f, 0.5f, 2.5f), false);
                }
            }

            if (speedPerTick < 0.001 && pulseTime % 6 == 0) {
                world.playSound(soundLocation.x, soundLocation.y, soundLocation.z, selectedSound, SoundCategory.NEUTRAL,
                        0.9f, 0.45f, false);
            }
        }
    }


    @Unique
    private boolean trainsounds$isEngineBuiltInOnThisCarriage() {
        if (entity instanceof EngineToggleCarrier carrier) {
            boolean engineBuiltIn = carrier.trainsounds$isEngineBuiltIn();
            trainsounds$debug("engine_check", "carrier_interface_found, engine_built_in=" + engineBuiltIn);
            return engineBuiltIn;
        }
        trainsounds$debug("engine_check", "carrier_interface_not_found, defaults_to_false");
        return false;
    }

    @Unique
    private boolean trainsounds$isEngineBuiltInOnCarriage(CarriageContraptionEntity carriageEntity) {
        if (carriageEntity instanceof EngineToggleCarrier carrier) {
            return carrier.trainsounds$isEngineBuiltIn();
        }
        return false;
    }

    @Unique
    private boolean trainsounds$isLeadingCarriage() {
        Carriage carriage = entity.getCarriage();
        if (carriage != null && carriage.train != null && carriage.train.carriages != null && !carriage.train.carriages.isEmpty()) {
            Carriage leadingCarriage = carriage.train.carriages.get(0);
            boolean isLeading = (carriage == leadingCarriage);
            trainsounds$debug("leading_carriage_check", "is_leading=" + isLeading);
            return isLeading;
        }
        trainsounds$debug("leading_carriage_check", "train_or_carriages_null, defaults_to_false");
        return false;
    }


    @Unique
    private boolean trainsounds$hasLivePantographContact() {
        return trainsounds$hasLivePantographContact(entity);
    }

    @Unique
    private boolean trainsounds$hasLivePantographContact(CarriageContraptionEntity carriageEntity) {
        Contraption contraption = carriageEntity.getContraption();
        if (contraption == null || contraption.presentBlockEntities == null || contraption.presentBlockEntities.isEmpty()) {
            trainsounds$debug("pantograph_check", "contraption_null_or_empty");
            return false;
        }

        trainsounds$debug("pantograph_check", "total_block_entities=" + contraption.presentBlockEntities.size());

        for (var blockEntity : contraption.presentBlockEntities.values()) {
            String className = blockEntity.getClass().getName();
            trainsounds$debug("pantograph_check", "found_block_entity=" + className);

            if (!className.equals(PAW_PANTOGRAPH_BE_CLASS)) {
                continue;
            }

            boolean isExpanded = trainsounds$isPantographExpanded(blockEntity);
            trainsounds$debug("pantograph_check", "paw_pantograph_expanded=" + isExpanded);

            if (isExpanded) {
                return true;
            }
        }

        trainsounds$debug("pantograph_check", "no_expanded_pantographs_found");
        return false;
    }

    @Unique
    private boolean trainsounds$isPantographExpanded(Object pantographBlockEntity) {
        try {
            // In PAW, expanded means raised and currently in wire-contact state.
            Method isExpanded = pantographBlockEntity.getClass().getMethod("isExpanded");
            Object result = isExpanded.invoke(pantographBlockEntity);
            boolean expanded = result instanceof Boolean b && b;
            trainsounds$debug("pantograph_expanded_check", "result=" + expanded);
            return expanded;
        } catch (ReflectiveOperationException e) {
            trainsounds$debug("pantograph_expanded_check", "reflection_failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    @Unique
    private SoundEvent trainsounds$selectSteamLikeSound() {
        return trainsounds$selectSteamLikeSound(entity);
    }

    @Unique
    private SoundEvent trainsounds$selectSteamLikeSound(CarriageContraptionEntity carriageEntity) {
        if (carriageEntity.getCarriage() == null || carriageEntity.getCarriage().train == null || carriageEntity.getCarriage().train.icon == null) {
            return AllSoundEvents.STEAM.getMainEvent();
        }

        String icon = carriageEntity.getCarriage().train.icon.getId().getPath();

        return switch (icon) {
            case "electric" -> Trainsounds.ELECTRIC_SOUND_EVENT;
            case "modern" -> Trainsounds.DIESEL_SOUND_EVENT;
            case "steam", "traditional" -> AllSoundEvents.STEAM.getMainEvent();
            default -> AllSoundEvents.STEAM.getMainEvent();
        };
    }

    @Unique
    private float trainsounds$dynamicPitchFromTrainSpeed(float basePitch) {
        return trainsounds$dynamicPitchFromTrainSpeed(entity, basePitch);
    }

    @Unique
    private float trainsounds$dynamicPitchFromTrainSpeed(CarriageContraptionEntity carriageEntity, float basePitch) {
        double speedPerTick = trainsounds$getTrainSpeedPerTick(carriageEntity);
        if (carriageEntity.getCarriage() == null || carriageEntity.getCarriage().train == null) {
            return MathHelper.clamp(basePitch, 0.5f, 2.5f);
        }

        float maxSpeedPerTick = Math.max(carriageEntity.getCarriage().train.maxSpeed(), 0.001f);
        float normalizedSpeed = MathHelper.clamp((float) (speedPerTick / maxSpeedPerTick), 0.0f, 1.0f);

        // Non-linear curve keeps low-speed sounds natural and ramps pitch more at higher speed.
        float curved = (float) Math.pow(normalizedSpeed, 0.65f);
        float pitchScale = MathHelper.lerp(curved, 0.95f, 1.45f);
        return MathHelper.clamp(basePitch * pitchScale, 0.5f, 2.5f);
    }

    @Unique
    private void trainsounds$debug(String stage, String details) {
        if (!trainsounds$shouldDebugLog()) {
            return;
        }

        TRAINSOUNDS_LOGGER.info(
                "[{}] train={} details={}",
                stage,
                entity.trainId,
                details
        );
    }

    @Unique
    private boolean trainsounds$shouldDebugLog() {
        // Keep logs readable and aligned with non-leading sound pulse timing.
        return trainsounds$getSoundPulseTime(entity.getWorld()) % 10 == 0;
    }

    @Unique
    private long trainsounds$getSoundPulseTime(World world) {
        return world.getTime();
    }

    @Unique
    private double trainsounds$getTrainSpeedPerTick() {
        return trainsounds$getTrainSpeedPerTick(entity);
    }

    @Unique
    private double trainsounds$getTrainSpeedPerTick(CarriageContraptionEntity carriageEntity) {
        Carriage carriage = carriageEntity.getCarriage();
        if (carriage != null && carriage.train != null) {
            Double speedFromTrain = trainsounds$extractTrainSpeed(carriage.train);
            if (speedFromTrain != null && Double.isFinite(speedFromTrain)) {
                double speed = Math.abs(speedFromTrain);
                trainsounds$debug("speed_source", "source=train_reflection, speed=" + String.format("%.4f", speed));
                return speed;
            }
        }

        double fallbackSpeed = carriageEntity.getPos().subtract(carriageEntity.getPrevPositionVec()).length();
        trainsounds$debug("speed_source", "source=entity_delta_fallback, speed=" + String.format("%.4f", fallbackSpeed));
        return fallbackSpeed;
    }

    @Unique
    private Double trainsounds$extractTrainSpeed(Object train) {
        Double byMethod = trainsounds$tryReadMethod(train, "speed");
        if (byMethod != null) {
            return byMethod;
        }

        byMethod = trainsounds$tryReadMethod(train, "getSpeed");
        if (byMethod != null) {
            return byMethod;
        }

        byMethod = trainsounds$tryReadMethod(train, "getCurrentSpeed");
        if (byMethod != null) {
            return byMethod;
        }

        Double byField = trainsounds$tryReadField(train, "speed");
        if (byField != null) {
            return byField;
        }

        trainsounds$debug("speed_source", "train_reflection_unresolved");
        return null;
    }

    @Unique
    private Double trainsounds$tryReadMethod(Object target, String methodName) {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(methodName);
                method.setAccessible(true);
                Object value = method.invoke(target);
                if (value instanceof Number number && Double.isFinite(number.doubleValue())) {
                    double result = number.doubleValue();
                    trainsounds$debug("speed_source", "method=" + methodName + ", owner=" + current.getSimpleName() + ", speed=" + String.format("%.4f", result));
                    return result;
                }
            } catch (NoSuchMethodException ignored) {
                // Continue in superclass.
            } catch (ReflectiveOperationException | RuntimeException exception) {
                trainsounds$debug("speed_source", "method_failed=" + methodName + ", error=" + exception.getClass().getSimpleName());
                return null;
            }

            current = current.getSuperclass();
        }

        return null;
    }

    @Unique
    private Double trainsounds$tryReadField(Object target, String fieldName) {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(target);
                if (value instanceof Number number && Double.isFinite(number.doubleValue())) {
                    double result = number.doubleValue();
                    trainsounds$debug("speed_source", "field=" + fieldName + ", owner=" + current.getSimpleName() + ", speed=" + String.format("%.4f", result));
                    return result;
                }
            } catch (NoSuchFieldException ignored) {
                // Continue in superclass.
            } catch (ReflectiveOperationException | RuntimeException exception) {
                trainsounds$debug("speed_source", "field_failed=" + fieldName + ", error=" + exception.getClass().getSimpleName());
                return null;
            }

            current = current.getSuperclass();
        }

        return null;
    }
}

