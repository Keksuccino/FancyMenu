package de.keksuccino.fancymenu.platform.services;

import net.minecraft.network.chat.Component;

import java.util.List;

public interface IPlatformCompatibilityLayer {

    /**
     * Returns the left-aligned title-screen branding block in visual top-to-bottom order.
     * Right-aligned status lines rendered above the copyright notice are not part of this block.
     */
    List<Component> getTitleScreenBrandingLines();

}
