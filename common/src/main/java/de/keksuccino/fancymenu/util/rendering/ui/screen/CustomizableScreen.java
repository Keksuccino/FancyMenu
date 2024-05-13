package de.keksuccino.fancymenu.util.rendering.ui.screen;

import net.minecraft.client.gui.components.events.GuiEventListener;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;

/**
 * Gets applied to the {@link Screen} class to add helper methods for easier customization.
 */
public interface CustomizableScreen {

    /**
     * Every widget that gets added to the {@link Screen#children()}, {@link Screen#renderables} and {@link Screen#narratables} lists and should,
     * under any circumstances, get removed from said lists before resizing/re-initializing the {@link Screen} (because they get rebuild on init/resize,
     * even if the {@link Screen} does not clear its child list) should get added to this list.
     */
    @NotNull
    List<GuiEventListener> removeOnInitChildrenFancyMenu();

    boolean isScreenInitialized_FancyMenu();

}
