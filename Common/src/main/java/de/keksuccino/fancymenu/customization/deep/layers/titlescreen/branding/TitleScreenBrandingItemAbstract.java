package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.branding;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.properties.PropertyContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;

import java.io.IOException;

public class TitleScreenBrandingItemAbstract extends AbstractDeepElement {

    protected int lastWidth = 0;
    protected int lastHeight = 0;

    public TitleScreenBrandingItemAbstract(DeepElementBuilder parentElement, PropertyContainer item) {
        super(parentElement, item);
    }

    @Override
    public void render(PoseStack matrix, Screen menu) throws IOException {

        RenderSystem.enableBlend();

        lastWidth = 0;
        lastHeight = 0;

        Font font = Minecraft.getInstance().font;
        Services.COMPAT.renderTitleScreenDeepCustomizationBranding(matrix, font, menu, lastWidth, lastHeight,
                (width) -> { lastWidth = width; },
                (height) -> { lastHeight = height; }
        );

        this.width = lastWidth;
        this.height = lastHeight;
        this.baseX = 2;
        this.baseY = menu.height - 2 - lastHeight;

    }

}