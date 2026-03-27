package de.ultrabuild.trainsounds.logic;

import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import de.ultrabuild.trainsounds.Trainsounds;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public final class TrainEngineToggleHandler {

    private TrainEngineToggleHandler() {
    }

    public static void register() {
        UseEntityCallback.EVENT.register(TrainEngineToggleHandler::onUseEntity);
    }

    private static ActionResult onUseEntity(PlayerEntity player, World world, Hand hand, Entity entity, Object hitResult) {
        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(Trainsounds.ENGINE_TOGGLE_ITEM)) {
            return ActionResult.PASS;
        }

        if (!(entity instanceof CarriageContraptionEntity carriageEntity)) {
            return ActionResult.PASS;
        }

        if (!(carriageEntity instanceof EngineToggleCarrier carrier)) {
            return ActionResult.PASS;
        }

        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        carrier.trainsounds$toggleEngineBuiltIn();
        boolean enabled = carrier.trainsounds$isEngineBuiltIn();
        player.sendMessage(Text.literal("Engine in carriage " + carriageEntity.carriageIndex + " is now " + (enabled ? "ENABLED" : "DISABLED")), true);
        return ActionResult.SUCCESS;
    }
}

