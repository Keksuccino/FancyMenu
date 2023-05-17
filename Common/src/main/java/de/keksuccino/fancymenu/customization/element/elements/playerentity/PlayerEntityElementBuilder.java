package de.keksuccino.fancymenu.customization.element.elements.playerentity;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.event.events.ModReloadEvent;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.acara.EventListener;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.utils.LocalizationUtils;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class PlayerEntityElementBuilder extends ElementBuilder<PlayerEntityElement, PlayerEntityEditorElement> {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final Map<String, PlayerEntityElement> ELEMENT_CACHE = new HashMap<>();

    public PlayerEntityElementBuilder() {
        super("fancymenu_customization_player_entity");
        EventHandler.INSTANCE.registerListenersOf(this);
    }
    
    @EventListener
    public void onMenuReload(ModReloadEvent e) {
        ELEMENT_CACHE.clear();
        LOGGER.info("[FANCYMENU] PlayerEntity element cache cleared!");
    }

    @Override
    public @NotNull PlayerEntityElement buildDefaultInstance() {
        PlayerEntityElement i = new PlayerEntityElement(this);
        i.width = 100;
        i.height = 300;
        return i;
    }

    @Override
    public PlayerEntityElement deserializeElement(@NotNull SerializedElement serialized) {

        PlayerEntityElement element = this.buildDefaultInstance();

        String copyClientPlayerString = serialized.getValue("copy_client_player");
        if ((copyClientPlayerString != null) && copyClientPlayerString.equals("true")) {
            element.setCopyClientPlayer(true);
        }

        if (!element.copyClientPlayer) {

            String playerNameString = serialized.getValue("playername");
            if (playerNameString != null) {
                element.setPlayerName(playerNameString, true);
            }

            String autoSkinString = serialized.getValue("auto_skin");
            if ((autoSkinString != null) && autoSkinString.equalsIgnoreCase("true")) {
                element.autoSkin = true;
            }

            String autoCapeString = serialized.getValue("auto_cape");
            if ((autoCapeString != null) && autoCapeString.equalsIgnoreCase("true")) {
                element.autoCape = true;
            }

            String slim = serialized.getValue("slim");
            if (slim != null) {
                if (slim.replace(" ", "").equalsIgnoreCase("true")) {
                    element.slim = true;
                }
            }

            if (!element.autoSkin) {
                element.skinUrl = serialized.getValue("skinurl");
                if (element.skinUrl != null) {
                    element.setSkinTextureBySource(element.skinUrl, true);
                }
                element.skinPath = serialized.getValue("skinpath");
                if ((element.skinPath != null) && (element.skinUrl == null)) {
                    element.setSkinTextureBySource(ScreenCustomization.getAbsoluteGameDirectoryPath(element.skinPath), false);
                }
            } else {
                element.setSkinByPlayerName();
            }

            if (!element.autoCape) {
                element.capeUrl = serialized.getValue("capeurl");
                if (element.capeUrl != null) {
                    element.setCapeTextureBySource(element.capeUrl, true);
                }
                element.capePath = serialized.getValue("capepath");
                if ((element.capePath != null) && (element.capeUrl == null)) {
                    element.setCapeTextureBySource(ScreenCustomization.getAbsoluteGameDirectoryPath(element.capePath), false);
                }
            } else {
                element.setCapeByPlayerName();
            }

        }

        String scaleString = serialized.getValue("scale");
        if ((scaleString != null) && MathUtils.isDouble(scaleString)) {
            element.scale = (int) Double.parseDouble(scaleString);
        }

        String hasParrotString = serialized.getValue("parrot");
        if (hasParrotString != null) {
            if (hasParrotString.replace(" ", "").equalsIgnoreCase("true")) {
                element.setHasParrotOnShoulder(true, false);
            }
        }

        String parrotLeftShoulderString = serialized.getValue("parrot_left_shoulder");
        if (parrotLeftShoulderString != null) {
            if (parrotLeftShoulderString.replace(" ", "").equalsIgnoreCase("true")) {
                element.setHasParrotOnShoulder(element.hasParrotOnShoulder, true);
            }
        }

        String isBabyString = serialized.getValue("is_baby");
        if (isBabyString != null) {
            if (isBabyString.replace(" ", "").equalsIgnoreCase("true")) {
                element.setIsBaby(true);
            }
        }

        String crouching = serialized.getValue("crouching");
        if (crouching != null) {
            if (crouching.replace(" ", "").equalsIgnoreCase("true")) {
                element.setCrouching(true);
            }
        }

        String showName = serialized.getValue("showname");
        if (showName != null) {
            if (showName.replace(" ", "").equalsIgnoreCase("false")) {
                element.setShowPlayerName(false);
            }
        }

        String followMouseString = serialized.getValue("follow_mouse");
        if (followMouseString != null) {
            if (followMouseString.replace(" ", "").equalsIgnoreCase("false")) {
                element.followMouse = false;
            }
        }

        String rotX = serialized.getValue("headrotationx");
        if (rotX != null) {
            rotX = rotX.replace(" ", "");
            if (MathUtils.isFloat(rotX)) {
                element.headRotationX = Float.parseFloat(rotX);
            }
        }

        String rotY = serialized.getValue("headrotationy");
        if (rotY != null) {
            rotY = rotY.replace(" ", "");
            if (MathUtils.isFloat(rotY)) {
                element.headRotationY = Float.parseFloat(rotY);
            }
        }

        String bodyrotX = serialized.getValue("bodyrotationx");
        if (bodyrotX != null) {
            bodyrotX = bodyrotX.replace(" ", "");
            if (MathUtils.isFloat(bodyrotX)) {
                element.bodyRotationX = Float.parseFloat(bodyrotX);
            }
        }

        String bodyrotY = serialized.getValue("bodyrotationy");
        if (bodyrotY != null) {
            bodyrotY = bodyrotY.replace(" ", "");
            if (MathUtils.isFloat(bodyrotY)) {
                element.bodyRotationY = Float.parseFloat(bodyrotY);
            }
        }

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull PlayerEntityElement element, @NotNull SerializedElement serializeTo) {

        serializeTo.putProperty("copy_client_player", "" + element.copyClientPlayer);
        if (element.playerName != null) {
            serializeTo.putProperty("playername", element.playerName);
        }
        serializeTo.putProperty("auto_skin", "" + element.autoSkin);
        serializeTo.putProperty("auto_cape", "" + element.autoCape);
        serializeTo.putProperty("slim", "" + element.slim);
        if (element.skinUrl != null) {
            serializeTo.putProperty("skinurl", element.skinUrl);
        }
        if (element.skinPath != null) {
            serializeTo.putProperty("skinpath", element.skinPath);
        }
        if (element.capeUrl != null) {
            serializeTo.putProperty("capeurl", element.capeUrl);
        }
        if (element.capePath != null) {
            serializeTo.putProperty("capepath", element.capePath);
        }
        serializeTo.putProperty("scale", "" + element.scale);
        serializeTo.putProperty("parrot", "" + element.hasParrotOnShoulder);
        serializeTo.putProperty("parrot_left_shoulder", "" + element.parrotOnLeftShoulder);
        serializeTo.putProperty("is_baby", "" + element.isBaby);
        serializeTo.putProperty("crouching", "" + element.crouching);
        serializeTo.putProperty("showname", "" + element.showPlayerName);
        serializeTo.putProperty("follow_mouse", "" + element.followMouse);
        serializeTo.putProperty("headrotationx", "" + element.headRotationX);
        serializeTo.putProperty("headrotationy", "" + element.headRotationY);
        serializeTo.putProperty("bodyrotationx", "" + element.bodyRotationX);
        serializeTo.putProperty("bodyrotationy", "" + element.bodyRotationY);

        return serializeTo;
        
    }

    @Override
    public @NotNull PlayerEntityEditorElement wrapIntoEditorElement(@NotNull PlayerEntityElement element, @NotNull LayoutEditorScreen editor) {
        return new PlayerEntityEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.helper.editor.items.playerentity");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.desc");
    }

}
