package de.keksuccino.fancymenu.customization.element.elements.audio;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.resources.ResourceSupplier;
import de.keksuccino.fancymenu.util.resources.audio.IAudio;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AudioElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final DrawableColor BACKGROUND_COLOR = DrawableColor.of(new Color(149, 196, 22));
    private static final Map<String, String> CACHED_CURRENT_AUDIOS = new HashMap<>();

    //TODO Cache current audio with element ID and check if audio is cached on load, to continue playing it
    // - Wenn current im cache, dann direkt play(), sonst vorher stop() (für neustart bei nicht gecachten audios)

    @NotNull
    public PlayMode playMode = PlayMode.NORMAL;
    public boolean loop = false;
    public final List<AudioInstance> audios = new ArrayList<>();
    public int currentAudioIndex = -1;
    @Nullable
    public AudioInstance currentAudio;

    public AudioElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    @Override
    public void tick() {

        if (this.currentAudio != null) {

            IAudio a = this.currentAudio.audio.get();

            //Pause the audio in case the element should not load/render due to loading requirements, etc.
            if (!this.shouldRender()) {
                if (a != null) a.stop();
                this.currentAudio = null;
                return;
            }

            //Start current audio if not playing
            if (this.currentAudio != null) {
                if ((a != null) && a.isReady() && !a.isPlaying()) a.play();
            }

        }

    }

    @Override
    public void onCloseScreen() {
        if (this.currentAudio != null) {
            IAudio a = this.currentAudio.audio.get();
            if (a != null) a.pause();
            this.currentAudio = null;
        }
    }

    protected void pickNextAudio() {
        if (this.currentAudioIndex == -1) {
            if (!this.audios.isEmpty())
        }
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;

        if (isEditor()) {
            int x = this.getAbsoluteX();
            int y = this.getAbsoluteY();
            int w = this.getAbsoluteWidth();
            int h = this.getAbsoluteHeight();
            RenderSystem.enableBlend();
            fill(pose, x, y, x + w, y + h, BACKGROUND_COLOR.getColorInt());
            enableScissor(x, y, w, h);
            drawCenteredString(pose, Minecraft.getInstance().font, Component.translatable("fancymenu.elements.audio"), x, y - (Minecraft.getInstance().font.lineHeight / 2), -1);
            disableScissor();
            RenderingUtils.resetShaderColor();
        }

    }

    public enum PlayMode implements LocalizedCycleEnum<PlayMode> {

        NORMAL("normal"),
        SHUFFLE("shuffle");

        private final String name;

        PlayMode(@NotNull String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getLocalizationKeyBase() {
            return "fancymenu.elements.audio.play_mode";
        }

        @Override
        public @NotNull Style getValueComponentStyle() {
            return LocalizedCycleEnum.WARNING_TEXT_STYLE.get();
        }

        @Override
        public @NotNull String getName() {
            return this.name;
        }

        @Override
        public @NotNull PlayMode[] getValues() {
            return PlayMode.values();
        }

        @Override
        public @Nullable PlayMode getByNameInternal(@NotNull String name) {
            return getByName(name);
        }

        @Nullable
        public static PlayMode getByName(@NotNull String name) {
            for (PlayMode mode : PlayMode.values()) {
                if (mode.getName().equals(name)) return mode;
            }
            return null;
        }

    }

    public static class AudioInstance {

        ResourceSupplier<IAudio> audio;
        float volume = 1.0F;

        public static void serializeAllToExistingContainer(@NotNull List<AudioInstance> instances, @NotNull PropertyContainer container) {
            int i = 0;
            for (AudioInstance instance : instances) {
                if (instance.audio != null) container.putProperty("audio_instance_" + i, "" + instance.volume + ";" + instance.audio.getSourceWithPrefix());
                i++;
            }
        }

        @NotNull
        public static List<AudioInstance> deserializeAllOfContainer(@NotNull PropertyContainer container) {
            List<AudioInstance> instances = new ArrayList<>();
            container.getProperties().forEach((key, value) -> {
                if (StringUtils.startsWith(key, "audio_instance_") && StringUtils.contains(value, ';')) {
                    AudioInstance instance = new AudioInstance();
                    String[] serialized = value.split(";", 2);
                    if (MathUtils.isFloat(serialized[0])) {
                        instance.volume = Float.parseFloat(serialized[0]);
                        instance.audio = ResourceSupplier.audio(serialized[1]);
                    }
                    instances.add(instance);
                }
            });
            return instances;
        }

    }

}
