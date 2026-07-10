package de.keksuccino.fancymenu.mixin.interfaces;

import org.jetbrains.annotations.NotNull;

/**
 * Updates the branding rendered by FancyMenu's title-screen branding widget.
 * Platform mixins use this bridge to preserve branding text transformed by other mods without linking common code to them.
 */
public interface TitleScreenBrandingController {

    void fancymenu$setBrandingText(@NotNull String branding);
}
