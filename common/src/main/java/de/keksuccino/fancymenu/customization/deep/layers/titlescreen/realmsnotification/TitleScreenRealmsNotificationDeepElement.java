package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.realmsnotification;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
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
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;

        RenderSystem.enableBlend();

        int yStart = getScreenHeight() / 4 + 48;
        int realmsButtonY = yStart + 24 * 2;
        this.baseWidth = 14 + 14 + 14 + 16 + 2;
        this.baseHeight = 13;
        this.posOffsetX = ((getScreenWidth() / 2) + 80) + 4 - 2;
        this.posOffsetY = realmsButtonY + 4;

        if (isEditor()) {
            this.drawIcons(graphics, mouseX, mouseY);
        }

        RenderingUtils.resetShaderColor(graphics);

    }

    private void drawIcons(GuiGraphics graphics, int mouseX, int mouseY) {

        int $$5 = getScreenHeight() / 4 + 48;
        int $$6 = getScreenWidth() / 2 + 80;
        int $$7 = $$5 + 48 + 2;
        int $$8 = -(14 + 14 + 16);

        graphics.blit(UNSEEN_NOTIFICATION_ICON_LOCATION, $$6 - $$8 + 5, $$7 + 3, 0.0F, 0.0F, 10, 10, 10, 10);
        $$8 += 14;

        graphics.pose().pushPose();
        graphics.pose().scale(0.4F, 0.4F, 0.4F);
        graphics.blit(NEWS_ICON_LOCATION, (int)((double)($$6 + 2 - $$8) * 2.5D), (int)((double)$$7 * 2.5D), 0.0F, 0.0F, 40, 40, 40, 40);
        graphics.pose().popPose();
        $$8 += 14;

        graphics.blit(INVITE_ICON_LOCATION, $$6 - $$8, $$7 - 6, 0.0F, 0.0F, 15, 25, 31, 25);
        $$8 += 16;

        int $$9 = 0;
        if ((Util.getMillis() / 800L & 1L) == 1L) {
            $$9 = 8;
        }
        graphics.blit(TRIAL_ICON_LOCATION, $$6 + 4 - $$8, $$7 + 4, 0.0F, (float)$$9, 8, 8, 8, 16);

    }

}