package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.logo;

import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import org.jetbrains.annotations.NotNull;

public class TitleScreenLogoDeepElement extends AbstractDeepElement {

    protected LogoRenderer logoRenderer = new LogoRenderer(true);

    public TitleScreenLogoDeepElement(DeepElementBuilder<?, ?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.posOffsetX = (getScreenWidth() / 2) - 137;
        this.posOffsetY = 30;
        this.baseWidth = 155 + 119;
        this.baseHeight = 52;

        if (!this.shouldRender()) return;

        this.logoRenderer.renderLogo(graphics, getScreenWidth(), 1.0F);

    }

}