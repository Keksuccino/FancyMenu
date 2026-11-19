package de.keksuccino.fancymenu.util.rendering.scissor;

/**
 * Enables scissors that are on the screen already, in whole GUI pixels.<br>
 * While the body of a PiP window or an editor widget or a scaled child of a container renders, {@code GuiGraphics#enableScissor} maps the scissor
 * from the space of what renders to the screen on its own and rounds it its own way (see {@code MixinGuiGraphics}). Code that maps and rounds
 * a scissor by itself enables it through this, so it does not get mapped a second time. It gets disabled like any other scissor.<br>
 * Implemented by every {@link net.minecraft.client.gui.GuiGraphics} via mixin.
 */
public interface ScreenScissorEnabler {

    void enableScreenScissor_FancyMenu(int minX, int minY, int maxX, int maxY);

}
