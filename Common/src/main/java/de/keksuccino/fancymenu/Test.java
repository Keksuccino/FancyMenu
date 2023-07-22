package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.UIComponent;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextListScrollAreaEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceLocation FM_LOGO_LOCATION = new ResourceLocation("fancymenu", "textures/fancymenu_logo.png");

    protected ScrollArea scrollArea = new ScrollArea(0, 0, 0, 0);
    protected boolean init = false;

    @EventListener(priority = -2000)
    public void onRenderPost(RenderScreenEvent.Post e) {

//        if (!init) {
//            init = true;
//            for (int i = 0; i < 100; i++) {
//                LogManager.getLogger().info("-------- add entry");
//                this.scrollArea.addEntry(new TextListScrollAreaEntry(this.scrollArea, Component.literal("this is a test texttttttttttttttttttttttttttttttttttttttttttttttt"), UIBase.getUIColorScheme().listing_dot_color_1, textListScrollAreaEntry -> {
//                    LogManager.getLogger().info("click");
//                }));
//            }
//        }
//        if (!e.getWidgets().contains(this.scrollArea)) {
//            e.getWidgets().add(0, this.scrollArea);
//        }
//        this.scrollArea.setX(40);
//        this.scrollArea.setY(40);
//        this.scrollArea.setWidth(200);
//        this.scrollArea.setHeight(200);
//        this.scrollArea.render(e.getPoseStack(), e.getMouseX(), e.getMouseY(), e.getPartial());

    }

}
