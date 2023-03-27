package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.loadingrequirement.BuildRequirementGroupScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.loadingrequirement.BuildRequirementScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.internal.LoadingRequirementContainer;
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
//        Minecraft.getInstance().setScreen(new TextEditorScreen(Component.literal("Text Editor"), Minecraft.getInstance().screen, null, (text) -> {
//            LogManager.getLogger().info("CLOSED EDITOR RETURNED: " + text);
//        }));
//        Minecraft.getInstance().setScreen(new BuildRequirementScreen(Minecraft.getInstance().screen, Component.literal(Locals.localize("fancymenu.editor.loading_requirement.screens.build_screen.add_requirement")), new LoadingRequirementContainer(), null, (call) -> {
//            LOGGER.info("---- Requirement Builder returned:");
//            if (call == null) {
//                LOGGER.info("NULL");
//            } else {
//                LOGGER.info("Requirement: " + call.requirement.getIdentifier());
//                LOGGER.info("Value: " + call.value);
//            }
//        }));
        Minecraft.getInstance().setScreen(new BuildRequirementGroupScreen(Minecraft.getInstance().screen, Component.literal("Add Group"), new LoadingRequirementContainer(), null, (call) -> {
            if (call != null) {
                LOGGER.info("BUILDER RETURNED: " + call.identifier);
            } else {
                LOGGER.info("BUILDER RETURNED: NULL");
            }
        }));
    });

    @SubscribeEvent
    public void onRenderScreenPost(ScreenEvent.Render.Post e) {

        if (e.getScreen() instanceof TitleScreen) {
            b.render(e.getPoseStack(), e.getMouseX(), e.getMouseY(), e.getPartialTick());
        }

    }

}
