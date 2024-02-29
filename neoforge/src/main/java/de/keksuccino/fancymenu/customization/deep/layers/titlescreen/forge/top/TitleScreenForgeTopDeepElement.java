package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.forge.top;

import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.ClientHooks;
import org.jetbrains.annotations.NotNull;

public class TitleScreenForgeTopDeepElement extends AbstractDeepElement {

    public TitleScreenForgeTopDeepElement(DeepElementBuilder<?, ?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;

        Font font = Minecraft.getInstance().font;
        if (isEditor()) {
            graphics.setColor(1.0F, 1.0F, 1.0F, this.opacity);
            Component line1 = Component.literal(I18n.get("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.forge.top.example.line1"));
            graphics.drawCenteredString(font, line1, getScreenWidth() / 2, 4 + (0 * (font.lineHeight + 1)), RenderingUtils.replaceAlphaInColor(DrawableColor.WHITE.getColorInt(), this.opacity));
            Component line2 = Component.literal(I18n.get("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.forge.top.example.line2"));
            graphics.drawCenteredString(font, line2, getScreenWidth() / 2, 4 + (1 * (font.lineHeight + 1)), RenderingUtils.replaceAlphaInColor(DrawableColor.WHITE.getColorInt(), this.opacity));
            this.baseWidth = font.width(line1);
            int w2 = font.width(line2);
            if (this.baseWidth < w2) {
                this.baseWidth = w2;
            }
            this.baseHeight = (font.lineHeight * 2) + 1;
            this.posOffsetX = (getScreenWidth() / 2) - (this.getAbsoluteWidth() / 2);
            this.posOffsetY = 4;
        } else {
            ClientHooks.renderMainMenu((TitleScreen) getScreen(), graphics, font, getScreenWidth(), getScreenHeight(), (int) (this.opacity * 255.0F));
        }

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

    }

}