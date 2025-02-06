package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.SerializationUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Objects;

public class AnimationControllerElementBuilder extends ElementBuilder<AnimationControllerElement, AnimationControllerEditorElement> {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();
    private static final Type KEYFRAME_LIST_TYPE = new TypeToken<ArrayList<AnimationKeyframe>>(){}.getType();
    private static final Type IDS_LIST_TYPE = new TypeToken<ArrayList<String>>(){}.getType();

    public AnimationControllerElementBuilder() {
        super("animation_controller");
    }

    @Override
    public @NotNull AnimationControllerElement buildDefaultInstance() {
        AnimationControllerElement element = new AnimationControllerElement(this);
        element.baseWidth = 100;
        element.baseHeight = 100;
        element.inEditorColor = DrawableColor.of(new Color(0, 255, 0, 100));
        return element;
    }

    @Override
    public AnimationControllerElement deserializeElement(@NotNull SerializedElement serialized) {

        AnimationControllerElement element = buildDefaultInstance();

        try {
            // Deserialize target element IDs
            String targetIds = serialized.getValue("target_element_ids");
            if (targetIds != null) {
                ArrayList<String> ids = Objects.requireNonNullElse(GSON.fromJson(targetIds, IDS_LIST_TYPE), new ArrayList<>());
                ids.forEach(s -> element.targetElements.add(new AnimationControllerElement.TargetElement(s)));
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to deserialize target element IDs of AnimationControllerElement!", ex);
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

        element.loop = SerializationUtils.deserializeBoolean(element.loop, serialized.getValue("loop"));

        element.offsetMode = SerializationUtils.deserializeBoolean(element.offsetMode, serialized.getValue("offset_mode"));

        element.ignoreSize = SerializationUtils.deserializeBoolean(element.ignoreSize, serialized.getValue("ignore_size"));

        element.ignorePosition = SerializationUtils.deserializeBoolean(element.ignorePosition, serialized.getValue("ignore_position"));

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull AnimationControllerElement element, @NotNull SerializedElement serializeTo) {

        // Serialize target element IDs
        if (!element.targetElements.isEmpty()) {
            ArrayList<String> ids = new ArrayList<>();
            element.targetElements.forEach(targetElement -> ids.add(targetElement.targetElementId));
            serializeTo.putProperty("target_element_ids", GSON.toJson(ids, IDS_LIST_TYPE));
        }

        // Serialize keyframes 
        if (!element.getKeyframes().isEmpty()) {
            String keyframesJson = GSON.toJson(element.getKeyframes(), KEYFRAME_LIST_TYPE);
            serializeTo.putProperty("keyframes", keyframesJson);
        }

        serializeTo.putProperty("loop", "" + element.loop);

        serializeTo.putProperty("offset_mode", "" + element.offsetMode);

        serializeTo.putProperty("ignore_size", "" + element.ignoreSize);

        serializeTo.putProperty("ignore_position", "" + element.ignorePosition);

        return serializeTo;

    }

    @Override
    public @NotNull AnimationControllerEditorElement wrapIntoEditorElement(@NotNull AnimationControllerElement element, @NotNull LayoutEditorScreen editor) {
        return new AnimationControllerEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Components.translatable("fancymenu.elements.animation_controller");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.desc");
    }

}
