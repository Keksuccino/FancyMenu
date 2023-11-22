package de.keksuccino.fancymenu.customization.element.elements.audio;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.Trio;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.resources.ResourceSupplier;
import de.keksuccino.fancymenu.util.resources.audio.IAudio;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;
import java.util.*;
import java.util.List;

public class AudioElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final DrawableColor BACKGROUND_COLOR = DrawableColor.of(new Color(149, 196, 22));
    private static final long AUDIO_START_COOLDOWN_MS = 2000;

    //TODO FIXEN: Audio startet beim ersten laden nicht (siehe log, wird direkt bei laden in skipToNextAudio gestoppt)
    //TODO FIXEN: Audio startet beim ersten laden nicht (siehe log, wird direkt bei laden in skipToNextAudio gestoppt)
    //TODO FIXEN: Audio startet beim ersten laden nicht (siehe log, wird direkt bei laden in skipToNextAudio gestoppt)
    //TODO FIXEN: Audio startet beim ersten laden nicht (siehe log, wird direkt bei laden in skipToNextAudio gestoppt)

    //TODO Actions
    // - Add as new category if possible ("Audio Element"); alternatively add prefix to all Actions ("Audio Element: ...")
    // - Set Volume
    // - Next Track
    // - Previous Track
    // - Toggle Mute
    // - Restart (set to start of first track)

    //TODO Placeholders
    // - Get Volume

    @NotNull
    protected PlayMode playMode = PlayMode.NORMAL;
    protected boolean loop = false;
    protected float volume = 1.0F;
    @NotNull
    protected SoundSource soundSource = SoundSource.MASTER;
    /** Call {@link AudioElement#resetAudioElementKeepAudios()} after adding or removing audios. **/
    public List<AudioInstance> audios = new ArrayList<>();
    protected int currentAudioIndex = -1;
    @Nullable
    protected AudioInstance currentAudioInstance;
    protected IAudio currentAudio;
    protected long lastAudioStart = -1L;
    /** Used when not looping and is shuffling. **/
    protected List<Integer> alreadyPlayedShuffleAudios = new ArrayList<>();
    protected boolean cacheChecked = false;

    public AudioElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    @Override
    public void tick() {

        //Don't play music in editor
        if (isEditor()) return; //TODO Maybe make this a toggleable option in editor???

        boolean loadedFromCache = false;
        if (!this.cacheChecked) {
            if (AudioElementBuilder.CURRENT_AUDIO_CACHE.containsKey(this.getInstanceIdentifier())) {
                int cached = AudioElementBuilder.CURRENT_AUDIO_CACHE.get(this.getInstanceIdentifier()).getThird();
                if ((this.audios.size()-1) >= cached) {
                    this.currentAudioIndex = cached;
                    loadedFromCache = true;
                    //TODO remove debug
                    LOGGER.info("######### CACHED FOUND FOR: INSTANCE ID: " + this.getInstanceIdentifier() + " | CACHED INDEX: " + this.currentAudioIndex + " | CACHED SOURCE: " + AudioElementBuilder.CURRENT_AUDIO_CACHE.get(this.getInstanceIdentifier()).getFirst().getSourceWithPrefix());
                }
            }
            this.cacheChecked = true;
        }

        if (this.shouldRender()) {
            this.pickNextAudio();
        } else if (loadedFromCache) {
            AudioElementBuilder.CURRENT_AUDIO_CACHE.get(this.getInstanceIdentifier()).getSecond().stop();
            AudioElementBuilder.CURRENT_AUDIO_CACHE.remove(this.getInstanceIdentifier());
            this.resetAudioElementKeepAudios();
            //TODO remove debug
            LOGGER.info("############ STOPPED CACHED AUDIO IN ELEMENT, BECAUSE shouldRender == false");
            return;
        }

        //Do nothing in case not looping and end reached
        if (this.currentAudioIndex == -2) return;
        //Do nothing if pickNextAudio() didn't update the index
        if (this.currentAudioIndex == -1) return;

        //Set currentAudio by currentAudioIndex
        if ((this.currentAudioInstance == null) && (this.currentAudioIndex >= 0)) {
            if ((this.audios.size()-1) >= this.currentAudioIndex) {
                this.currentAudioInstance = this.audios.get(this.currentAudioIndex);
                if (this.currentAudioInstance != null) {
                    this.currentAudio = this.currentAudioInstance.supplier.get();
                    if (this.currentAudio != null) {
                        //TODO remove debug
                        LOGGER.info("############ PICKED NEW AUDIO: " + this.currentAudioInstance.supplier.getSourceWithPrefix() + " | INDEX: " + this.currentAudioIndex);
                        AudioElementBuilder.CURRENT_AUDIO_CACHE.put(this.getInstanceIdentifier(), Trio.of(this.currentAudioInstance.supplier, this.currentAudio, this.currentAudioIndex));
                    }
                }
            }
        }

        if (this.currentAudio != null) {

            //Stop the audio in case the element should not load/render due to loading requirements, etc.
            if (!this.shouldRender()) {
                this.resetAudioElementKeepAudios();
                return;
            }

            long now = System.currentTimeMillis();

            //Update current volume, channel if loaded from cache + update lastAudioStart in case current loaded from cache is still playing
            if (loadedFromCache) {
                if (this.currentAudio.isReady() && this.currentAudio.isPlaying()) {
                    this.lastAudioStart = now;
                }
                this.currentAudio.setVolume(this.volume);
                this.currentAudio.setSoundChannel(this.soundSource);
            }

            boolean isOnCooldown = ((this.lastAudioStart + AUDIO_START_COOLDOWN_MS) > now);

            //Start current audio if not playing
            if (!isOnCooldown && (this.currentAudioInstance != null)) {
                if (this.currentAudio.isReady() && !this.currentAudio.isPlaying()) {
                    this.lastAudioStart = now;
                    this.currentAudio.setVolume(this.volume);
                    this.currentAudio.setSoundChannel(this.soundSource);
                    this.currentAudio.play();
                    //TODO remove debug
                    LOGGER.info("############## STARTING NEW AUDIO: " + this.currentAudioInstance.supplier.getSourceWithPrefix());
                }
            }

        }

    }

    protected void pickNextAudio() {

        //Return if no audios to play
        if (this.audios.isEmpty()) return;
        //-2 = not looping & last track finished
        if (this.currentAudioIndex == -2) return;

        if (this.currentAudioInstance != null) {
            if (this.currentAudio != null) {
                long now = System.currentTimeMillis();
                boolean isOnCooldown = ((this.lastAudioStart + AUDIO_START_COOLDOWN_MS) > now);
                if (!isOnCooldown && this.currentAudio.isReady() && !this.currentAudio.isPlaying()) {
                    this.skipToNextAudio(false);
                }
            } else {
                LOGGER.warn("[FANCYMENU] Audio element was unable to load audio track! Skipping to next track, because track was NULL: " + this.currentAudioInstance.supplier.getSourceWithPrefix());
                this.skipToNextAudio(false);
            }
        } else {
            this.skipToNextAudio(false);
        }

    }

    public void skipToNextAudio(boolean forceRestartIfEndReached) {
        if (this.playMode == PlayMode.SHUFFLE) {
            List<Integer> indexes = this.buildShuffleIndexesList();
            if (!indexes.isEmpty()) {
                int pickedIndex = (indexes.size() == 1) ? 0 : MathUtils.getRandomNumberInRange(0, indexes.size()-1);
                this.currentAudioIndex = indexes.get(pickedIndex);
                if (!this.loop) this.alreadyPlayedShuffleAudios.add(this.currentAudioIndex);
            } else {
                this.currentAudioIndex = -2;
            }
        } else {
            this.currentAudioIndex++;
            //Restart if end of audio list reached (or stop if not looping)
            if (this.currentAudioIndex > (this.audios.size()-1)) {
                this.currentAudioIndex = this.loop ? 0 : -2;
            }
        }
        AudioElementBuilder.CURRENT_AUDIO_CACHE.remove(this.getInstanceIdentifier());
        if ((this.currentAudio != null) && this.currentAudio.isReady()) {
            //TODO remove debug
            LOGGER.info("########### STOPPING OLD CURRENT AUDIO IN skipToNextAudio!");
            this.currentAudio.stop();
        }
        this.currentAudioInstance = null;
        this.currentAudio = null;
        if ((this.currentAudioIndex == -2) && forceRestartIfEndReached) {
            this.resetAudioElementKeepAudios();
        }
    }

    @NotNull
    protected List<Integer> buildShuffleIndexesList() {
        List<Integer> indexes = new ArrayList<>();
        if (this.playMode != PlayMode.SHUFFLE) return indexes;
        int i = 0;
        for (AudioInstance ignored : this.audios) {
            indexes.add(i);
            i++;
        }
        if (!this.loop) {
            indexes.removeIf(integer -> this.alreadyPlayedShuffleAudios.contains(integer));
        }
        return indexes;
    }

    public void resetAudioElementKeepAudios() {
        //TODO remove debug
        LOGGER.info("########### RESETTING AUDIO ELEMENT");
        if ((this.currentAudio != null) && this.currentAudio.isReady()) {
            this.currentAudio.stop();
        }
        this.currentAudioInstance = null;
        this.currentAudio = null;
        this.alreadyPlayedShuffleAudios.clear();
        this.currentAudioIndex = -1;
        this.lastAudioStart = -1;
        AudioElementBuilder.CURRENT_AUDIO_CACHE.remove(this.getInstanceIdentifier());
    }

    public void setLooping(boolean loop) {
        this.loop = loop;
        this.resetAudioElementKeepAudios();
    }

    public boolean isLooping() {
        return this.loop;
    }

    public void setPlayMode(@NotNull PlayMode mode) {
        this.playMode = Objects.requireNonNull(mode);
        this.resetAudioElementKeepAudios();
    }

    @NotNull
    public PlayMode getPlayMode() {
        return this.playMode;
    }

    /**
     * Value between 0.0F and 1.0F.
     */
    public void setVolume(float volume) {
        if (volume > 1.0F) volume = 1.0F;
        if (volume < 0.0F) volume = 0.0F;
        this.volume = volume;
        if (this.currentAudio != null) {
            this.currentAudio.setVolume(this.volume);
        }
    }

    /**
     * Value between 0.0F and 1.0F.
     */
    public float getVolume() {
        return this.volume;
    }

    public void setSoundSource(@NotNull SoundSource soundSource) {
        this.soundSource = Objects.requireNonNull(soundSource);
        if (this.currentAudio != null) {
            this.currentAudio.setSoundChannel(soundSource);
        }
    }

    @NotNull
    public SoundSource getSoundSource() {
        return this.soundSource;
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

        @NotNull
        ResourceSupplier<IAudio> supplier;

        public AudioInstance(@NotNull ResourceSupplier<IAudio> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        public static void serializeAllToExistingContainer(@NotNull List<AudioInstance> instances, @NotNull PropertyContainer container) {
            int i = 0;
            for (AudioInstance instance : instances) {
                container.putProperty("audio_instance_" + i, instance.supplier.getSourceWithPrefix());
                i++;
            }
        }

        @NotNull
        public static List<AudioInstance> deserializeAllOfContainer(@NotNull PropertyContainer container) {
            List<AudioInstance> instances = new ArrayList<>();
            container.getProperties().forEach((key, value) -> {
                if (StringUtils.startsWith(key, "audio_instance_")) {
                    instances.add(new AudioInstance(ResourceSupplier.audio(value)));
                }
            });
            return instances;
        }

    }

}
