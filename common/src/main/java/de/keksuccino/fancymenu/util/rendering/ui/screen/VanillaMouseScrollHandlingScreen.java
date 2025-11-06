package de.keksuccino.fancymenu.util.rendering.ui.screen;

import de.keksuccino.fancymenu.mixin.hooks.MouseHandlerHooks;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import net.minecraft.client.gui.screens.Screen;

/**
 * Applying this interface to a {@link Screen} makes the game handle even {@link FancyMenuUiComponent}s
 * in aVanilla-like way, instead of handing it in a special way in {@link MouseHandlerHooks}.
 */
public interface VanillaMouseScrollHandlingScreen {
}
