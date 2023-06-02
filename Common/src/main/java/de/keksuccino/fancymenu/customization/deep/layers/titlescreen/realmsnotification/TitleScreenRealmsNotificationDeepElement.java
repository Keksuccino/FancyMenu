package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.realmsnotification;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class TitleScreenRealmsNotificationDeepElement extends AbstractDeepElement {

    private static final ResourceLocation NEWS_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/news_notification_mainscreen.png");

    public TitleScreenRealmsNotificationDeepElement(DeepElementBuilder<?, ?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;

        RenderSystem.enableBlend();

        int yStart = getScreenHeight() / 4 + 48;
        int l = getScreenWidth() / 2 + 80;
        int i1 = yStart + 48 + 2;
        int j1 = 0;
        int xOffset = 20;

        int realmsButtonX = getScreenWidth() / 2 + 2;
        int realmsButtonY = yStart + 24 * 2;
        int realmsButtonWidth = 98;

        RenderSystem.setShaderTexture(0, NEWS_ICON_LOCATION);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.opacity);
        pose.pushPose();
        pose.scale(0.4F, 0.4F, 0.4F);
        blit(pose, (int)(((double)(l + 2 - j1) * 2.5D) + (xOffset / 0.4F)), (int)((double)i1 * 2.5D), 0.0F, 0.0F, 40, 40, 40, 40);
        pose.popPose();

        this.width = 13;
        this.height = 13;
        this.baseX = realmsButtonX + realmsButtonWidth + xOffset - 17;
        this.baseY = realmsButtonY + 4;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    }

}