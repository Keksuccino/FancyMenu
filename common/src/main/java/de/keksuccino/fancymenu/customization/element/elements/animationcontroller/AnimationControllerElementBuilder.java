package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.SerializationHelper;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
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
    private static final Type TARGETS_LIST_TYPE = new TypeToken<ArrayList<AnimationControllerElement.TargetElement>>(){}.getType();
    private static final Type IDS_LIST_TYPE = new TypeToken<ArrayList<String>>(){}.getType();

    public AnimationControllerElementBuilder() {
        super("animation_controller");
    }

    @Override
    public @NotNull AnimationControllerElement buildDefaultInstance() {
        AnimationControllerElement element = new AnimationControllerElement(this);
        element.baseWidth = 100;
        element.baseHeight = 100;
        element.inEditorColor.setDefault(DrawableColor.of(new Color(0, 255, 0, 100)).getHex()).set(DrawableColor.of(new Color(0, 255, 0, 100)).getHex());
        return element;
    }

    @Override
    public AnimationControllerElement deserializeElement(@NotNull SerializedElement serialized) {

        AnimationControllerElement element = buildDefaultInstance();

        boolean loadedTargets = false;
        try {
            // Deserialize target elements (with offsets)
            String targetElementsJson = serialized.getValue("target_elements");
            if (targetElementsJson != null) {
                ArrayList<AnimationControllerElement.TargetElement> targets = Objects.requireNonNullElse(GSON.fromJson(targetElementsJson, TARGETS_LIST_TYPE), new ArrayList<>());
                targets.stream()
                        .filter(target -> target != null && target.targetElementId != null && !target.targetElementId.isEmpty())
                        .forEach(element.targetElements::add);
                loadedTargets = true;
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to deserialize target elements of AnimationControllerElement!", ex);
        }

        if (!loadedTargets) {
            try {
                // Deserialize legacy target element IDs
                String targetIds = serialized.getValue("target_element_ids");
                if (targetIds != null) {
                    ArrayList<String> ids = Objects.requireNonNullElse(GSON.fromJson(targetIds, IDS_LIST_TYPE), new ArrayList<>());
                    ids.forEach(s -> element.targetElements.add(new AnimationControllerElement.TargetElement(s)));
                }
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to deserialize target element IDs of AnimationControllerElement!", ex);
            }
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

        element.loop = SerializationHelper.INSTANCE.deserializeBoolean(element.loop, serialized.getValue("loop"));

        element.offsetMode = SerializationHelper.INSTANCE.deserializeBoolean(element.offsetMode, serialized.getValue("offset_mode"));

        element.ignoreSize = SerializationHelper.INSTANCE.deserializeBoolean(element.ignoreSize, serialized.getValue("ignore_size"));

        element.ignorePosition = SerializationHelper.INSTANCE.deserializeBoolean(element.ignorePosition, serialized.getValue("ignore_position"));

        element.randomTimingOffsetMode = SerializationHelper.INSTANCE.deserializeBoolean(element.randomTimingOffsetMode, serialized.getValue("random_timing_offset_mode"));

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull AnimationControllerElement element, @NotNull SerializedElement serializeTo) {

        // Serialize target elements (with offsets)
        if (!element.targetElements.isEmpty()) {
            ArrayList<AnimationControllerElement.TargetElement> targets = new ArrayList<>(element.targetElements);
            serializeTo.putProperty("target_elements", GSON.toJson(targets, TARGETS_LIST_TYPE));
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

        serializeTo.putProperty("random_timing_offset_mode", "" + element.randomTimingOffsetMode);

        return serializeTo;

    }

    @Override
    public @NotNull AnimationControllerEditorElement wrapIntoEditorElement(@NotNull AnimationControllerElement element, @NotNull LayoutEditorScreen editor) {
        return new AnimationControllerEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.elements.animation_controller");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.desc");
    }

}
