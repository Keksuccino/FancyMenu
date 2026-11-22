package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MouseHandler.class)
public interface AccessorMixinMouseHandler {

    @Invoker("onMove")
    void invoke_onMove_FancyMenu(long windowPointer, double xpos, double ypos);

    @Invoker("onButton")
    void invoke_onButton_FancyMenu(long window, MouseButtonInfo buttonInfo, int action);

    @Invoker("onScroll")
    void invoke_onScroll_FancyMenu(long window, double xOffset, double yOffset);

}
