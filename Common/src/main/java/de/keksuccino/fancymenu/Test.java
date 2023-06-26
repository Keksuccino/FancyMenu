package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.rendering.ui.screen.filechooser.SaveFileScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceLocation FM_LOGO_LOCATION = new ResourceLocation("fancymenu", "textures/fancymenu_logo_icon.png");

    @EventListener(priority = -2000)
    public void onInit(InitOrResizeScreenEvent.Post e) {

//        if (!(e.getScreen() instanceof TitleScreen)) return;
//
//        e.addRenderableWidget(Button.builder(Component.literal("open save screen"), (button) -> {
//            LOGGER.info("################## CLICK");
//            Minecraft.getInstance().setScreen(SaveFileScreen.build(FancyMenu.getGameDirectory(), null, "txt", (call) -> {
//                LOGGER.info("################ CLICK CALLBACK");
//                if (call != null) {
//                    try {
//                        if (call.isFile()) call.delete();
//                        call.createNewFile();
//                        LogManager.getLogger().info("FILE SAVED AS: " + call.getPath());
//                    } catch (Exception ex) {
//                        ex.printStackTrace();
//                    }
//                }
//                Minecraft.getInstance().setScreen(e.getScreen());
//            }));
//        }).size(100, 20).pos(30, 30).build());

    }

}
