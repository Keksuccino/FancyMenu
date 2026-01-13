package de.keksuccino.fancymenu;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.event.acara.EventPriority;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.GuiBlurRenderer;
import de.keksuccino.fancymenu.util.rendering.SmoothRectangleRenderer;
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
    private static final DrawableColor TINT = DrawableColor.of(new Color(255, 0, 234, 95));
    private static final DrawableColor BORDER_TINT = DrawableColor.of(new Color(12, 32, 92, 200));
    private static final float BORDER_THICKNESS = 2.0F;

    private boolean blurFirst = false;
    private boolean blurSecond = false;
    private boolean blurThird = false;

    @EventListener(priority = EventPriority.VERY_LOW)
    public void onRenderPost(RenderScreenEvent.Post e) {

        if (blurFirst) {
            SmoothRectangleRenderer.renderSmoothRect(e.getGraphics(), 50, 40, 300, 300, 4, TINT.getColorInt(), e.getPartial());
            SmoothRectangleRenderer.renderSmoothBorder(e.getGraphics(), 50, 40, 300, 300, BORDER_THICKNESS, 4, BORDER_TINT.getColorInt(), e.getPartial());
        }

        if (blurSecond) {
            SmoothRectangleRenderer.renderSmoothRect(e.getGraphics(), e.getScreen().width - 300, e.getScreen().height - 300, 200, 200, 4, TINT.getColorInt(), e.getPartial());
            SmoothRectangleRenderer.renderSmoothBorder(e.getGraphics(), e.getScreen().width - 300, e.getScreen().height - 300, 200, 200, BORDER_THICKNESS, 4, BORDER_TINT.getColorInt(), e.getPartial());
        }

        if (blurThird) {
            SmoothRectangleRenderer.renderSmoothRect(e.getGraphics(), e.getScreen().width - 300, 40, 100, 100, 4, TINT.getColorInt(), e.getPartial());
            SmoothRectangleRenderer.renderSmoothBorder(e.getGraphics(), e.getScreen().width - 300, 40, 100, 100, BORDER_THICKNESS, 4, BORDER_TINT.getColorInt(), e.getPartial());
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

    }

}
