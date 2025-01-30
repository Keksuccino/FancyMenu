package de.keksuccino.fancymenu.customization.element.elements.audio;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.Trio;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
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
    private static final DrawableColor BACKGROUND_COLOR = DrawableColor.of(new Color(92, 166, 239));
    private static final long AUDIO_START_COOLDOWN_MS = 2000;

    @NotNull
    protected PlayMode playMode = PlayMode.NORMAL;
    protected boolean loop = false;
    protected float volume = 1.0F;
    @NotNull
    protected SoundSource soundSource = SoundSource.MASTER;
    /** Call {@link AudioElement#resetAudioElementKeepAudios()} after adding or removing audios. **/
    public List<AudioInstance> audios = new ArrayList<>();
    public int currentAudioIndex = -1;
    @Nullable
    public AudioInstance currentAudioInstance;
    public IAudio currentAudio;
    protected boolean currentAudioStarted = false;
    protected long lastAudioStart = -1L;
    protected boolean cacheChecked = false;
    protected float lastControllerVolume = 1.0F;

    public AudioElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    /**
     * To play every track only once per loop (or only once and then the audio stops if the element is not looping).
     */
    protected List<Integer> getAlreadyPlayedShuffleAudios() {
        return this.getMemory().putPropertyIfAbsentAndGet("already_played_shuffle_audios", new ArrayList<>());
    }

    /**
     * Used to not play the same track multiple times in a row
     */
    protected int getLastPlayedLoopShuffleAudio() {
        return this.getMemory().putPropertyIfAbsentAndGet("last_played_loop_shuffle_audio", -10000);
    }

    /**
     * Used to not play the same track multiple times in a row
     */
    protected void setLastPlayedLoopShuffleAudio(int lastPlayedLoopShuffleAudio) {
        this.getMemory().putProperty("last_played_loop_shuffle_audio", lastPlayedLoopShuffleAudio);
    }

    @Override
    public void afterConstruction() {
        if (!AudioElementController.hasMetaFor(this.getInstanceIdentifier())) AudioElementController.putMeta(this.getInstanceIdentifier(), new AudioElementController.AudioElementMeta(this.getInstanceIdentifier(), 1.0F));
    }

    public void renderTick() {

        //Don't play music in editor
        if (isEditor()) return;

        boolean loadedFromCache = false;
        if (!this.cacheChecked) {
            if (AudioElementBuilder.CURRENT_AUDIO_CACHE.containsKey(this.getInstanceIdentifier())) {
                int cached = AudioElementBuilder.CURRENT_AUDIO_CACHE.get(this.getInstanceIdentifier()).getThird();
                if ((this.audios.size()-1) >= cached) {
                    this.currentAudioIndex = cached;
                    loadedFromCache = true;
//                    LOGGER.info("######### CACHED FOUND FOR: INSTANCE ID: " + this.getInstanceIdentifier() + " | CACHED INDEX: " + this.currentAudioIndex + " | CACHED SOURCE: " + AudioElementBuilder.CURRENT_AUDIO_CACHE.get(this.getInstanceIdentifier()).getFirst().getSourceWithPrefix() + " | INSTANCE: " + this);
                }
            }
            this.cacheChecked = true;
        }

        if (this.shouldRender()) {
            if (!loadedFromCache) this.pickNextAudio(false);
        } else if (loadedFromCache) {
            AudioElementBuilder.CURRENT_AUDIO_CACHE.get(this.getInstanceIdentifier()).getSecond().stop();
            AudioElementBuilder.CURRENT_AUDIO_CACHE.remove(this.getInstanceIdentifier());
            if ((this.currentAudio != null) && this.currentAudio.isReady() && this.currentAudio.isPlaying()) {
                this.currentAudio.stop();
            }
//            LOGGER.info("############ STOPPED CACHED AUDIO IN ELEMENT, BECAUSE shouldRender == false" + " | INSTANCE: " + this);
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
//                        LOGGER.info("############ PICKED NEW AUDIO: " + this.currentAudioInstance.supplier.getSourceWithPrefix() + " | INDEX: " + this.currentAudioIndex + " | INSTANCE: " + this);
                        AudioElementBuilder.CURRENT_AUDIO_CACHE.put(this.getInstanceIdentifier(), Trio.of(this.currentAudioInstance.supplier, this.currentAudio, this.currentAudioIndex));
                    }
                }
            }
        }

        if (this.currentAudio != null) {

            //Stop the audio in case the element should not load/render due to loading requirements, etc.
            if (!this.shouldRender()) {
                if (this.currentAudio.isReady() && this.currentAudio.isPlaying()) {
                    this.currentAudio.stop();
                }
                return;
            }

            long now = System.currentTimeMillis();

            //Update current volume, channel if loaded from cache + update lastAudioStart in case current loaded from cache is still playing
            if (loadedFromCache) {
                if (this.currentAudio.isReady() && (this.currentAudio.isPlaying() || this.currentAudio.isPaused())) {
                    this.lastAudioStart = now;
                    this.currentAudioStarted = true;
                }
                this.updateVolume();
                this.currentAudio.setSoundChannel(this.soundSource);
            }

            boolean isOnCooldown = ((this.lastAudioStart + AUDIO_START_COOLDOWN_MS) > now);

            //Start current audio if not playing
            if (!isOnCooldown && (this.currentAudioInstance != null)) {
                if (!this.currentAudioStarted && this.currentAudio.isReady() && !this.currentAudio.isPlaying()) {
                    this.lastAudioStart = now;
                    this.updateVolume();
                    this.currentAudio.setSoundChannel(this.soundSource);
                    this.currentAudio.play();
                    this.currentAudioStarted = true;
//                    LOGGER.info("############## STARTING NEW AUDIO: " + this.currentAudioInstance.supplier.getSourceWithPrefix() + " | INSTANCE: " + this);
                }
            }

            if (this.lastControllerVolume != this.getControllerVolume()) {
                this.updateVolume();
                this.lastControllerVolume = this.getControllerVolume();
            }

        }

    }

    public void pickNextAudio(boolean ignorePaused) {

        //Return if no audios to play
        if (this.audios.isEmpty()) return;
        //-2 = not looping & last track finished
        if (this.currentAudioIndex == -2) return;

        if (this.currentAudioInstance != null) {
            if (this.currentAudio != null) {
                long now = System.currentTimeMillis();
                boolean isOnCooldown = ((this.lastAudioStart + AUDIO_START_COOLDOWN_MS) > now);
                if (!isOnCooldown && this.currentAudioStarted && (!this.currentAudio.isReady() || (!this.currentAudio.isPlaying() && (!this.currentAudio.isPaused() || ignorePaused)))) {
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
            //Clear alreadyPlayed if all tracks played and element is looping
            if (this.loop && indexes.isEmpty() && !this.audios.isEmpty()) {
                this.getAlreadyPlayedShuffleAudios().clear();
                if (this.audios.size() == 1) this.setLastPlayedLoopShuffleAudio(-10000);
                indexes = this.buildShuffleIndexesList();
            }
            if (!indexes.isEmpty()) {
                int pickedIndex = (indexes.size() == 1) ? 0 : MathUtils.getRandomNumberInRange(0, indexes.size()-1);
                this.currentAudioIndex = indexes.get(pickedIndex);
                this.getAlreadyPlayedShuffleAudios().add(this.currentAudioIndex);
                if (this.loop) this.setLastPlayedLoopShuffleAudio(this.currentAudioIndex);
            } else {
                this.currentAudioIndex = -2;
                this.clearCacheForElement();
            }
        } else {
            this.currentAudioIndex++;
            //Restart if end of audio list reached (or stop if not looping)
            if (this.currentAudioIndex > (this.audios.size()-1)) {
                this.currentAudioIndex = this.loop ? 0 : -2;
                if (this.currentAudioIndex == -2) this.clearCacheForElement();
            }
        }
        if ((this.currentAudio != null) && this.currentAudio.isReady()) {
//            LOGGER.info("########### STOPPING OLD CURRENT AUDIO IN skipToNextAudio!" + " | INSTANCE: " + this);
            this.currentAudio.stop();
        }
        this.currentAudioInstance = null;
        this.currentAudio = null;
        this.currentAudioStarted = false;
        if ((this.currentAudioIndex == -2) && forceRestartIfEndReached) {
            this.resetAudioElementKeepAudios();
        }
    }

    public void goToPreviousAudio() {
        if (this.audios.isEmpty()) return;

        if (this.playMode == PlayMode.NORMAL) {
            handleNormalModePrevious();
        } else if (this.playMode == PlayMode.SHUFFLE) {
            handleShuffleModePrevious();
        }

        resetAudioStateAndUpdateCache();
    }

    private void handleNormalModePrevious() {
        int newIndex = this.currentAudioIndex - 1;
        if (newIndex < 0) {
            newIndex = this.loop ? this.audios.size() - 1 : -2;
        }

        if (newIndex >= 0 && newIndex < this.audios.size()) {
            this.currentAudioIndex = newIndex;
        } else {
            this.currentAudioIndex = newIndex;
            if (newIndex == -2) this.clearCacheForElement();
        }
    }

    private void handleShuffleModePrevious() {
        List<Integer> alreadyPlayed = this.getAlreadyPlayedShuffleAudios();
        if (alreadyPlayed.isEmpty()) return;

        // Remove current index from history
        int currentIndex = alreadyPlayed.remove(alreadyPlayed.size() - 1);

        // Update loop tracking if needed
        if (this.loop) {
            this.setLastPlayedLoopShuffleAudio(
                    alreadyPlayed.isEmpty() ? -10000 : alreadyPlayed.get(alreadyPlayed.size() - 1)
            );
        }

        // Determine new current index
        this.currentAudioIndex = alreadyPlayed.isEmpty() ? -1 : alreadyPlayed.get(alreadyPlayed.size() - 1);
    }

    public void goToNextAudio() {
        if (this.audios.isEmpty()) return;

        if (this.playMode == PlayMode.NORMAL) {
            handleNormalModeNext();
        } else if (this.playMode == PlayMode.SHUFFLE) {
            handleShuffleModeNext();
        }

        resetAudioStateAndUpdateCache();
    }

    private void handleNormalModeNext() {
        this.currentAudioIndex++;
        if (this.currentAudioIndex >= this.audios.size()) {
            this.currentAudioIndex = this.loop ? 0 : -2;
        }
    }

    private void handleShuffleModeNext() {
        List<Integer> indexes = this.buildShuffleIndexesList();

        // Reset playback history if looping and no tracks available
        if (indexes.isEmpty() && this.loop && !this.audios.isEmpty()) {
            this.getAlreadyPlayedShuffleAudios().clear();
            if (this.audios.size() == 1) this.setLastPlayedLoopShuffleAudio(-10000);
            indexes = this.buildShuffleIndexesList();
        }

        if (!indexes.isEmpty()) {
            int pickedIndex = (indexes.size() == 1) ? 0 : MathUtils.getRandomNumberInRange(0, indexes.size()-1);
            this.currentAudioIndex = indexes.get(pickedIndex);
            this.getAlreadyPlayedShuffleAudios().add(this.currentAudioIndex);
            if (this.loop) this.setLastPlayedLoopShuffleAudio(this.currentAudioIndex);
        } else {
            this.currentAudioIndex = -2;
        }
    }

    private void resetAudioStateAndUpdateCache() {
        // Stop current audio and reset state
        if (this.currentAudio != null && this.currentAudio.isReady()) {
            this.currentAudio.stop();
        }
        this.currentAudioInstance = null;
        this.currentAudio = null;
        this.currentAudioStarted = false;
        this.lastAudioStart = -1L;

        // Update cache with new index or clear if invalid
        if (this.currentAudioIndex >= 0 && this.currentAudioIndex < this.audios.size()) {
            AudioInstance instance = this.audios.get(this.currentAudioIndex);
            if (instance != null) {
                this.currentAudioInstance = instance;
                this.currentAudio = instance.supplier.get();
                if (this.currentAudio != null) {
                    AudioElementBuilder.CURRENT_AUDIO_CACHE.put(
                            this.getInstanceIdentifier(),
                            Trio.of(instance.supplier, this.currentAudio, this.currentAudioIndex)
                    );
                }
            }
        } else {
            this.clearCacheForElement();
        }
    }

    @NotNull
    protected List<Integer> buildShuffleIndexesList() {
        List<Integer> indexes = new ArrayList<>();
        if (this.playMode != PlayMode.SHUFFLE) return indexes;

        // Create weighted pool of indices
        List<Integer> weightedPool = new ArrayList<>();
        int i = 0;
        for (AudioInstance audio : this.audios) {
            // Add index multiple times based on weight
            int occurrences = Math.max(1, Math.round(audio.weight * 10)); // Scale weight by 10 for more granular control
            for (int j = 0; j < occurrences; j++) {
                weightedPool.add(i);
            }
            i++;
        }

        // Remove already played tracks
        weightedPool.removeIf(integer -> this.getAlreadyPlayedShuffleAudios().contains(integer));

        // Remove last played track if looping (to avoid repeats)
        if (this.loop && (this.getLastPlayedLoopShuffleAudio() != -10000)) {
            weightedPool.removeIf(integer -> integer == this.getLastPlayedLoopShuffleAudio());
        }

        // Convert weighted pool to unique indices
        Set<Integer> uniqueIndexes = new LinkedHashSet<>(weightedPool);
        return new ArrayList<>(uniqueIndexes);
    }

    public void resetAudioElementKeepAudios() {
//        LOGGER.info("########### RESETTING AUDIO ELEMENT" + " | INSTANCE: " + this, new Throwable());
        if ((this.currentAudio != null) && this.currentAudio.isReady()) {
            this.currentAudio.stop();
        }
        this.currentAudioInstance = null;
        this.currentAudio = null;
        this.currentAudioStarted = false;
        this.currentAudioIndex = -1;
        this.lastAudioStart = -1;
        this.getMemory().clear();
        this.clearCacheForElement();
    }

    public void clearCacheForElement() {
        if (AudioElementBuilder.CURRENT_AUDIO_CACHE.containsKey(this.getInstanceIdentifier())) {
            Trio<ResourceSupplier<IAudio>, IAudio, Integer> cache = AudioElementBuilder.CURRENT_AUDIO_CACHE.get(this.getInstanceIdentifier());
            if ((cache != null) && cache.getSecond().isReady()) cache.getSecond().stop();
//            LOGGER.info("######### CLEAR CACHE FOR ELEMENT !!!!!!!!!!" + " | INSTANCE: " + this, new Throwable());
            AudioElementBuilder.CURRENT_AUDIO_CACHE.remove(this.getInstanceIdentifier());
        }
    }

    public void setLooping(boolean loop, boolean resetElement) {
        this.loop = loop;
        if (resetElement) this.resetAudioElementKeepAudios();
    }

    public boolean isLooping() {
        return this.loop;
    }

    public void setPlayMode(@NotNull PlayMode mode, boolean resetElement) {
        this.playMode = Objects.requireNonNull(mode);
        if (resetElement) this.resetAudioElementKeepAudios();
    }

    @NotNull
    public PlayMode getPlayMode() {
        return this.playMode;
    }

    public void updateVolume() {
        float v = Math.max(0.0F, Math.min(1.0F, this.getControllerVolume() * this.volume));
        if (this.currentAudio != null) {
            this.currentAudio.setVolume(v);
        }
    }

    /**
     * Value between 0.0F and 1.0F.
     */
    public void setVolume(float volume) {
        if (volume > 1.0F) volume = 1.0F;
        if (volume < 0.0F) volume = 0.0F;
        this.volume = volume;
        this.updateVolume();
    }

    /**
     * Value between 0.0F and 1.0F.
     */
    public float getVolume() {
        return this.volume;
    }

    /**
     * Returns the volume of this element that is set in the {@link AudioElementController}.<br>
     * The controller volume is set by actions and similar things that are user-controlled in most cases.
     */
    public float getControllerVolume() {
        if (!AudioElementController.hasMetaFor(this.getInstanceIdentifier())) AudioElementController.putMeta(this.getInstanceIdentifier(), new AudioElementController.AudioElementMeta(this.getInstanceIdentifier(), 1.0F));
        AudioElementController.AudioElementMeta meta = AudioElementController.getMeta(this.getInstanceIdentifier());
        if (meta != null) return Math.max(0.0F, Math.min(1.0F, meta.volume));
        return 1.0F;
    }

    public void setControllerVolume(float volume) {
        AudioElementController.AudioElementMeta meta = AudioElementController.getMeta(this.getInstanceIdentifier());
        if (meta == null) {
            meta = new AudioElementController.AudioElementMeta(this.getInstanceIdentifier(), 1.0F);
            AudioElementController.putMeta(this.getInstanceIdentifier(), meta);
        }
        meta.volume = Math.max(0.0F, Math.min(1.0F, volume));
        AudioElementController.syncChanges();
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
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (isEditor()) {
            int x = this.getAbsoluteX();
            int y = this.getAbsoluteY();
            int w = this.getAbsoluteWidth();
            int h = this.getAbsoluteHeight();
            RenderSystem.enableBlend();
            graphics.fill(RenderType.guiOverlay(), x, y, x + w, y + h, BACKGROUND_COLOR.getColorInt());
            graphics.enableScissor(x, y, x + w, y + h);
            graphics.drawCenteredString(Minecraft.getInstance().font, this.getDisplayName(), x + (w / 2), y + (h / 2) - (Minecraft.getInstance().font.lineHeight / 2), -1);
            graphics.disableScissor();
        }

    }

    @Override
    public void renderTick_Inner_Stage_1() {
        this.renderTick();
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
        public ResourceSupplier<IAudio> supplier;
        float weight = 1.0f; // Default weight of 1.0

        public AudioInstance(@NotNull ResourceSupplier<IAudio> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        public AudioInstance(@NotNull ResourceSupplier<IAudio> supplier, float weight) {
            this.supplier = Objects.requireNonNull(supplier);
            this.weight = Math.max(0.0f, weight); // Ensure non-negative weight
        }

        public static void serializeAllToExistingContainer(@NotNull List<AudioInstance> instances, @NotNull PropertyContainer container) {
            int i = 0;
            for (AudioInstance instance : instances) {
                container.putProperty("audio_instance_" + i, instance.supplier.getSourceWithPrefix());
                container.putProperty("audio_instance_weight_" + i, String.valueOf(instance.weight));
                i++;
            }
        }

        @NotNull
        public static List<AudioInstance> deserializeAllOfContainer(@NotNull PropertyContainer container) {
            List<AudioInstance> instances = new ArrayList<>();
            container.getProperties().forEach((key, value) -> {
                if (StringUtils.startsWith(key, "audio_instance_") && !key.contains("weight")) {
                    float weight = 1.0f;
                    String weightKey = "audio_instance_weight_" + key.substring("audio_instance_".length());
                    String weightValue = container.getValue(weightKey);
                    if (weightValue != null && MathUtils.isFloat(weightValue)) {
                        weight = Float.parseFloat(weightValue);
                    }
                    instances.add(new AudioInstance(ResourceSupplier.audio(value), weight));
                }
            });
            return instances;
        }

    }

}
