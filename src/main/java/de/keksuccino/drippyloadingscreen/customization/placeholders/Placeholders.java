package de.keksuccino.drippyloadingscreen.customization.placeholders;

import de.keksuccino.drippyloadingscreen.customization.placeholders.bars.ProgressHeightPlaceholder;
import de.keksuccino.drippyloadingscreen.customization.placeholders.bars.ProgressWidthPlaceholder;
import de.keksuccino.drippyloadingscreen.customization.placeholders.bars.ProgressXPlaceholder;
import de.keksuccino.drippyloadingscreen.customization.placeholders.bars.ProgressYPlaceholder;
import de.keksuccino.drippyloadingscreen.customization.placeholders.general.*;
import de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderRegistry;

public class Placeholders {

    public static void registerAll() {

        PlaceholderRegistry.registerPlaceholder(new ProgressWidthPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new ProgressHeightPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new ProgressXPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new ProgressYPlaceholder());

        PlaceholderRegistry.registerPlaceholder(new LoadingProgressPercentPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new CpuInfoPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new FpsPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new GpuInfoPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new JavaVersionPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new JavaVMPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new OpenGLInfoPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new OperatingSystemPlaceholder());

    }

}
