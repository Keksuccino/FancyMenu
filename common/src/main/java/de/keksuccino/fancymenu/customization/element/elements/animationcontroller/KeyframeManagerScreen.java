package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframe;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.editor.KeyframeEditorResult;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

/**
 * Compatibility entry point for integrations that opened the former root-package editor screen directly.
 * New code should use the editor-package screen and {@link KeyframeEditorResult}.
 */
public class KeyframeManagerScreen extends de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.editor.KeyframeManagerScreen {

    public KeyframeManagerScreen(@NotNull AnimationControllerElement controller, @NotNull Consumer<AnimationControllerMetadata> resultCallback) {
        super(controller, result -> resultCallback.accept(convertResult(result)));
    }

    public KeyframeManagerScreen(@NotNull LayoutEditorScreen parentEditor, @NotNull AnimationControllerElement controller, @NotNull Consumer<AnimationControllerMetadata> resultCallback) {
        super(parentEditor, controller, result -> resultCallback.accept(convertResult(result)));
    }

    private static AnimationControllerMetadata convertResult(KeyframeEditorResult result) {
        return result == null ? null : new AnimationControllerMetadata(result.keyframes(), result.offsetMode());
    }

    public record AnimationControllerMetadata(@NotNull List<AnimationKeyframe> keyframes, boolean isOffsetMode) {
    }

}
