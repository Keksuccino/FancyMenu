package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

/**
 * Mixin invoker for the private inner class GuiGraphics.ScissorStack.
 * <p>
 * This allows invoking the {@code peek()} method on an instance of the ScissorStack.
 * The mixin targets the class using its fully qualified string name because it is private.
 */
@Mixin(GuiGraphics.ScissorStack.class)
public interface IMixinScissorStack {

    /**
     * Invokes the {@code peek()} method on the ScissorStack instance.
     * <p>
     * This method returns the current scissor rectangle at the top of the stack
     * without removing it.
     *
     * @return The current {@link ScreenRectangle} for scissoring, or {@code null} if the stack is empty.
     */
    @Invoker("peek") @Nullable ScreenRectangle invoke_peek_FancyMenu();

}