package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.realmsnotification;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class TitleScreenRealmsNotificationDeepElement extends AbstractDeepElement {

    protected static final ResourceLocation UNSEEN_NOTIFICATION_SPRITE = ResourceLocation.parse("icon/unseen_notification");
    protected static final ResourceLocation NEWS_SPRITE = ResourceLocation.parse("icon/news");
    protected static final ResourceLocation INVITE_SPRITE = ResourceLocation.parse("icon/invite");
    protected static final ResourceLocation TRIAL_AVAILABLE_SPRITE = ResourceLocation.parse("icon/trial_available");

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
            this.drawIcons(graphics);
        }

        RenderingUtils.resetShaderColor(graphics);

    }

    private void drawIcons(GuiGraphics graphics) {

        int k = getScreenHeight() / 4 + 48;
        int l = getScreenWidth() / 2 + 100;
        int m = k + 48 + 2;
        int n = l - 3;

        graphics.blitSprite(UNSEEN_NOTIFICATION_SPRITE, n - 12, m + 3, 10, 10);
        n -= 16;

        graphics.blitSprite(NEWS_SPRITE, n - 14, m + 1, 14, 14);
        n -= 16;

        graphics.blitSprite(INVITE_SPRITE, n - 14, m + 1, 14, 14);
        n -= 16;

        graphics.blitSprite(TRIAL_AVAILABLE_SPRITE, n - 10, m + 4, 8, 8);

    }

}