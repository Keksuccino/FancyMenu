package de.keksuccino.fancymenu.util.rendering.ui.widget;

import de.keksuccino.fancymenu.util.ClassExtender;
import de.keksuccino.fancymenu.util.resources.RenderableResource;
import net.minecraft.client.gui.components.AbstractSliderButton;
import org.jetbrains.annotations.Nullable;

/**
 * This interface gets applied to the {@link AbstractSliderButton} class to add a bunch of helper methods for easier customization.
 */
@ClassExtender(AbstractSliderButton.class)
public interface CustomizableSlider {

    void setCustomSliderBackgroundNormalFancyMenu(@Nullable RenderableResource background);

    @Nullable
    RenderableResource getCustomSliderBackgroundNormalFancyMenu();

    void setCustomSliderBackgroundHighlightedFancyMenu(@Nullable RenderableResource background);

    @Nullable
    RenderableResource getCustomSliderBackgroundHighlightedFancyMenu();

}
