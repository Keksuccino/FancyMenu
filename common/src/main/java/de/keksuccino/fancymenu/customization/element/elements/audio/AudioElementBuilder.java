package de.keksuccino.fancymenu.customization.element.elements.audio;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.events.ModReloadEvent;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenStartingEvent;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.Trio;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.*;
import java.util.List;

public class AudioElementBuilder extends ElementBuilder<AudioElement, AudioEditorElement> {

    private static final Logger LOGGER = LogManager.getLogger();
    protected static final Map<String, Trio<ResourceSupplier<IAudio>, IAudio, Integer>> CURRENT_AUDIO_CACHE = new HashMap<>();

    protected static boolean screenIsNull = false;

    public AudioElementBuilder() {
        super("audio_v2");
        EventHandler.INSTANCE.registerListenersOf(this);
    }

    @EventListener
    public void onClientTickPre(ClientTickEvent.Pre e) {
        //Stop all audios if screen is null
        if ((Minecraft.getInstance().screen == null) && !screenIsNull) {
            screenIsNull = true;
            stopAllActiveAudios();
        }
    }

    @EventListener
    public void onInitOrResizeStarting(InitOrResizeScreenStartingEvent e) {
        screenIsNull = false;
    }

    @EventListener
    public void onInitOrResizeScreenCompleted(InitOrResizeScreenCompletedEvent e) {
        ScreenCustomizationLayer activeLayer = ScreenCustomizationLayerHandler.getActiveLayer();
        if (ScreenCustomization.isCustomizationEnabledForScreen(e.getScreen()) && (activeLayer != null)) {
            List<String> removeFromCache = new ArrayList<>();
            CustomGuiBaseScreen customGui = (e.getScreen() instanceof CustomGuiBaseScreen c) ? c : null;
            Screen customGuiParentScreen = (customGui != null) ? customGui.getParentScreen() : null;
            ScreenCustomizationLayer customGuiParentScreenLayer = (customGuiParentScreen != null) ? ScreenCustomizationLayerHandler.getLayerOfScreen(customGuiParentScreen) : null;
            CURRENT_AUDIO_CACHE.forEach((s, resourceSupplierIAudioPair) -> {
                AbstractElement element = activeLayer.getElementByInstanceIdentifier(s);
                if ((element == null) && (customGuiParentScreenLayer != null) && customGui.getGuiMetadata().popupMode) {
                    element = customGuiParentScreenLayer.getElementByInstanceIdentifier(s);
                }
                if (element == null) {
                    resourceSupplierIAudioPair.getSecond().stop();
                    removeFromCache.add(s);
                } else if (element instanceof AudioElement a) {
                    boolean audioFound = false;
                    for (AudioElement.AudioInstance instance : a.audios) {
                        if (Objects.equals(instance.supplier.get(), resourceSupplierIAudioPair.getSecond())) {
                            audioFound = true;
                            break;
                        }
                    }
                    if (!audioFound) {
                        resourceSupplierIAudioPair.getSecond().stop();
                        removeFromCache.add(s);
                    }
                }
            });
            for (String s : removeFromCache) {
                CURRENT_AUDIO_CACHE.remove(s);
            }
        } else {
            stopAllActiveAudios();
        }
    }

    @EventListener
    public void onModReload(ModReloadEvent e) {
        LOGGER.info("[FANCYMENU] Clearing Audio element cache..");
        stopAllActiveAudios();
    }

    public static void stopAllActiveAudios() {
        if (CURRENT_AUDIO_CACHE.isEmpty()) {
            return;
        }
        List<IAudio> audiosToStop = new ArrayList<>();
        CURRENT_AUDIO_CACHE.values().forEach(record -> {
            IAudio audio = record.getSecond();
            if (audio != null) {
                audiosToStop.add(audio);
            }
        });
        for (IAudio audio : audiosToStop) {
            try {
                audio.stop();
            } catch (Exception ex) {
                LOGGER.warn("[FANCYMENU] Failed to stop audio instance!", ex);
            }
        }
        CURRENT_AUDIO_CACHE.clear();
    }

    @Override
    public @NotNull AudioElement buildDefaultInstance() {
        AudioElement i = new AudioElement(this);
        i.baseWidth = 100;
        i.baseHeight = 100;
        i.inEditorColor = DrawableColor.of(new Color(92, 166, 239));
        return i;
    }

    @Override
    public AudioElement deserializeElement(@NotNull SerializedElement serialized) {

        AudioElement element = this.buildDefaultInstance();

        //Manually set the identifier early to fix call to getControllerVolume()
        String id = serialized.getValue("instance_identifier");
        element.setInstanceIdentifier(Objects.requireNonNull(id));

        element.audios.addAll(AudioElement.AudioInstance.deserializeAllOfContainer(serialized));

        String playMode = serialized.getValue("play_mode");
        if (playMode != null) element.setPlayMode(Objects.requireNonNullElse(AudioElement.PlayMode.getByName(playMode), AudioElement.PlayMode.NORMAL), false);

        element.setLooping(deserializeBoolean(element.loop, serialized.getValue("looping")), false);

        element.lastControllerVolume = element.getControllerVolume();
        element.volume = deserializeNumber(Float.class, element.volume, serialized.getValue("volume"));
        element.updateVolume();

        String soundSource = serialized.getValue("sound_source");
        if (soundSource != null) element.setSoundSource(Objects.requireNonNullElse(getSoundSourceByName(soundSource), SoundSource.MASTER));

        return element;

    }

    @Nullable
    protected static SoundSource getSoundSourceByName(@NotNull String name) {
        for (SoundSource source : SoundSource.values()) {
            if (source.getName().equals(name)) return source;
        }
        return null;
    }

    @Override
    protected SerializedElement serializeElement(@NotNull AudioElement element, @NotNull SerializedElement serializeTo) {

        AudioElement.AudioInstance.serializeAllToExistingContainer(element.audios, serializeTo);

        serializeTo.putProperty("play_mode", element.playMode.getName());

        serializeTo.putProperty("looping", "" + element.loop);

        serializeTo.putProperty("volume", "" + element.volume);

        serializeTo.putProperty("sound_source", element.soundSource.getName());

        return serializeTo;
        
    }

    @Override
    public @NotNull AudioEditorElement wrapIntoEditorElement(@NotNull AudioElement element, @NotNull LayoutEditorScreen editor) {
        return new AudioEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Components.translatable("fancymenu.elements.audio");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.audio.desc");
    }

}
