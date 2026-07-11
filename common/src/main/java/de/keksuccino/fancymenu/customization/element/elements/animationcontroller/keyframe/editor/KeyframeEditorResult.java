package de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.editor;

import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframe;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframeSequence;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Result returned when the keyframe editor is confirmed. */
public record KeyframeEditorResult(@NotNull List<AnimationKeyframe> keyframes, boolean offsetMode) {

    public KeyframeEditorResult {
        keyframes = AnimationKeyframeSequence.copyAndSort(keyframes);
    }

}
