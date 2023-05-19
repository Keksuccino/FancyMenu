package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.forge.top;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ForgeHooksClient;
import org.jetbrains.annotations.NotNull;

public class TitleScreenForgeTopDeepElement extends AbstractDeepElement {

    public TitleScreenForgeTopDeepElement(DeepElementBuilder<?, ?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;

        Font font = Minecraft.getInstance().font;
        if (isEditor()) {
            Component line1 = Component.literal(I18n.get("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.forge.top.example.line1"));
            drawCenteredString(pose, font, line1, getScreenWidth() / 2, 4 + (0 * (font.lineHeight + 1)), -1);
            Component line2 = Component.literal(I18n.get("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.forge.top.example.line2"));
            drawCenteredString(pose, font, line2, getScreenWidth() / 2, 4 + (1 * (font.lineHeight + 1)), -1);
            this.width = font.width(line1);
            int w2 = font.width(line2);
            if (this.width < w2) {
                this.width = w2;
            }
            this.height = (font.lineHeight * 2) + 1;
            this.baseX = (getScreenWidth() / 2) - (this.getWidth() / 2);
            this.baseY = 4;
        } else {
            ForgeHooksClient.renderMainMenu((TitleScreen) getScreen(), pose, font, getScreenWidth(), getScreenHeight(), 255);
        }

    }

}