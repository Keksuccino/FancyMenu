package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorScreen;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;

public class Test {

    private AdvancedButton b = new AdvancedButton(20, 20, 100, 20, "Open Text Editor", true, (press) -> {
        Minecraft.getInstance().setScreen(new TextEditorScreen(Component.literal("Text Editor"), Minecraft.getInstance().screen, null, (text) -> {
            LogManager.getLogger().info("CLOSED EDITOR RETURNED: " + text);
        }));
    });

    @SubscribeEvent
    public void onRenderScreenPost(ScreenEvent.Render.Post e) {

        if (e.getScreen() instanceof TitleScreen) {
            b.render(e.getPoseStack(), e.getMouseX(), e.getMouseY(), e.getPartialTick());
        }

    }

}
