package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.branding;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class TitleScreenBrandingDeepElement extends AbstractDeepElement {

    public TitleScreenBrandingDeepElement(DeepElementBuilder<?, ?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;

        RenderSystem.enableBlend();

        Font font = Minecraft.getInstance().font;
        List<Component> lines = Services.COMPAT.getTitleScreenBrandingLines();
        int totalHeight = (font.lineHeight + 1) * lines.size();
        if (totalHeight > 0) {
            totalHeight--;
        }

        this.baseHeight = totalHeight;
        this.posOffsetX = 2;
        this.posOffsetY = getScreenHeight() - 2 - totalHeight;

        int w = 0;
        int i = 0;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.opacity);
        for (Component line : Services.COMPAT.getTitleScreenBrandingLines()) {
            graphics.drawString(font, line, 2, getScreenHeight() - (10 + (i * (font.lineHeight + 1))), DrawableColor.WHITE.getColorIntWithAlpha(this.opacity));
            int lineW = font.width(line);
            if (lineW > w) {
                w = lineW;
            }
            i++;
        }

        this.baseWidth = w;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    }

}