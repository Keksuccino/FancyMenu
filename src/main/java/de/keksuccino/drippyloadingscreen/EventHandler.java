package de.keksuccino.drippyloadingscreen;

import de.keksuccino.fancymenu.events.RenderScreenEvent;
import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.drippyloadingscreen.customization.DrippyOverlayScreen;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedImageButton;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;

public class EventHandler {

    private static final ResourceLocation EDIT_BUTTON_TEXTURE = new ResourceLocation("drippyloadingscreen", "textures/edit_button.png");

    private static AdvancedButton editLoadingScreenButton = new AdvancedImageButton(-30, 40, 80, 40, EDIT_BUTTON_TEXTURE, true, (press) -> {
        Minecraft.getInstance().setScreen(new DrippyOverlayScreen());
    }) {
        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            this.setDescription(StringUtils.splitLines(I18n.get("drippyloadingscreen.edit_loading_screen.desc"), "\n"));
            UIBase.colorizeButton(this);
            if (this.isHoveredOrFocused()) {
                this.setX(-20);
            } else {
                this.setX(-30);
            }
            super.render(graphics, mouseX, mouseY, partialTicks);
        }
    };

    @SubscribeEvent
    public void onDrawScreenPost(RenderScreenEvent.Post e) {
        if (e.getGui() instanceof TitleScreen) {
            if (FancyMenu.config.getOrDefault("showcustomizationbuttons", true)) {
                editLoadingScreenButton.render(e.getGuiGraphics(), e.getMouseX(), e.getMouseY(), e.getRenderPartialTicks());
            }
        }
    }

}
