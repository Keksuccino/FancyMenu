package de.keksuccino.fancymenu;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.event.acara.EventPriority;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.GuiBlurRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
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

import java.awt.*;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final DrawableColor TINT = DrawableColor.of(new Color(255, 255, 255, 45));

    private boolean blurFirst = false;
    private boolean blurSecond = false;
    private boolean blurThird = false;

    @EventListener(priority = EventPriority.VERY_LOW)
    public void onRenderPost(RenderScreenEvent.Post e) {

        if (blurFirst) {
            GuiBlurRenderer.renderBlurArea(e.getGraphics(), 50, 40, 300, 300, 4, 6, TINT, e.getPartial());
        }

        if (blurSecond) {
            GuiBlurRenderer.renderBlurArea(e.getGraphics(), e.getScreen().width - 300, e.getScreen().height - 300, 200, 200, 4, 6, TINT, e.getPartial());
        }

        if (blurThird) {
            GuiBlurRenderer.renderBlurArea(e.getGraphics(), e.getScreen().width - 300, 40, 100, 100, 4, 6, TINT, e.getPartial());
        }

    }

    @EventListener
    public void onInitScreenPost(InitOrResizeScreenCompletedEvent e) {

        e.addRenderableWidget(new ExtendedButton(20, 20, 100, 20, "Toggle First Blur", button -> {
            blurFirst = !blurFirst;
        }));
        e.addRenderableWidget(new ExtendedButton(20, 40, 100, 20, "Toggle Second Blur", button -> {
            blurSecond = !blurSecond;
        }));
        e.addRenderableWidget(new ExtendedButton(20, 60, 100, 20, "Toggle Third Blur", button -> {
            blurThird = !blurThird;
        }));

//        e.addRenderableWidget(new ExtendedButton(20, 20, 100, 20, "Open PiP Window", button -> {
//            ColorPickerScreen s = new ColorPickerScreen(null, drawableColor -> {}, drawableColor -> {}, drawableColor -> {});
//            PiPWindowHandler.INSTANCE.openWindowWithDefaultSizeAndPosition(new PiPWindow(Component.translatable("fancymenu.ui.color_picker.title")).setScreen(s), null);
//        }));
//
//        e.addRenderableWidget(new ExtendedButton(20, 45, 100, 20, "Open PiP Window 2", button -> {
//            OptionsScreen s = new OptionsScreen(new TitleScreen(), Minecraft.getInstance().options);
//            PiPWindowHandler.INSTANCE.openWindowWithDefaultSizeAndPosition(new PiPWindow(Component.literal("Some Window")).setScreen(s), null);
//        }));

//        e.addRenderableWidget(new ExtendedButton(20, 20, 100, 20, "INFO without Callback", button -> {
//            Dialogs.openMessage(Component.literal("This is a test."), MessageDialogStyle.INFO);
//        }));
//
//        e.addRenderableWidget(new ExtendedButton(20, 40, 100, 20, "INFO with Callback", button -> {
//            Dialogs.openMessageWithCallback(Component.literal("This is a test."), MessageDialogStyle.INFO, aBoolean -> {
//                Dialogs.openMessage(Component.literal("Level 2 dialog text."), MessageDialogStyle.ERROR);
//            });
//        }));
//
//        e.addRenderableWidget(new ExtendedButton(20, 60, 100, 20, "WARN without Callback", button -> {
//            Dialogs.openMessage(Component.literal("This is a test."), MessageDialogStyle.WARNING);
//        }));
//
//        e.addRenderableWidget(new ExtendedButton(20, 80, 100, 20, "WARN with Callback", button -> {
//            Dialogs.openMessageWithCallback(Component.literal("This is a test."), MessageDialogStyle.WARNING, aBoolean -> {});
//        }));
//
//        e.addRenderableWidget(new ExtendedButton(20, 100, 100, 20, "ERROR without Callback", button -> {
//            Dialogs.openMessage(Component.literal("This is a test."), MessageDialogStyle.ERROR);
//        }));
//
//        e.addRenderableWidget(new ExtendedButton(20, 120, 100, 20, "ERROR with Callback", button -> {
//            Dialogs.openMessageWithCallback(Component.literal("This is a test."), MessageDialogStyle.ERROR, aBoolean -> {});
//        }));

    }

}
