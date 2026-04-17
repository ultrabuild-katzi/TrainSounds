package de.ultrabuild.trainsounds.client.gui;

import de.ultrabuild.trainsounds.client.config.TrainSoundVolumeConfig;
import de.ultrabuild.trainsounds.client.config.TrainSoundVolumeConfigManager;
import de.ultrabuild.trainsounds.client.gui.widget.TrainSoundVolumeSliderWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TrainSoundVolumeScreen extends Screen {

    private static final String[] CHANNELS = {"diesel", "electric", "aux"};

    private final Screen parent;

    public TrainSoundVolumeScreen(Screen parent) {
        super(Text.translatable("screen.trainsounds.volume.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        TrainSoundVolumeConfig config = TrainSoundVolumeConfigManager.getConfig();

        int left = (this.width - 260) / 2;
        int top = this.height / 4;

        for (int i = 0; i < CHANNELS.length; i++) {
            String channel = CHANNELS[i];
            Text channelName = Text.translatable("gui.trainsounds.channel." + channel);
            int y = top + (i * 24);

            TrainSoundVolumeSliderWidget slider = new TrainSoundVolumeSliderWidget(
                    left,
                    y,
                    220,
                    20,
                    channel,
                    channelName,
                    config.getChannelVolume(channel),
                    (id, value) -> {
                        TrainSoundVolumeConfigManager.getConfig().setChannelVolume(id, value);
                        TrainSoundVolumeConfigManager.save();
                    }
            );
            this.addDrawableChild(slider);

            this.addDrawableChild(ButtonWidget.builder(getMuteText(channel), button -> {
                        TrainSoundVolumeConfig cfg = TrainSoundVolumeConfigManager.getConfig();
                        cfg.setMuted(channel, !cfg.isMuted(channel));
                        TrainSoundVolumeConfigManager.save();
                        button.setMessage(getMuteText(channel));
                    })
                    .dimensions(left + 226, y, 34, 20)
                    .build());
        }

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions((this.width - 200) / 2, this.height - 28, 200, 20)
                .build());
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 16, 0xFFFFFF);
    }

    private Text getMuteText(String channel) {
        boolean muted = TrainSoundVolumeConfigManager.getConfig().isMuted(channel);
        if (muted) {
            return Text.literal("🔇").formatted(Formatting.RED);
        }

        return Text.literal("🔊").formatted(Formatting.GREEN);
    }
}

