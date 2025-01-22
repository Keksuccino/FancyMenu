package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Objects;

public class AnimationControllerElementBuilder extends ElementBuilder<AnimationControllerElement, AnimationControllerEditorElement> {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();
    private static final Type KEYFRAME_LIST_TYPE = new TypeToken<ArrayList<AnimationKeyframe>>(){}.getType();

    public AnimationControllerElementBuilder() {
        super("animation_controller");
    }

    @Override
    public @NotNull AnimationControllerElement buildDefaultInstance() {
        AnimationControllerElement element = new AnimationControllerElement(this);
        element.baseWidth = 100;
        element.baseHeight = 100;
        element.isPaused = false;
        element.pauseStartTime = -1;
        return element;
    }

    @Override
    public AnimationControllerElement deserializeElement(@NotNull SerializedElement serialized) {

        AnimationControllerElement element = buildDefaultInstance();

        // Deserialize target element ID
        String targetId = serialized.getValue("target_element_id");
        if (targetId != null) {
            element.setTargetElementId(targetId);
        }

        // Deserialize keyframes
        String keyframesJson = serialized.getValue("keyframes");
        if (keyframesJson != null) {
            try {
                element.keyframes = Objects.requireNonNullElse(GSON.fromJson(keyframesJson, KEYFRAME_LIST_TYPE), new ArrayList<>());
                element.keyframes.forEach(animationKeyframe -> {
                    if (animationKeyframe.anchorPoint != null) {
                        animationKeyframe.anchorPoint = Objects.requireNonNullElse(ElementAnchorPoints.getAnchorPointByName(animationKeyframe.anchorPoint.getName()), ElementAnchorPoints.TOP_LEFT);
                    } else {
                        animationKeyframe.anchorPoint = ElementAnchorPoints.TOP_LEFT;
                    }
                });
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to deserialize animation keyframes of AnimationControllerElement!", ex);
            }
        }

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull AnimationControllerElement element, @NotNull SerializedElement serializeTo) {

        // Serialize target element ID
        if (element.getTargetElementId() != null) {
            serializeTo.putProperty("target_element_id", element.getTargetElementId());
        }

        // Serialize keyframes 
        if (!element.getKeyframes().isEmpty()) {
            String keyframesJson = GSON.toJson(element.getKeyframes(), KEYFRAME_LIST_TYPE);
            serializeTo.putProperty("keyframes", keyframesJson);
        }

        return serializeTo;

    }

    @Override
    public @NotNull AnimationControllerEditorElement wrapIntoEditorElement(@NotNull AnimationControllerElement element, @NotNull LayoutEditorScreen editor) {
        return new AnimationControllerEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.editor.add.animation_controller");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.editor.add.animation_controller.desc");
    }

}
