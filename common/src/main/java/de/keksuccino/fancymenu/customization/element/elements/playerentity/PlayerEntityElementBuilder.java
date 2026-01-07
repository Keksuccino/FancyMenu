package de.keksuccino.fancymenu.customization.element.elements.playerentity;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

public class PlayerEntityElementBuilder extends ElementBuilder<PlayerEntityElement, PlayerEntityEditorElement> {

    public PlayerEntityElementBuilder() {
        super("player_entity_v2");
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

            element.setPlayerName(serialized.getValue("playername"));
            element.autoSkin = this.deserializeBoolean(element.autoSkin, serialized.getValue("auto_skin"));
            element.autoCape = this.deserializeBoolean(element.autoCape, serialized.getValue("auto_cape"));
            element.slim = this.deserializeBoolean(element.slim, serialized.getValue("slim"));

            if (!element.autoSkin) {
                //Legacy support
                String skinUrl = serialized.getValue("skinurl");
                String skinPath = serialized.getValue("skinpath");
                if (skinUrl != null) {
                    element.setSkinBySource(skinUrl);
                } else if (skinPath != null) {
                    element.setSkinBySource(skinPath);
                }
                //Modern source deserialization
                String skinResourceSource = serialized.getValue("skin_source");
                if (skinResourceSource != null) {
                    element.setSkinBySource(skinResourceSource);
                }
            } else {
                element.setSkinByPlayerName();
            }

            if (!element.autoCape) {
                //Legacy support
                String capeUrl = serialized.getValue("capeurl");
                String capePath = serialized.getValue("capepath");
                if (capeUrl != null) {
                    element.setCapeBySource(capeUrl);
                } else if (capePath != null) {
                    element.setCapeBySource(capePath);
                }
                //Modern source deserialization
                String capeResourceSource = serialized.getValue("cape_source");
                if (capeResourceSource != null) {
                    element.setCapeBySource(capeResourceSource);
                }
            } else {
                element.setCapeByPlayerName();
            }

        }

        element.setHasParrotOnShoulder(
                this.deserializeBoolean(element.hasParrotOnShoulder, serialized.getValue("parrot")),
                this.deserializeBoolean(element.parrotOnLeftShoulder, serialized.getValue("parrot_left_shoulder"))
        );

        element.setIsBaby(this.deserializeBoolean(element.isBaby, serialized.getValue("is_baby")));

        element.pose = Objects.requireNonNullElse(PlayerEntityElement.PlayerPose.getByName(serialized.getValue("player_pose")), PlayerEntityElement.PlayerPose.STANDING);

        element.bodyMovement = this.deserializeBoolean(element.bodyMovement, serialized.getValue("body_movement"));

        element.setShowPlayerName(this.deserializeBoolean(element.showPlayerName, serialized.getValue("showname")));

        boolean isLegacyFollowMouse = serialized.getValue("follow_mouse") != null;
        boolean legacyFollowMouse = this.deserializeBoolean(false, serialized.getValue("follow_mouse"));
        element.headFollowsMouse = !isLegacyFollowMouse ? this.deserializeBoolean(element.headFollowsMouse, serialized.getValue("head_follows_mouse")) : legacyFollowMouse;
        element.bodyFollowsMouse = !isLegacyFollowMouse ? this.deserializeBoolean(element.bodyFollowsMouse, serialized.getValue("body_follows_mouse")) : legacyFollowMouse;

        element.headXRot = serialized.getValue("headrotationx");
        element.headYRot = serialized.getValue("headrotationy");
        element.headZRot = serialized.getValue("head_z_rot");

        element.bodyXRot = serialized.getValue("bodyrotationx");
        element.bodyYRot = serialized.getValue("bodyrotationy");
        element.bodyZRot = serialized.getValue("bodyrotationz");

        element.leftArmXRot = serialized.getValue("left_arm_x_rot");
        element.leftArmYRot = serialized.getValue("left_arm_y_rot");
        element.leftArmZRot = serialized.getValue("left_arm_z_rot");

        element.rightArmXRot = serialized.getValue("right_arm_x_rot");
        element.rightArmYRot = serialized.getValue("right_arm_y_rot");
        element.rightArmZRot = serialized.getValue("right_arm_z_rot");

        element.leftLegXRot = serialized.getValue("left_leg_x_rot");
        element.leftLegYRot = serialized.getValue("left_leg_y_rot");
        element.leftLegZRot = serialized.getValue("left_leg_z_rot");

        element.rightLegXRot = serialized.getValue("right_leg_x_rot");
        element.rightLegYRot = serialized.getValue("right_leg_y_rot");
        element.rightLegZRot = serialized.getValue("right_leg_z_rot");

