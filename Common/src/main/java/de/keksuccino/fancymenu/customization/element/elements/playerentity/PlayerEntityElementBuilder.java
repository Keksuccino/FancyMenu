package de.keksuccino.fancymenu.customization.element.elements.playerentity;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.events.ModReloadEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("all")
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
        i.baseWidth = 100;
        i.baseHeight = 300;
        return i;
    }

    @Override
    public PlayerEntityElement deserializeElement(@NotNull SerializedElement serialized) {

        PlayerEntityElement element = this.buildDefaultInstance();

        element.setCopyClientPlayer(this.deserializeBoolean(element.copyClientPlayer, serialized.getValue("copy_client_player")));

        if (!element.copyClientPlayer) {

            element.setPlayerName(Objects.requireNonNullElse(serialized.getValue("playername"), element.playerName), true);

            element.autoSkin = this.deserializeBoolean(element.autoSkin, serialized.getValue("auto_skin"));

            element.autoCape = this.deserializeBoolean(element.autoCape, serialized.getValue("auto_cape"));

            element.slim = this.deserializeBoolean(element.slim, serialized.getValue("slim"));

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

        element.scale = this.deserializeNumber(Integer.class, element.scale, serialized.getValue("scale"));

        element.setHasParrotOnShoulder(
                this.deserializeBoolean(element.hasParrotOnShoulder, serialized.getValue("parrot")),
                this.deserializeBoolean(element.parrotOnLeftShoulder, serialized.getValue("parrot_left_shoulder"))
        );

        element.setIsBaby(this.deserializeBoolean(element.isBaby, serialized.getValue("is_baby")));

        element.setCrouching(this.deserializeBoolean(element.crouching, serialized.getValue("crouching")));

        element.setShowPlayerName(this.deserializeBoolean(element.showPlayerName, serialized.getValue("showname")));

        element.followMouse = this.deserializeBoolean(element.followMouse, serialized.getValue("follow_mouse"));

        element.headXRot = this.deserializeNumber(Float.class, element.headXRot, serialized.getValue("headrotationx"));
        element.headYRot = this.deserializeNumber(Float.class, element.headYRot, serialized.getValue("headrotationy"));

        element.bodyXRot = this.deserializeNumber(Float.class, element.bodyXRot, serialized.getValue("bodyrotationx"));
        element.bodyYRot = this.deserializeNumber(Float.class, element.bodyYRot, serialized.getValue("bodyrotationy"));

        element.headZRot = this.deserializeNumber(Float.class, element.headZRot, serialized.getValue("head_z_rot"));

        element.leftArmXRot = this.deserializeNumber(Float.class, element.leftArmXRot, serialized.getValue("left_arm_x_rot"));
        element.leftArmYRot = this.deserializeNumber(Float.class, element.leftArmYRot, serialized.getValue("left_arm_y_rot"));
        element.leftArmZRot = this.deserializeNumber(Float.class, element.leftArmZRot, serialized.getValue("left_arm_z_rot"));

        element.rightArmXRot = this.deserializeNumber(Float.class, element.rightArmXRot, serialized.getValue("right_arm_x_rot"));
        element.rightArmYRot = this.deserializeNumber(Float.class, element.rightArmYRot, serialized.getValue("right_arm_y_rot"));
        element.rightArmZRot = this.deserializeNumber(Float.class, element.rightArmZRot, serialized.getValue("right_arm_z_rot"));

        element.leftLegXRot = this.deserializeNumber(Float.class, element.leftLegXRot, serialized.getValue("left_leg_x_rot"));
        element.leftLegYRot = this.deserializeNumber(Float.class, element.leftLegYRot, serialized.getValue("left_leg_y_rot"));
        element.leftLegZRot = this.deserializeNumber(Float.class, element.leftLegZRot, serialized.getValue("left_leg_z_rot"));

        element.rightLegXRot = this.deserializeNumber(Float.class, element.rightLegXRot, serialized.getValue("right_leg_x_rot"));
        element.rightLegYRot = this.deserializeNumber(Float.class, element.rightLegYRot, serialized.getValue("right_leg_y_rot"));
        element.rightLegZRot = this.deserializeNumber(Float.class, element.rightLegZRot, serialized.getValue("right_leg_z_rot"));

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
        serializeTo.putProperty("headrotationx", "" + element.headXRot);
        serializeTo.putProperty("headrotationy", "" + element.headYRot);
        serializeTo.putProperty("bodyrotationx", "" + element.bodyXRot);
        serializeTo.putProperty("bodyrotationy", "" + element.bodyYRot);
        serializeTo.putProperty("head_z_rot", "" + element.headZRot);
        serializeTo.putProperty("left_arm_x_rot", "" + element.leftArmXRot);
        serializeTo.putProperty("left_arm_y_rot", "" + element.leftArmYRot);
        serializeTo.putProperty("left_arm_z_rot", "" + element.leftArmZRot);
        serializeTo.putProperty("right_arm_x_rot", "" + element.rightArmXRot);
        serializeTo.putProperty("right_arm_y_rot", "" + element.rightArmYRot);
        serializeTo.putProperty("right_arm_z_rot", "" + element.rightArmZRot);
        serializeTo.putProperty("left_leg_x_rot", "" + element.leftLegXRot);
        serializeTo.putProperty("left_leg_y_rot", "" + element.leftLegYRot);
        serializeTo.putProperty("left_leg_z_rot", "" + element.leftLegZRot);
        serializeTo.putProperty("right_leg_x_rot", "" + element.rightLegXRot);
        serializeTo.putProperty("right_leg_y_rot", "" + element.rightLegYRot);
        serializeTo.putProperty("right_leg_z_rot", "" + element.rightLegZRot);

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
