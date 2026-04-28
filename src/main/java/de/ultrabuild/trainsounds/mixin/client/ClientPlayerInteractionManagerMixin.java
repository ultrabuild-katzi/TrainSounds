package de.ultrabuild.trainsounds.mixin.client;

import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import de.ultrabuild.trainsounds.Trainsounds;
import de.ultrabuild.trainsounds.client.TrainSoundsClientHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.client.network.ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "interactEntity", at = @At("HEAD"), cancellable = true)
    private void trainsounds$onClientInteractEntity(PlayerEntity player, Entity entity, Hand hand, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof CarriageContraptionEntity carriageEntity)) {
            return;
        }

        net.minecraft.item.ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(Trainsounds.ENGINE_TOGGLE_ITEM)) {
            return;
        }

        // Open the GUI instead of letting the normal interaction happen
        TrainSoundsClientHandler.openCarriageManagementScreen(carriageEntity);
        cir.setReturnValue(true);
    }
}

