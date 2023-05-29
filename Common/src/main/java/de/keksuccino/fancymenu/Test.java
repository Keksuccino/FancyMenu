package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.event.acara.EventListener;
import de.keksuccino.fancymenu.event.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.event.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinScreen;
import de.keksuccino.fancymenu.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.rendering.ui.screen.filechooser.FileChooserScreen;
import de.keksuccino.fancymenu.rendering.ui.widget.Button;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

    private ContextMenu contextMenu;

    @EventListener(priority = -2000)
    public void onInit(InitOrResizeScreenCompletedEvent e) {

        if (!(e.getScreen() instanceof TitleScreen)) return;

        e.addRenderableWidget(new Button(20, 20, 100, 20, "Open File Chooser", (button) -> {
            FileChooserScreen s = FileChooserScreen.create(e.getScreen(), FancyMenu.getGameDirectory(), FancyMenu.getGameDirectory(), (call) -> {
                if (call != null) {
                    LOGGER.info("RETURNED FILE: " + call.getAbsolutePath());
                }
            });
            Minecraft.getInstance().setScreen(s);
        }));

    }

//    @EventListener(priority = -100)
//    public void onRenderPost(RenderScreenEvent.Post e) {
//
//        if (e.getScreen() instanceof TitleScreen) {
//
//            if (MouseInput.isRightMouseDown()) {
//                this.contextMenu.openMenuAtMouse();
//            }
//
//            this.contextMenu.render(e.getPoseStack(), e.getMouseX(), e.getMouseY(), e.getPartial());
//
//        }
//
//    }

}
