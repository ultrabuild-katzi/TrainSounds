package de.ultrabuild.trainsounds.mixin;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.trains.entity.CarriageSounds;
import de.ultrabuild.trainsounds.Trainsounds;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CarriageSounds.class)
public abstract class CarriageSoundsMixin {

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
            world.playSound(
                    soundLocation.x,
                    soundLocation.y,
                    soundLocation.z,
                    Trainsounds.ELECTRIC_SOUND_EVENT,
                    SoundCategory.NEUTRAL,
                    volume,
                    pitch,
                    fade
            );
            return;
        }

        soundEntry.playAt(world, soundLocation, volume, pitch, fade);
    }
}

