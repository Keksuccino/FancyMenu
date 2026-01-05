package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.event.acara.EventPriority;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ColorPickerScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

    @EventListener(priority = EventPriority.VERY_LOW)
    public void onRenderPost(RenderScreenEvent.Post e) {

    }

    @EventListener
    public void onInitScreenPost(InitOrResizeScreenCompletedEvent e) {

        e.addRenderableWidget(new ExtendedButton(20, 20, 100, 20, "Open PiP Window", button -> {
            ColorPickerScreen s = new ColorPickerScreen(null, drawableColor -> {}, drawableColor -> {}, drawableColor -> {});
            PiPWindowHandler.INSTANCE.openWindowWithDefaultSizeAndPosition(new PiPWindow(Component.translatable("fancymenu.ui.color_picker.title")).setScreen(s), null);
        }));

        e.addRenderableWidget(new ExtendedButton(20, 45, 100, 20, "Open PiP Window 2", button -> {
            OptionsScreen s = new OptionsScreen(new TitleScreen(), Minecraft.getInstance().options);
            PiPWindowHandler.INSTANCE.openWindowWithDefaultSizeAndPosition(new PiPWindow(Component.literal("Some Window")).setScreen(s), null);
        }));

    }

}
