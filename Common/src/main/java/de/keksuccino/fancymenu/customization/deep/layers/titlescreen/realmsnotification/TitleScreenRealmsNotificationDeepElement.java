package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.realmsnotification;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class TitleScreenRealmsNotificationDeepElement extends AbstractDeepElement {

    private static final ResourceLocation INVITE_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/invite_icon.png");
    private static final ResourceLocation TRIAL_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/trial_icon.png");
    private static final ResourceLocation NEWS_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/news_notification_mainscreen.png");
    private static final ResourceLocation UNSEEN_NOTIFICATION_ICON_LOCATION = new ResourceLocation("minecraft", "textures/gui/unseen_notification.png");

    public TitleScreenRealmsNotificationDeepElement(DeepElementBuilder<?, ?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;

        RenderSystem.enableBlend();

        int yStart = getScreenHeight() / 4 + 48;
        int realmsButtonY = yStart + 24 * 2;
        this.baseWidth = 14 + 14 + 14 + 16 + 2;
        this.baseHeight = 13;
        this.posOffsetX = ((getScreenWidth() / 2) + 80) + 4 - 2;
        this.posOffsetY = realmsButtonY + 4;

        if (isEditor()) {
            this.drawIcons(pose, mouseX, mouseY);
        }

        RenderingUtils.resetShaderColor();

    }

    private void drawIcons(PoseStack pose, int mouseX, int mouseY) {

        int $$5 = getScreenHeight() / 4 + 48;
        int $$6 = getScreenWidth() / 2 + 80;
        int $$7 = $$5 + 48 + 2;
        int $$8 = -(14 + 14 + 16);

        RenderSystem.setShaderTexture(0, UNSEEN_NOTIFICATION_ICON_LOCATION);
        GuiComponent.blit(pose, $$6 - $$8 + 5, $$7 + 3, 0.0F, 0.0F, 10, 10, 10, 10);
        $$8 += 14;

        RenderSystem.setShaderTexture(0, NEWS_ICON_LOCATION);
        pose.pushPose();
        pose.scale(0.4F, 0.4F, 0.4F);
        GuiComponent.blit(pose, (int)((double)($$6 + 2 - $$8) * 2.5D), (int)((double)$$7 * 2.5D), 0.0F, 0.0F, 40, 40, 40, 40);
        pose.popPose();
        $$8 += 14;

        RenderSystem.setShaderTexture(0, INVITE_ICON_LOCATION);
        GuiComponent.blit(pose, $$6 - $$8, $$7 - 6, 0.0F, 0.0F, 15, 25, 31, 25);
        $$8 += 16;

        RenderSystem.setShaderTexture(0, TRIAL_ICON_LOCATION);
        int $$9 = 0;
        if ((Util.getMillis() / 800L & 1L) == 1L) {
            $$9 = 8;
        }
        GuiComponent.blit(pose, $$6 + 4 - $$8, $$7 + 4, 0.0F, (float)$$9, 8, 8, 8, 16);

    }

}