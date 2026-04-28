package de.ultrabuild.trainsounds.logic;

import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import de.ultrabuild.trainsounds.Trainsounds;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TrainEngineToggleHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("TrainSounds/Toggle");
    private static final String DEBUG_SEPARATOR = "_____________";
    private static final Map<UUID, ToggleAttempt> LAST_TOGGLE_ATTEMPT = new HashMap<>();

    private record ToggleAttempt(long tick, UUID targetId, String source) {
    }

    private TrainEngineToggleHandler() {
    }

    public static void register() {
        UseEntityCallback.EVENT.register(TrainEngineToggleHandler::onUseEntity);
        UseItemCallback.EVENT.register(TrainEngineToggleHandler::onUseItem);
    }

    private static ActionResult onUseEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        logBoundary("START", "use_entity", player, world, "hand=" + hand + ", entity=" + entity.getUuid());
        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(Trainsounds.ENGINE_TOGGLE_ITEM)) {
            logBoundary("FINISH", "use_entity", player, world, "result=PASS, reason=wrong_item");
            return ActionResult.PASS;
        }

        if (!(entity instanceof CarriageContraptionEntity carriageEntity)) {
            logBoundary("FINISH", "use_entity", player, world, "result=PASS, reason=not_carriage");
            return ActionResult.PASS;
        }

        if (!(carriageEntity instanceof EngineToggleCarrier carrier)) {
            logBoundary("FINISH", "use_entity", player, world, "result=PASS, reason=carrier_mixin_missing");
            return ActionResult.PASS;
        }

        // Pass to client to open GUI
        if (world.isClient) {
            logBoundary("FINISH", "use_entity", player, world, "result=PASS, reason=client_side");
            return ActionResult.PASS;
        }

        // On server side, just consume but don't do anything
        // The client will handle opening the GUI
        logBoundary(
                "FINISH",
                "use_entity",
                player,
                world,
                "result=CONSUME (GUI opened on client), entity=" + carriageEntity.getUuid() + ", carriage_index=" + carriageEntity.carriageIndex
        );
        return ActionResult.CONSUME;
    }

    private static TypedActionResult<ItemStack> onUseItem(PlayerEntity player, World world, Hand hand) {
        logBoundary("START", "use_item", player, world, "hand=" + hand);
        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(Trainsounds.ENGINE_TOGGLE_ITEM)) {
            logBoundary("FINISH", "use_item", player, world, "result=PASS, reason=wrong_item");
            return TypedActionResult.pass(stack);
        }

        // Let vanilla send interaction packets; actual toggle runs on server.
        if (world.isClient) {
            logBoundary("FINISH", "use_item", player, world, "result=PASS, reason=client_side");
            return TypedActionResult.pass(stack);
        }

        CarriageContraptionEntity target = findCarriageInFront(player, world, 8.0d);
        if (target == null) {
            logBoundary("FINISH", "use_item", player, world, "result=PASS, reason=no_carriage_in_front");
            return TypedActionResult.pass(stack);
        }

        if (!(target instanceof EngineToggleCarrier carrier)) {
            player.sendMessage(Text.literal("Carriage engine state mixin missing."), true);
            logBoundary("FINISH", "use_item", player, world, "result=CONSUME, reason=carrier_mixin_missing");
            return TypedActionResult.consume(stack);
        }

        if (isDuplicateToggle(player, world, target, "use_item")) {
            logBoundary("FINISH", "use_item", player, world, "result=CONSUME, reason=duplicate_tick_target");
            return TypedActionResult.consume(stack);
        }

        // Send packet to client to open GUI
        if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            de.ultrabuild.trainsounds.network.TrainSoundsNetworking.sendOpenCarriageGuiPacket(serverPlayer, target);
        }

        logBoundary(
                "FINISH",
                "use_item",
                player,
                world,
                "result=CONSUME (sent GUI open packet), entity=" + target.getUuid() + ", carriage_index=" + target.carriageIndex
        );
        return TypedActionResult.consume(stack);
    }

    private static void toggleCarriageEngine(PlayerEntity player, CarriageContraptionEntity carriageEntity, EngineToggleCarrier carrier) {
        boolean before = carrier.trainsounds$isEngineBuiltIn();
        LOGGER.info(
                "{} toggle_engine START {} player={} entity={} carriage_index={} state_before={}",
                DEBUG_SEPARATOR,
                DEBUG_SEPARATOR,
                player.getUuid(),
                carriageEntity.getUuid(),
                carriageEntity.carriageIndex,
                before
        );
        carrier.trainsounds$toggleEngineBuiltIn();
        boolean enabled = carrier.trainsounds$isEngineBuiltIn();
        int carriageDisplayIndex = carriageEntity.carriageIndex + 1;
        LOGGER.info(
                "{} toggle_engine FINISH {} player={} entity={} carriage_index={} state_after={}",
                DEBUG_SEPARATOR,
                DEBUG_SEPARATOR,
                player.getUuid(),
                carriageEntity.getUuid(),
                carriageEntity.carriageIndex,
                enabled
        );
        player.sendMessage(Text.translatable(
                enabled ? "message.trainsounds.engine_on" : "message.trainsounds.engine_off",
                carriageDisplayIndex
        ), true);
    }

    private static CarriageContraptionEntity findCarriageInFront(PlayerEntity player, World world, double maxDistance) {
        Vec3d eyePos = player.getEyePos();
        Vec3d lookDir = player.getRotationVec(1.0f).normalize();
        Vec3d rayEnd = eyePos.add(lookDir.multiply(maxDistance));
        Box searchBox = player.getBoundingBox().expand(maxDistance);

        List<CarriageContraptionEntity> candidates = world.getEntitiesByClass(
                CarriageContraptionEntity.class,
                searchBox,
                entity -> entity != null && entity.isAlive()
        );

        return candidates.stream()
                .filter(entity -> {
                    Vec3d toEntity = entity.getPos().subtract(eyePos);
                    double forwardDistance = toEntity.dotProduct(lookDir);
                    return forwardDistance > 0 && forwardDistance <= maxDistance;
                })
                .filter(entity -> entity.getBoundingBox().expand(0.25f).raycast(eyePos, rayEnd).isPresent())
                .min(Comparator.comparingDouble(entity -> {
                    Vec3d hitPos = entity.getBoundingBox().expand(0.25f).raycast(eyePos, rayEnd).orElse(entity.getPos());
                    return eyePos.squaredDistanceTo(hitPos);
                }))
                .orElse(null);
    }

    private static boolean isDuplicateToggle(PlayerEntity player, World world, CarriageContraptionEntity target, String source) {
        long now = world.getTime();
        UUID playerId = player.getUuid();
        UUID targetId = target.getUuid();

        ToggleAttempt previous = LAST_TOGGLE_ATTEMPT.put(playerId, new ToggleAttempt(now, targetId, source));
        boolean duplicate = previous != null && previous.tick == now && previous.targetId != null && previous.targetId.equals(targetId);

        LOGGER.info(
                "{} duplicate_check {} player={} tick={} source={} target={} duplicate={} previous_tick={} previous_target={} previous_source={}",
                DEBUG_SEPARATOR,
                DEBUG_SEPARATOR,
                playerId,
                now,
                source,
                targetId,
                duplicate,
                previous != null ? previous.tick : null,
                previous != null ? previous.targetId : null,
                previous != null ? previous.source : null
        );

        return duplicate;
    }

    private static void logBoundary(String edge, String source, PlayerEntity player, World world, String details) {
        LOGGER.info(
                "{} {} {} player={} tick={} side={} details={} {}",
                DEBUG_SEPARATOR,
                source,
                edge,
                player.getUuid(),
                world.getTime(),
                world.isClient ? "client" : "server",
                details,
                DEBUG_SEPARATOR
        );
    }
}

