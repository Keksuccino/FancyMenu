package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.actions.BuildActionScreen;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

    private AdvancedButton b = new AdvancedButton(20, 20, 100, 20, "Fancy Button", true, (press) -> {
        Minecraft.getInstance().setScreen(new BuildActionScreen(Minecraft.getInstance().screen, null, (call) -> {
            LOGGER.info("SCREEN RETURNED: " + call);
        }));
    });

    @SubscribeEvent
    public void onRenderScreenPost(ScreenEvent.Render.Post e) {

        if (e.getScreen() instanceof TitleScreen) {
            b.render(e.getPoseStack(), e.getMouseX(), e.getMouseY(), e.getPartialTick());
        }

    }

}