        element.bodyXRotAdvancedMode = this.deserializeBoolean(element.bodyXRotAdvancedMode, serialized.getValue("body_x_rot_advanced_mode"));
        element.bodyYRotAdvancedMode = this.deserializeBoolean(element.bodyYRotAdvancedMode, serialized.getValue("body_y_rot_advanced_mode"));
        element.bodyZRotAdvancedMode = this.deserializeBoolean(element.bodyZRotAdvancedMode, serialized.getValue("body_z_rot_advanced_mode"));
        element.headXRotAdvancedMode = this.deserializeBoolean(element.headXRotAdvancedMode, serialized.getValue("head_x_rot_advanced_mode"));
        element.headYRotAdvancedMode = this.deserializeBoolean(element.headYRotAdvancedMode, serialized.getValue("head_y_rot_advanced_mode"));
        element.headZRotAdvancedMode = this.deserializeBoolean(element.headZRotAdvancedMode, serialized.getValue("head_z_rot_advanced_mode"));
        element.leftArmXRotAdvancedMode = this.deserializeBoolean(element.leftArmXRotAdvancedMode, serialized.getValue("left_arm_x_rot_advanced_mode"));
        element.leftArmYRotAdvancedMode = this.deserializeBoolean(element.leftArmYRotAdvancedMode, serialized.getValue("left_arm_y_rot_advanced_mode"));
        element.leftArmZRotAdvancedMode = this.deserializeBoolean(element.leftArmZRotAdvancedMode, serialized.getValue("left_arm_z_rot_advanced_mode"));
        element.rightArmXRotAdvancedMode = this.deserializeBoolean(element.rightArmXRotAdvancedMode, serialized.getValue("right_arm_x_rot_advanced_mode"));
        element.rightArmYRotAdvancedMode = this.deserializeBoolean(element.rightArmYRotAdvancedMode, serialized.getValue("right_arm_y_rot_advanced_mode"));
        element.rightArmZRotAdvancedMode = this.deserializeBoolean(element.rightArmZRotAdvancedMode, serialized.getValue("right_arm_z_rot_advanced_mode"));
        element.leftLegXRotAdvancedMode = this.deserializeBoolean(element.leftLegXRotAdvancedMode, serialized.getValue("left_leg_x_rot_advanced_mode"));
        element.leftLegYRotAdvancedMode = this.deserializeBoolean(element.leftLegYRotAdvancedMode, serialized.getValue("left_leg_y_rot_advanced_mode"));
        element.leftLegZRotAdvancedMode = this.deserializeBoolean(element.leftLegZRotAdvancedMode, serialized.getValue("left_leg_z_rot_advanced_mode"));
        element.rightLegXRotAdvancedMode = this.deserializeBoolean(element.rightLegXRotAdvancedMode, serialized.getValue("right_leg_x_rot_advanced_mode"));
        element.rightLegYRotAdvancedMode = this.deserializeBoolean(element.rightLegYRotAdvancedMode, serialized.getValue("right_leg_y_rot_advanced_mode"));
        element.rightLegZRotAdvancedMode = this.deserializeBoolean(element.rightLegZRotAdvancedMode, serialized.getValue("right_leg_z_rot_advanced_mode"));

