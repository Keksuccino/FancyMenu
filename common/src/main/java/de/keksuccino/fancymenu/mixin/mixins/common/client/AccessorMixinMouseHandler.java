package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * The input callbacks of the window, for simulated pointer input of the dev-only decoration overlay preview (see TESTING_GUIDELINES.md).
 */
@Mixin(MouseHandler.class)
public interface AccessorMixinMouseHandler {

    @Invoker("onMove")
    void invoke_onMove_FancyMenu(long windowPointer, double xpos, double ypos);

    @Invoker("onPress")
    void invoke_onPress_FancyMenu(long windowPointer, int button, int action, int modifiers);

}
