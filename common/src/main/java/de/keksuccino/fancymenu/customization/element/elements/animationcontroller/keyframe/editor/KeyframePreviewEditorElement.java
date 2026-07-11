package de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.editor;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.gui.GuiRenderTypes;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

import static de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen.ELEMENT_DRAG_CRUMPLE_ZONE;

/** Interactive resize/move wrapper dedicated to the keyframe preview. */
final class KeyframePreviewEditorElement extends AbstractEditorElement<KeyframePreviewEditorElement, KeyframePreviewElement> {

    private static final DrawableColor NORMAL_COLOR = DrawableColor.of(new Color(33, 176, 58));
    private static final DrawableColor RECORDING_COLOR = DrawableColor.of(new Color(196, 37, 37));
    private static final DrawableColor SELECTED_COLOR = DrawableColor.of(new Color(180, 37, 196));

    private final BooleanSupplier recordingSupplier;
    private final IntSupplier selectionSizeSupplier;
    private boolean elementMovingStarted;
    private boolean resizingStarted;

    public KeyframePreviewEditorElement(@NotNull KeyframePreviewElement element, @NotNull LayoutEditorScreen editor, @NotNull BooleanSupplier recordingSupplier, @NotNull IntSupplier selectionSizeSupplier) {
        super(element, editor);
        this.recordingSupplier = recordingSupplier;
        this.selectionSizeSupplier = selectionSizeSupplier;
        this.settings.setFadeable(false);
        this.settings.setAdvancedSizingSupported(false);
        this.settings.setAdvancedPositioningSupported(false);
        this.settings.setOpacityChangeable(false);
        this.settings.setDelayable(false);
        this.settings.setElementAnchorPointAllowed(false);
        this.settings.setStretchable(false);
        this.settings.setVanillaAnchorPointAllowed(false);
        this.settings.setOrderable(false);
        this.settings.setCopyable(false);
        this.settings.setDestroyable(false);
        this.settings.setIdentifierCopyable(false);
    }

    @Override
    public void init() {
        super.init();
        this.topLeftDisplay.clearLines();
        this.bottomRightDisplay.clearLines();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.renderPreviewBody(graphics);
        super.render(graphics, mouseX, mouseY, partial);
    }

    public void renderPreviewBody(@NotNull GuiGraphics graphics) {
        DrawableColor color = NORMAL_COLOR;
        if (this.recordingSupplier.getAsBoolean()) color = RECORDING_COLOR;
        if (this.selectionSizeSupplier.getAsInt() == 1) color = SELECTED_COLOR;
        graphics.fill(GuiRenderTypes.gui(), this.element.getAbsoluteX(), this.element.getAbsoluteY(), this.element.getAbsoluteX() + this.element.getAbsoluteWidth(), this.element.getAbsoluteY() + this.element.getAbsoluteHeight(), color.getColorInt());
    }

    public boolean wasRecentlyResized() {
        return this.recentlyResized;
    }

    public boolean wasRecentlyMovedByDragging() {
        return this.recentlyMovedByDragging;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int draggingDiffX = (int)(mouseX - this.leftMouseDownMouseX);
        int draggingDiffY = (int)(mouseY - this.leftMouseDownMouseY);
        this.movingCrumpleZonePassed = (Math.abs(draggingDiffX) >= ELEMENT_DRAG_CRUMPLE_ZONE) || (Math.abs(draggingDiffY) >= ELEMENT_DRAG_CRUMPLE_ZONE);
        if (this.movingCrumpleZonePassed && !this.elementMovingStarted) {
            this.updateMovingStartPos((int)mouseX, (int)mouseY);
            this.elementMovingStarted = true;
        }
        if (!this.resizingStarted) {
            this.updateResizingStartPos((int)mouseX, (int)mouseY);
            this.resizingStarted = true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.movingCrumpleZonePassed = false;
        this.elementMovingStarted = false;
        this.resizingStarted = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

}
