package de.keksuccino.fancymenu.customization.action.actions.audio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPCellWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.RangeSlider;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class PlayAudioAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final long READY_TIMEOUT_MS = 10_000L;
    private static long lastErrorTriggered = -1L;
    private static final Object ACTION_AUDIO_TRACKER_LOCK = new Object();
    private static final Set<IAudio> TRACKED_ACTION_AUDIOS = ConcurrentHashMap.newKeySet();

    public PlayAudioAction() {
        super("play_audio");
    }

    public static void trackActionAudio(@NotNull IAudio audio) {
        Objects.requireNonNull(audio);
        if (audio.isClosed()) return;
        synchronized (ACTION_AUDIO_TRACKER_LOCK) {
            TRACKED_ACTION_AUDIOS.removeIf(IAudio::isClosed);
            TRACKED_ACTION_AUDIOS.add(audio);
        }
    }

    public static int stopAllTrackedActionAudios() {
        List<IAudio> toStop;
        synchronized (ACTION_AUDIO_TRACKER_LOCK) {
            toStop = new ArrayList<>(TRACKED_ACTION_AUDIOS);
            TRACKED_ACTION_AUDIOS.clear();
        }
        int stoppedCount = 0;
        for (IAudio audio : toStop) {
            if ((audio == null) || audio.isClosed()) continue;
            try {
                audio.stop();
                stoppedCount++;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] PlayAudioAction: Failed to stop tracked action audio!", ex);
            }
        }
        return stoppedCount;
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {

        PlayAudioConfig config = PlayAudioConfig.parse(value);
        if (config == null) {
            LOGGER.error("[FANCYMENU] PlayAudioAction: Failed to parse configuration!");
            return;
        }

        if (config.audioSource.isBlank()) {
            LOGGER.error("[FANCYMENU] PlayAudioAction: No audio source provided!");
            return;
        }

        if (!RenderSystem.isOnRenderThread()) {
            long now = System.currentTimeMillis();
            if ((lastErrorTriggered + 60000L) < now) {
                lastErrorTriggered = now;
                MainThreadTaskExecutor.executeInMainThread(
                        () -> ScreenUtils.setScreen(new GenericMessageScreen(Component.translatable("fancymenu.actions.generic.async_error", this.getDisplayName()))),
                        MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            }
            return;
        }

        config.normalize();

        IAudio audio = ResourceSupplier.audio(config.audioSource).get();
        if (audio == null) {
            LOGGER.error("[FANCYMENU] PlayAudioAction: Failed to load audio resource: " + config.audioSource);
            return;
        }

        Runnable playInMainThread = () -> {
            try {
                if (audio.isReady()) {
                    audio.setSoundChannel(config.getSoundSource());
                    audio.setVolume(config.baseVolume);
                    audio.play();
                    trackActionAudio(audio);
                }
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] PlayAudioAction: Failed to play audio: " + config.audioSource, ex);
            }
        };

        if (audio.isReady()) {
            MainThreadTaskExecutor.executeInMainThread(playInMainThread, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
            return;
        }

        Thread waitThread = new Thread(() -> {
            long start = System.currentTimeMillis();
            while ((start + READY_TIMEOUT_MS) > System.currentTimeMillis()) {
                try {
                    if (audio.isLoadingFailed()) {
                        LOGGER.error("[FANCYMENU] PlayAudioAction: Audio resource failed to load: " + config.audioSource);
                        return;
                    }
                    if (audio.isReady()) {
                        MainThreadTaskExecutor.executeInMainThread(playInMainThread, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
                        return;
                    }
                    Thread.sleep(50L);
                } catch (InterruptedException ignored) {
                    return;
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] PlayAudioAction: Error while waiting for audio to become ready: " + config.audioSource, ex);
                    return;
                }
            }
            LOGGER.error("[FANCYMENU] PlayAudioAction: Timeout while waiting for audio to become ready (10s): " + config.audioSource);
        }, "FancyMenu-PlayAudioActionWaiter");
        waitThread.setDaemon(true);
        waitThread.start();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.play_audio");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.play_audio.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.empty();
    }

    @Override
    public String getValuePreset() {
        return PlayAudioConfig.defaultConfig().serialize();
    }

    @Override
    public void editValue(@NotNull ActionInstance instance, @NotNull Action.ActionEditingCompletedFeedback onEditingCompleted, @NotNull Action.ActionEditingCanceledFeedback onEditingCanceled) {
        String oldValue = instance.value;
        boolean[] handled = {false};
        PlayAudioConfig config = PlayAudioConfig.parse(instance.value);
        if (config == null) {
            config = PlayAudioConfig.defaultConfig();
        }
        final PiPWindow[] windowHolder = new PiPWindow[1];
        PlayAudioActionValueScreen s = new PlayAudioActionValueScreen(config, value -> {
            if (handled[0]) {
                return;
            }
            handled[0] = true;
            if (value != null) {
                instance.value = value;
                onEditingCompleted.accept(instance, oldValue, value);
            } else {
                onEditingCanceled.accept(instance);
            }
            PiPWindow window = windowHolder[0];
            if (window != null) {
                window.close();
            }
        });
        PiPWindow window = new PiPWindow(s.getTitle())
                .setScreen(s)
                .setForceFancyMenuUiScale(true)
                .setAlwaysOnTop(true)
                .setBlockMinecraftScreenInputs(true)
                .setForceFocus(true)
                .setMinSize(TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT)
                .setSize(TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT);
        windowHolder[0] = window;
        PiPWindowHandler.INSTANCE.openWindowCentered(window, null);
        window.addCloseCallback(() -> {
            if (handled[0]) {
                return;
            }
            handled[0] = true;
            onEditingCanceled.accept(instance);
        });
    }

    public static class PlayAudioConfig {

        public String audioSource = "";
        public String soundChannel = SoundSource.MASTER.getName();
        public float baseVolume = 1.0F;

        @NotNull
        public static PlayAudioConfig defaultConfig() {
            PlayAudioConfig c = new PlayAudioConfig();
            c.audioSource = ResourceSourceType.LOCAL.getSourcePrefix() + "/config/fancymenu/assets/example.ogg";
            c.soundChannel = SoundSource.MASTER.getName();
            c.baseVolume = 1.0F;
            return c;
        }

        public void normalize() {
            if (this.audioSource == null) this.audioSource = "";
            this.audioSource = this.audioSource.trim();
            if (this.soundChannel == null) this.soundChannel = SoundSource.MASTER.getName();
            this.baseVolume = Math.max(0.0F, Math.min(1.0F, this.baseVolume));
        }

        @NotNull
        public SoundSource getSoundSource() {
            if (this.soundChannel == null) return SoundSource.MASTER;
            for (SoundSource source : SoundSource.values()) {
                if (source.getName().equalsIgnoreCase(this.soundChannel)) {
                    return source;
                }
            }
            return SoundSource.MASTER;
        }

        public void setSoundSource(@NotNull SoundSource soundSource) {
            this.soundChannel = Objects.requireNonNull(soundSource).getName();
        }

        @NotNull
        public String serialize() {
            this.normalize();
            return GSON.toJson(this);
        }

        @Nullable
        public static PlayAudioConfig parse(@Nullable String value) {
            if ((value == null) || value.isBlank()) return null;
            try {
                PlayAudioConfig c = GSON.fromJson(value, PlayAudioConfig.class);
                if (c == null) return null;
                c.normalize();
                return c;
            } catch (Exception ignored) {
                return null;
            }
        }

    }

    public static class PlayAudioActionValueScreen extends PiPCellWindowBody {

        protected final PlayAudioConfig config;
        protected final Consumer<String> callback;
        protected TextInputCell audioSourceCell;

        protected PlayAudioActionValueScreen(@NotNull PlayAudioConfig config, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.actions.play_audio.edit.title"));
            this.config = Objects.requireNonNull(config);
            this.callback = Objects.requireNonNull(callback);
        }

        @Override
        protected void initCells() {

            this.addStartEndSpacerCell();

            // Audio Source
            this.addLabelCell(Component.translatable("fancymenu.actions.play_audio.edit.audio_source"));
            this.audioSourceCell = this.addTextInputCell(null, false, true)
                    .setEditListener(s -> this.config.audioSource = s.trim())
                    .setText(this.config.audioSource);
            this.audioSourceCell.editBox.setUITooltip(() -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.actions.play_audio.edit.audio_source.desc")));

            this.addWidgetCell(new ExtendedButton(0, 0, 20, 20, Component.translatable("fancymenu.actions.play_audio.edit.choose_audio"), button -> {
                ResourceChooserWindowBody<IAudio, ?> chooser = ResourceChooserWindowBody.audio(null, source -> {
                    if (source != null) {
                        this.config.audioSource = source;
                        this.audioSourceCell.setText(source);
                    }
                });
                String chooserPreset = this.config.audioSource;
                if (chooserPreset.isBlank()) {
                    chooserPreset = ResourceSourceType.LOCAL.getSourcePrefix() + "/config/fancymenu/assets/";
                }
                chooser.setSource(chooserPreset, false);
                chooser.openInWindow(null);
            }).setUITooltip(UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.actions.play_audio.edit.choose_audio.desc"))), true);

            this.addWidgetCell(new ExtendedButton(0, 0, 20, 20, Component.translatable("fancymenu.actions.play_audio.edit.clear_audio"), button -> {
                this.config.audioSource = "";
                this.audioSourceCell.setText("");
            }).setUITooltip(UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.actions.play_audio.edit.clear_audio.desc"))), true);

            this.addCellGroupEndSpacerCell();

            // Sound Channel
            CycleButton<SoundSource> soundChannelButton = new CycleButton<>(0, 0, 20, 20,
                    CommonCycles.cycle("fancymenu.actions.play_audio.edit.sound_channel", Arrays.asList(SoundSource.values()), this.config.getSoundSource())
                            .setValueNameSupplier(soundSource -> Component.translatable("soundCategory." + soundSource.getName()).getString())
                            .setValueComponentStyleSupplier(consumes -> Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt())),
                    (value, button) -> this.config.setSoundSource(value));
            soundChannelButton.setUITooltip(UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.actions.play_audio.edit.sound_channel.desc")));
            this.addWidgetCell(soundChannelButton, true);

            this.addCellGroupEndSpacerCell();

            // Base Volume
            this.addLabelCell(Component.translatable("fancymenu.actions.play_audio.edit.base_volume"));
            RangeSlider slider = new RangeSlider(0, 0, 20, 20, Component.empty(), 0.0D, 1.0D, this.config.baseVolume);
            slider.setRoundingDecimalPlace(2);
            slider.setLabelSupplier(consumes -> Component.translatable("fancymenu.actions.play_audio.edit.base_volume.value", Component.literal(this.getVolumePercentage() + "%")));
            slider.setSliderValueUpdateListener((slider1, valueDisplayText, value) -> this.config.baseVolume = (float) ((RangeSlider) slider1).getRangeValue());
            this.addWidgetCell(slider, true);

            this.addStartEndSpacerCell();

        }

        protected int getVolumePercentage() {
            return Math.min(100, Math.max(0, (int) (this.config.baseVolume * 100.0F)));
        }

        @Override
        public boolean allowDone() {
            return (this.config.audioSource != null) && !this.config.audioSource.isBlank();
        }

        @Override
        protected void onCancel() {
            this.callback.accept(null);
        }

        @Override
        protected void onDone() {
            this.callback.accept(this.config.serialize());
        }

        @Override
        protected void autoScaleScreen(AbstractWidget topRightSideWidget) {
        }

    }

}
