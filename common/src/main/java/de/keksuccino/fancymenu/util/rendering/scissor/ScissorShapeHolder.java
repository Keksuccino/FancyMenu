package de.keksuccino.fancymenu.util.rendering.scissor;

import org.jetbrains.annotations.Nullable;

/**
 * Lets a scissor rectangle carry the {@link ScissorShape} it stands for, when the rectangle only is the bounds of a scissor that does not line up
 * with the screen axes. Render states keep the scissor rectangle they got rendered in, so the shape reaches the GUI renderer through it.<br>
 * Implemented by every {@link de.keksuccino.fancymenu.util.rendering.gui.ScreenRectangle} via mixin. Use {@link ScissorShapes} to read and write it.
 */
public interface ScissorShapeHolder {

    @Nullable
    ScissorShape getScissorShape_FancyMenu();

    void setScissorShape_FancyMenu(@Nullable ScissorShape shape);

}
