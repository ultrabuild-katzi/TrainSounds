package de.ultrabuild.trainsounds.mixin.client;

import de.ultrabuild.trainsounds.client.gui.TrainSoundVolumeScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.SoundOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundOptionsScreen.class)
public abstract class SoundOptionsScreenMixin {

    @Inject(method = "init", at = @At("HEAD"))
    private void trainsounds$addVolumePanelButton(CallbackInfo ci) {
        SoundOptionsScreen screen = (SoundOptionsScreen) (Object) this;
        int buttonWidth = 20;
        int buttonHeight = 20;
        int x = screen.width - buttonWidth - 8;
        int y = screen.height - buttonHeight - 8;

        ((ScreenAccessor) screen).trainsounds$addDrawableChild(ButtonWidget.builder(Text.literal("🔊"), button -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client != null) {
                        client.setScreen(new TrainSoundVolumeScreen(screen));
                    }
                })
                .dimensions(x, y, buttonWidth, buttonHeight)
                .build());
    }
}

