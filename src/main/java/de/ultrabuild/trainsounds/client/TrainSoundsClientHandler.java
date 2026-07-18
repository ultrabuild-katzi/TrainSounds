package de.ultrabuild.trainsounds.client;

import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import de.ultrabuild.trainsounds.Trainsounds;
import de.ultrabuild.trainsounds.client.gui.CarriageManagementScreen;
import de.ultrabuild.trainsounds.network.TrainSoundsNetworking;
import de.ultrabuild.trainsounds.network.packet.OpenCarriageGuiS2CPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import de.ultrabuild.trainsounds.logic.TrainEngineToggleHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Environment(EnvType.CLIENT)
public class TrainSoundsClientHandler {

    public static void register() {
        // Use Fabric's UseEntityCallback for client-side entity interaction
        UseEntityCallback.EVENT.register(TrainSoundsClientHandler::onUseEntity);
        UseItemCallback.EVENT.register(TrainSoundsClientHandler::onUseItem);
        
        // Register S2C packet handler
        assert TrainSoundsNetworking.OPEN_CARRIAGE_GUI != null;
        ClientPlayNetworking.registerGlobalReceiver(TrainSoundsNetworking.OPEN_CARRIAGE_GUI, (client, handler, buf, sender) -> {
            OpenCarriageGuiS2CPacket packet = new OpenCarriageGuiS2CPacket(buf);
            client.execute(() -> {
                if (client.world != null) {
                    Entity entity = client.world.getEntityById(packet.getCarriageEntityId());
                    if (entity instanceof CarriageContraptionEntity carriage) {
                        openCarriageManagementScreen(carriage);
                    }
                }
            });
        });
    }

    private static ActionResult onUseEntity(net.minecraft.entity.player.PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        // Only on client
        if (world.isClient) {
            ItemStack stack = player.getStackInHand(hand);

            // Check if holding the engine toggle tool
            if (!stack.isOf(Trainsounds.ENGINE_TOGGLE_ITEM)) {
                return ActionResult.PASS;
            }

            // Check if it's a carriage
            if (!(entity instanceof CarriageContraptionEntity carriageEntity)) {
                return ActionResult.PASS;
            }

            // Open the GUI
            openCarriageManagementScreen(carriageEntity);
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    private static TypedActionResult<ItemStack> onUseItem(net.minecraft.entity.player.PlayerEntity player, World world, Hand hand) {
        if (world.isClient) {
            ItemStack stack = player.getStackInHand(hand);
            if (!stack.isOf(Trainsounds.ENGINE_TOGGLE_ITEM)) {
                return TypedActionResult.pass(stack);
            }

            CarriageContraptionEntity target = TrainEngineToggleHandler.findCarriageInFront(player, world, 8.0d);
            if (target != null) {
                openCarriageManagementScreen(target);
                return TypedActionResult.success(stack);
            }
        }
        return TypedActionResult.pass(player.getStackInHand(hand));
    }

    public static void openCarriageManagementScreen(CarriageContraptionEntity startCarriage) {
        MinecraftClient client = MinecraftClient.getInstance();
        World world = startCarriage.getWorld();

        if (world == null) {
            return;
        }

        // Get all carriages in the train
        List<CarriageContraptionEntity> allCarriages = getAllCarriagesInTrain(startCarriage, world);

        if (allCarriages.isEmpty()) {
            return;
        }

        // Find the index of the clicked carriage
        int startIndex = 0;
        for (int i = 0; i < allCarriages.size(); i++) {
            if (allCarriages.get(i).getId() == startCarriage.getId()) {
                startIndex = i;
                break;
            }
        }

        // Open the GUI
        client.setScreen(new CarriageManagementScreen(client.currentScreen, allCarriages, startIndex));
    }

    public static List<CarriageContraptionEntity> getAllCarriagesInTrain(CarriageContraptionEntity startCarriage, World world) {
        List<CarriageContraptionEntity> result = new ArrayList<>();

        // Get the train from the carriage
        com.simibubi.create.content.trains.entity.Carriage carriage = startCarriage.getCarriage();
        if (carriage == null || carriage.train == null) {
            // If we can't get the train, just return the single carriage
            result.add(startCarriage);
            return result;
        }

        // Get all carriages in the train
        List<CarriageContraptionEntity> candidates = world.getEntitiesByClass(
                CarriageContraptionEntity.class,
                startCarriage.getBoundingBox().expand(512.0d),
                e -> e != null && e.isAlive() && e.getCarriage() != null && e.getCarriage().train == carriage.train
        );

        // Sort by carriage index
        candidates.sort(Comparator.comparingInt(c -> c.carriageIndex));
        result.addAll(candidates);

        return result;
    }
}

