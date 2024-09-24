package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.forge.copyright;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraftforge.internal.BrandingControl;
import org.jetbrains.annotations.NotNull;

public class TitleScreenForgeCopyrightDeepElement extends AbstractDeepElement {

    public TitleScreenForgeCopyrightDeepElement(DeepElementBuilder<?, ?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;

        Font font = Minecraft.getInstance().font;

        if (isEditor()) {
            String line = I18n.get("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.forge.copyright.example.line1");
            int lineCount = 0;
            graphics.drawString(font, line, getScreenWidth() - font.width(line) - 1, getScreenHeight() - (11 + (lineCount + 1) * (font.lineHeight + 1)), 16777215);
            this.baseWidth = font.width(line);
            this.baseHeight = font.lineHeight;
            this.posOffsetX = getScreenWidth() - this.getAbsoluteWidth() - 1;
            this.posOffsetY = getScreenHeight() - 11 - font.lineHeight;
        } else {
            BrandingControl.forEachAboveCopyrightLine((brd, brdline) -> {
                graphics.drawString(font, brd, getScreenWidth() - font.width(brd) - 1, getScreenHeight() - (11 + (brdline + 1) * (font.lineHeight + 1)), 16777215);
            });
        }

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

    }

}