package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.event.acara.EventPriority;
import de.keksuccino.fancymenu.util.rendering.ui.blur.GuiBlurUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

    @EventListener(priority = EventPriority.VERY_LOW)
    public void onRenderPost(RenderScreenEvent.Post e) {

//        GuiBlurUtils.applyBlurArea(e.getGraphics(), 20, 20, 200, 300, -1, 0.5F, true);
//
//        e.getGraphics().drawString(Minecraft.getInstance().font, "This is a test.", 50, 50, UIBase.getUIColorTheme().warning_text_color.getColorInt());

    }

}
