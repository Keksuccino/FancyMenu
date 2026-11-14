package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.rendering.scissor.ScissorShape;
import de.keksuccino.fancymenu.util.rendering.scissor.ScissorShapeHolder;
import de.keksuccino.fancymenu.util.rendering.gui.ScreenRectangle;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Gives scissor rectangles the shape of the scissor they are the bounds of. The field is no record component,
 * so equality, hashing and the record's own constructors stay untouched, and every new rectangle starts without a shape.
 */
@Mixin(value = ScreenRectangle.class, remap = false)
public class MixinScreenRectangle implements ScissorShapeHolder {

    @Unique @Nullable
    private ScissorShape scissorShape_FancyMenu;

    @Unique
    @Override
    public @Nullable ScissorShape getScissorShape_FancyMenu() {
        return this.scissorShape_FancyMenu;
    }

    @Unique
    @Override
    public void setScissorShape_FancyMenu(@Nullable ScissorShape shape) {
        this.scissorShape_FancyMenu = shape;
    }

}