        element.leftHandWearable = PlayerEntityElement.Wearable.deserialize(serialized.getValue("left_hand_wearable"));
        element.rightHandWearable = PlayerEntityElement.Wearable.deserialize(serialized.getValue("right_hand_wearable"));
        element.headWearable = PlayerEntityElement.Wearable.deserialize(serialized.getValue("head_wearable"));
        element.chestWearable = PlayerEntityElement.Wearable.deserialize(serialized.getValue("chest_wearable"));
        element.legsWearable = PlayerEntityElement.Wearable.deserialize(serialized.getValue("legs_wearable"));
        element.feetWearable = PlayerEntityElement.Wearable.deserialize(serialized.getValue("feet_wearable"));

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull PlayerEntityElement element, @NotNull SerializedElement serializeTo) {

        serializeTo.putProperty("copy_client_player", "" + element.copyClientPlayer);
        serializeTo.putProperty("playername", element.playerName);
        serializeTo.putProperty("auto_skin", "" + element.autoSkin);
        serializeTo.putProperty("auto_cape", "" + element.autoCape);
        serializeTo.putProperty("slim", "" + element.slim);
        if (element.skinTextureSupplier != null) {
            serializeTo.putProperty("skin_source", element.skinTextureSupplier.getSourceWithPrefix());
        }
        if (element.capeTextureSupplier != null) {
            serializeTo.putProperty("cape_source", element.capeTextureSupplier.getSourceWithPrefix());
        }
        serializeTo.putProperty("parrot", "" + element.hasParrotOnShoulder);
        serializeTo.putProperty("parrot_left_shoulder", "" + element.parrotOnLeftShoulder);
        serializeTo.putProperty("is_baby", "" + element.isBaby);
        serializeTo.putProperty("body_movement", "" + element.bodyMovement);
        serializeTo.putProperty("player_pose", element.pose.name);
        serializeTo.putProperty("showname", "" + element.showPlayerName);
        serializeTo.putProperty("head_follows_mouse", "" + element.headFollowsMouse);
        serializeTo.putProperty("body_follows_mouse", "" + element.bodyFollowsMouse);
        serializeTo.putProperty("headrotationx", element.headXRot);
        serializeTo.putProperty("headrotationy", element.headYRot);
        serializeTo.putProperty("bodyrotationx", element.bodyXRot);
        serializeTo.putProperty("bodyrotationy", element.bodyYRot);
        serializeTo.putProperty("bodyrotationz", element.bodyZRot);
        serializeTo.putProperty("head_z_rot", element.headZRot);
        serializeTo.putProperty("left_arm_x_rot", element.leftArmXRot);
        serializeTo.putProperty("left_arm_y_rot", element.leftArmYRot);
        serializeTo.putProperty("left_arm_z_rot", element.leftArmZRot);
        serializeTo.putProperty("right_arm_x_rot", element.rightArmXRot);
        serializeTo.putProperty("right_arm_y_rot", element.rightArmYRot);
        serializeTo.putProperty("right_arm_z_rot", element.rightArmZRot);
        serializeTo.putProperty("left_leg_x_rot", element.leftLegXRot);
        serializeTo.putProperty("left_leg_y_rot", element.leftLegYRot);
        serializeTo.putProperty("left_leg_z_rot", element.leftLegZRot);
        serializeTo.putProperty("right_leg_x_rot", element.rightLegXRot);
        serializeTo.putProperty("right_leg_y_rot", element.rightLegYRot);
        serializeTo.putProperty("right_leg_z_rot", element.rightLegZRot);
        serializeTo.putProperty("body_x_rot_advanced_mode", "" + element.bodyXRotAdvancedMode);
        serializeTo.putProperty("body_y_rot_advanced_mode", "" + element.bodyYRotAdvancedMode);
        serializeTo.putProperty("body_z_rot_advanced_mode", "" + element.bodyZRotAdvancedMode);
        serializeTo.putProperty("head_x_rot_advanced_mode", "" + element.headXRotAdvancedMode);
        serializeTo.putProperty("head_y_rot_advanced_mode", "" + element.headYRotAdvancedMode);
        serializeTo.putProperty("head_z_rot_advanced_mode", "" + element.headZRotAdvancedMode);
        serializeTo.putProperty("left_arm_x_rot_advanced_mode", "" + element.leftArmXRotAdvancedMode);
        serializeTo.putProperty("left_arm_y_rot_advanced_mode", "" + element.leftArmYRotAdvancedMode);
        serializeTo.putProperty("left_arm_z_rot_advanced_mode", "" + element.leftArmZRotAdvancedMode);
        serializeTo.putProperty("right_arm_x_rot_advanced_mode", "" + element.rightArmXRotAdvancedMode);
        serializeTo.putProperty("right_arm_y_rot_advanced_mode", "" + element.rightArmYRotAdvancedMode);
        serializeTo.putProperty("right_arm_z_rot_advanced_mode", "" + element.rightArmZRotAdvancedMode);
        serializeTo.putProperty("left_leg_x_rot_advanced_mode", "" + element.leftLegXRotAdvancedMode);
        serializeTo.putProperty("left_leg_y_rot_advanced_mode", "" + element.leftLegYRotAdvancedMode);
        serializeTo.putProperty("left_leg_z_rot_advanced_mode", "" + element.leftLegZRotAdvancedMode);
        serializeTo.putProperty("right_leg_x_rot_advanced_mode", "" + element.rightLegXRotAdvancedMode);
        serializeTo.putProperty("right_leg_y_rot_advanced_mode", "" + element.rightLegYRotAdvancedMode);
        serializeTo.putProperty("right_leg_z_rot_advanced_mode", "" + element.rightLegZRotAdvancedMode);

        serializeTo.putProperty("left_hand_wearable", element.leftHandWearable.serialize());
        serializeTo.putProperty("right_hand_wearable", element.rightHandWearable.serialize());
        serializeTo.putProperty("head_wearable", element.headWearable.serialize());
        serializeTo.putProperty("chest_wearable", element.chestWearable.serialize());
        serializeTo.putProperty("legs_wearable", element.legsWearable.serialize());
        serializeTo.putProperty("feet_wearable", element.feetWearable.serialize());

        return serializeTo;
        
    }

    @Override
    public @NotNull PlayerEntityEditorElement wrapIntoEditorElement(@NotNull PlayerEntityElement element, @NotNull LayoutEditorScreen editor) {
        return new PlayerEntityEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.elements.player_entity");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.desc");
    }

}
