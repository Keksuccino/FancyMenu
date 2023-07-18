package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.rendering.ui.TestUIComponent;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceLocation FM_LOGO_LOCATION = new ResourceLocation("fancymenu", "textures/fancymenu_logo.png");

    private static final TestUIComponent COMP = new TestUIComponent();

    @EventListener(priority = -2000)
    public void onRenderPost(RenderScreenEvent.Post e) {

//        COMP.render(e.getPoseStack(), e.getMouseX(), e.getMouseY(), e.getPartial());

        UIBase.fillF(e.getPoseStack(), COMP.getX(), COMP.getY(), COMP.getX() + COMP.getWidth(), COMP.getY() + COMP.getHeight(), UIBase.getUIColorScheme().error_text_color.getColorInt());

    }

}
