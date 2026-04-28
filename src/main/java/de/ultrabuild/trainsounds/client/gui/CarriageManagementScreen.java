package de.ultrabuild.trainsounds.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import de.ultrabuild.trainsounds.logic.EngineToggleCarrier;
import de.ultrabuild.trainsounds.network.TrainSoundsNetworking;
import de.ultrabuild.trainsounds.network.packet.ToggleCarriageEngineC2SPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class CarriageManagementScreen extends Screen {

    private static final int BLUEPRINT_COLOR = 0xFF4A90E2;
    private static final int BACKGROUND_COLOR = 0xFF1a1a2e;

    private final Screen parent;
    private final List<CarriageContraptionEntity> carriages;
    private int currentCarriageIndex = 0;
    private ButtonWidget prevButton;
    private ButtonWidget nextButton;
    private ButtonWidget toggleButton;

    private int panelLeft;
    private int panelRight;
    private int panelTop;
    private int panelBottom;

    // Model interaction variables
    private float modelRotationY = 0.0f;
    private float modelRotationX = 0.0f;
    private float modelScale = 30.0f; // Initial scale
    private boolean isDragging = false;
    private double lastMouseX;
    private double lastMouseY;

    public CarriageManagementScreen(Screen parent, List<CarriageContraptionEntity> carriages, int startIndex) {
        super(Text.literal("TrainSounds"));
        this.parent = parent;
        this.carriages = new ArrayList<>(carriages);
        this.currentCarriageIndex = Math.max(0, Math.min(startIndex, this.carriages.size() - 1));
    }

    @Override
    protected void init() {
        // Calculate panel dimensions (centered, wider)
        int panelWidth = Math.min(this.width - 40, 400);
        int panelHeight = this.height - 80;
        panelLeft = (this.width - panelWidth) / 2;
        panelRight = panelLeft + panelWidth;
        panelTop = 40;
        panelBottom = panelTop + panelHeight;

        int centerX = (panelLeft + panelRight) / 2;

        // Previous button (left arrow)
        prevButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("◀"), button -> {
            if (currentCarriageIndex > 0) {
                currentCarriageIndex--;
                updateToggleButton();
            }
        }).dimensions(panelLeft + 20, panelBottom - 50, 40, 20).build());

        // Next button (right arrow)
        nextButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("▶"), button -> {
            if (currentCarriageIndex < carriages.size() - 1) {
                currentCarriageIndex++;
                updateToggleButton();
            }
        }).dimensions(panelRight - 60, panelBottom - 50, 40, 20).build());

        // Engine toggle button
        toggleButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Engine"), button -> {
            toggleCurrentCarriageEngine();
        }).dimensions(centerX - 40, panelBottom - 80, 80, 20).build());

        // Close button
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> this.close())
                .dimensions(centerX - 50, this.height - 25, 100, 20).build());

        updateToggleButton();
    }

    private void updateToggleButton() {
        if (currentCarriageIndex >= 0 && currentCarriageIndex < carriages.size()) {
            CarriageContraptionEntity carriage = carriages.get(currentCarriageIndex);
            if (carriage instanceof EngineToggleCarrier carrier) {
                boolean isEngineOn = carrier.trainsounds$isEngineBuiltIn();
                Text text = isEngineOn ? Text.literal("Engine: ON").formatted(Formatting.GREEN)
                        : Text.literal("Engine: OFF").formatted(Formatting.RED);
                toggleButton.setMessage(text);
            }
        }
    }

    private void toggleCurrentCarriageEngine() {
        if (currentCarriageIndex >= 0 && currentCarriageIndex < carriages.size()) {
            CarriageContraptionEntity carriage = carriages.get(currentCarriageIndex);
            if (carriage instanceof EngineToggleCarrier carrier) {
                // Send packet to server to toggle the engine
                sendTogglePacket(carriage.getId());
                // Update local state
                carrier.trainsounds$toggleEngineBuiltIn();
                updateToggleButton();
            }
        }
    }

    private void sendTogglePacket(int entityId) {
        net.minecraft.network.PacketByteBuf buf = new net.minecraft.network.PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        ToggleCarriageEngineC2SPacket packet = new ToggleCarriageEngineC2SPacket(entityId);
        packet.write(buf);
        ClientPlayNetworking.send(TrainSoundsNetworking.TOGGLE_ENGINE, buf);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw blueprint background
        drawBlueprintBackground(context);

        // Draw panel background
        context.fill(panelLeft, panelTop, panelRight, panelBottom, 0xFF0a0a1a);

        // Draw blueprint border
        drawBlueprintBorder(context, panelLeft, panelTop, panelRight, panelBottom);

        // Draw title
        Text title = Text.literal("TrainSounds").formatted(Formatting.BOLD);
        context.drawCenteredTextWithShadow(this.textRenderer, title, this.width / 2, 15, 0x4A90E2);

        // Draw train/carriage info
        if (!carriages.isEmpty()) {
            CarriageContraptionEntity currentCarriage = carriages.get(currentCarriageIndex);
            
            // Train name at top of panel
            String trainName = "Train";
            if (currentCarriage.getCarriage() != null && currentCarriage.getCarriage().train != null) {
                trainName = currentCarriage.getCarriage().train.name.getString();
            }
            Text trainText = Text.literal(trainName).formatted(Formatting.BOLD);
            context.drawCenteredTextWithShadow(this.textRenderer, trainText, this.width / 2, panelTop + 10, 0xFFFFFF);

            // Carriage counter
            Text carriageText = Text.literal("Carriage " + (currentCarriageIndex + 1) + " of " + carriages.size());
            context.drawCenteredTextWithShadow(this.textRenderer, carriageText, this.width / 2, panelBottom - 20, 0xAAAAAA);

            // Draw 3D carriage model in the center
            int modelX = this.width / 2;
            int modelY = (panelTop + panelBottom) / 2 - 20;
            drawCarriageModel(context, modelX, modelY, currentCarriage, delta);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawBlueprintBackground(DrawContext context) {
        context.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);

        // Draw grid pattern
        int gridSize = 20;
        for (int x = 0; x < this.width; x += gridSize) {
            context.fill(x, 0, x + 1, this.height, 0xFF1a4a7a);
        }
        for (int y = 0; y < this.height; y += gridSize) {
            context.fill(0, y, this.width, y + 1, 0xFF1a4a7a);
        }
    }

    private void drawBlueprintBorder(DrawContext context, int x1, int y1, int x2, int y2) {
        // Draw blueprint-style border
        int borderColor = 0xFF4A90E2;

        // Outer border
        context.fill(x1 - 2, y1 - 2, x2 + 2, y1, borderColor);      // Top outer
        context.fill(x1 - 2, y2, x2 + 2, y2 + 2, borderColor);      // Bottom outer
        context.fill(x1 - 2, y1 - 2, x1, y2 + 2, borderColor);      // Left outer
        context.fill(x2, y1 - 2, x2 + 2, y2 + 2, borderColor);      // Right outer

        // Inner border
        context.fill(x1, y1, x2, y1 + 1, borderColor);              // Top inner
        context.fill(x1, y2 - 1, x2, y2, borderColor);              // Bottom inner
        context.fill(x1, y1, x1 + 1, y2, borderColor);              // Left inner
        context.fill(x2 - 1, y1, x2, y2, borderColor);              // Right inner
    }

    private void drawCarriageModel(DrawContext context, int x, int y, CarriageContraptionEntity carriage, float delta) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 300);

        try {
            // Enable proper lighting and depth testing
            DiffuseLighting.enableGuiDepthLighting();
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();

            context.getMatrices().push();
            
            // Apply scale
            context.getMatrices().scale(modelScale, -modelScale, modelScale);

            // Apply rotation
            context.getMatrices().multiply(RotationAxis.POSITIVE_X.rotationDegrees(modelRotationX));
            context.getMatrices().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(modelRotationY));
            
            // Render the blocks and block entities of the contraption
            assert this.client != null;
            if (carriage.getContraption() != null) {
                Contraption contraption = carriage.getContraption();

                // Render blocks
                for (Map.Entry<BlockPos, StructureTemplate.StructureBlockInfo> entry : contraption.getBlocks().entrySet()) {
                    BlockPos localPos = entry.getKey();
                    StructureTemplate.StructureBlockInfo info = entry.getValue();
                    BlockState blockState = info.state();

                    context.getMatrices().push();
                    context.getMatrices().translate(localPos.getX(), localPos.getY(), localPos.getZ());
                    this.client.getBlockRenderManager().renderBlockAsEntity(blockState, context.getMatrices(), context.getVertexConsumers(),
                        net.minecraft.client.render.LightmapTextureManager.MAX_LIGHT_COORDINATE,
                        net.minecraft.client.render.OverlayTexture.DEFAULT_UV);
                    context.getMatrices().pop();
                }

                // Render block entities
                for (BlockPos localPos : contraption.getBlocks().keySet()) {
                    BlockEntity blockEntity = contraption.getBlockEntityClientSide(localPos);
                    if (blockEntity != null) {
                        var renderer = this.client.getBlockEntityRenderDispatcher().get(blockEntity);
                        if (renderer != null) {
                            context.getMatrices().push();
                            context.getMatrices().translate(localPos.getX(), localPos.getY(), localPos.getZ());
                            renderer.render(blockEntity, delta, context.getMatrices(), context.getVertexConsumers(),
                                net.minecraft.client.render.LightmapTextureManager.MAX_LIGHT_COORDINATE,
                                net.minecraft.client.render.OverlayTexture.DEFAULT_UV);
                            context.getMatrices().pop();
                        }
                    }
                }
            }

            // Render the entity (this includes bogeys in CarriageContraptionEntity)
            EntityRenderDispatcher dispatcher = this.client.getEntityRenderDispatcher();
            dispatcher.render(carriage, 0, 0, 0, 0, delta, context.getMatrices(),
                context.getVertexConsumers(), net.minecraft.client.render.LightmapTextureManager.MAX_LIGHT_COORDINATE);

            context.getMatrices().pop();
            
            // Flush all rendering
            context.getVertexConsumers().draw();
            
            // Restore rendering state
            DiffuseLighting.disableGuiDepthLighting();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();

        } catch (Exception e) {
            // Ultimate fallback - show indicator text
            try {
                context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("[Carriage Preview]").formatted(Formatting.GRAY), 0, 0, 0xAAAAAA);
            } catch (Exception ignored) {
            }
        }

        context.getMatrices().pop();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (amount != 0) {
            modelScale = Math.max(5.0f, Math.min(100.0f, modelScale + (float) amount * 2.0f));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) { // Right-click
            isDragging = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) { // Right-click
            isDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDragging && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            modelRotationY += (float) (mouseX - lastMouseX);
            modelRotationX += (float) (mouseY - lastMouseY);
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (currentCarriageIndex > 0) {
                currentCarriageIndex--;
                updateToggleButton();
                return true;
            }
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (currentCarriageIndex < carriages.size() - 1) {
                currentCarriageIndex++;
                updateToggleButton();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
