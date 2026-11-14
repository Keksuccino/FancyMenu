package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.rendering.scissor.ScissorShape;
import de.keksuccino.fancymenu.util.rendering.scissor.ScissorShapes;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Deque;
import java.util.Iterator;

@Mixin(targets = "net.minecraft.client.gui.GuiGraphics$ScissorStack")
public class MixinGuiGraphics_ScissorStack {

    @Shadow @Final private Deque<ScreenRectangle> stack;

    /**
     * @reason A scissor inside a shaped scissor (like the scissor of a rotated element) shows only what both of them show,
     * so the rectangle it pushes gets the shape of the outer scissor, cut to its own shape if it has one.
     */
    @Inject(method = "push", at = @At("RETURN"))
    private void after_push_FancyMenu(ScreenRectangle rectangle, CallbackInfoReturnable<ScreenRectangle> info) {
        ScreenRectangle pushed = info.getReturnValue();
        // Without an outer scissor, vanilla pushes the given rectangle itself, which already carries its own shape.
        // The shared empty rectangle hides everything anyway and must never get a shape.
        if ((pushed == rectangle) || (pushed == ScreenRectangle.empty())) return;
        Iterator<ScreenRectangle> fromTop = this.stack.descendingIterator();
        fromTop.next();
        ScreenRectangle outer = fromTop.hasNext() ? fromTop.next() : null;
        ScissorShape shape = ScissorShape.intersect(ScissorShapes.getShape(outer), ScissorShapes.getShape(rectangle));
        if (shape != null) ScissorShapes.setShape(pushed, shape);
    }

}
