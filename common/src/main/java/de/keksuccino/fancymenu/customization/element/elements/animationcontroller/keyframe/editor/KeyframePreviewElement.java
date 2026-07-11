package de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.editor;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

/** Non-rendering element used as the interactive keyframe preview body. */
final class KeyframePreviewElement extends AbstractElement {

    private final AnimationPreviewViewport viewport;

    public KeyframePreviewElement(@NotNull ElementBuilder<?, ?> builder, @NotNull AnimationPreviewViewport viewport) {
        super(builder);
        this.viewport = viewport;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    @Override
    public int getPositioningScreenWidth() {
        return this.viewport.getDisplayWidth();
    }

    @Override
    public int getPositioningScreenHeight() {
        return this.viewport.getDisplayHeight();
    }

}
