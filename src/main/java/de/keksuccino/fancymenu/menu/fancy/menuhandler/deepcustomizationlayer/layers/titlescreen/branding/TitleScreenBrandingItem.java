package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.branding;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.io.IOException;
import java.util.List;

public class TitleScreenBrandingItem extends DeepCustomizationItem {

    protected int lastWidth = 0;
    protected int lastHeight = 0;

    public TitleScreenBrandingItem(DeepCustomizationElement parentElement, PropertiesSection item) {
        super(parentElement, item);
    }

    @Override
    public void render(GuiScreen menu) throws IOException {

        GlStateManager.enableBlend();

        lastWidth = 0;
        lastHeight = 0;

        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        List<String> brandings = Lists.reverse(FMLCommonHandler.instance().getBrandings(true));
        for (int brdline = 0; brdline < brandings.size(); brdline++) {
            String brd = brandings.get(brdline);
            if (!Strings.isNullOrEmpty(brd)) {
                menu.drawString(Minecraft.getMinecraft().fontRenderer, brd, 2, menu.height - ( 10 + brdline * (Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT + 1)), 16777215);
                int w = font.getStringWidth(brd);
                if (lastWidth < w) {
                    lastWidth = w;
                }
                lastHeight += font.FONT_HEIGHT + 1;
            }
        }

        this.setWidth(lastWidth);
        this.setHeight(lastHeight);
        this.posX = 2;
        this.posY = menu.height - 2 - lastHeight;

    }

}