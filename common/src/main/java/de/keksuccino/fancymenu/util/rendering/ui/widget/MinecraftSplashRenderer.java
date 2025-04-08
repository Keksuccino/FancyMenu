package de.keksuccino.fancymenu.util.rendering.ui.widget;

import com.mojang.math.Axis;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinSplashRenderer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class MinecraftSplashRenderer {

    public static final int WIDTH_OFFSET = 123;
    public static final int HEIGHT_OFFSET = 69;

    private static MinecraftSplashRenderer defaultInstance = null;
    private static MinecraftSplashRenderer fallbackInstance = new MinecraftSplashRenderer("ERROR");

    private final String splash;

    public MinecraftSplashRenderer(String splash) {
        this.splash = splash;
    }

    public int getDefaultPositionX(int screenWidth) {
        return (int)(((float)screenWidth / 2.0F) + WIDTH_OFFSET);
    }

    public int getDefaultPositionY() {
        return HEIGHT_OFFSET;
    }

    /**
     * Renders the splash text at the default position.
     *
     * @param guiGraphics the graphics context to render with
     * @param screenWidth the width of the screen
     * @param font the font to use for rendering
     * @param color the color of the text
     */
    public void render(GuiGraphics guiGraphics, int screenWidth, Font font, int color) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((float)screenWidth / 2.0F + WIDTH_OFFSET, HEIGHT_OFFSET, 0.0F);
        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(-20.0F));
        float f = 1.8F - Mth.abs(Mth.sin((float)(Util.getMillis() % 1000L) / 1000.0F * (float) (Math.PI * 2)) * 0.1F);
        f = f * 100.0F / (float)(font.width(this.splash) + 32);
        guiGraphics.pose().scale(f, f, f);
        guiGraphics.drawCenteredString(font, this.splash, 0, -8, 16776960 | color);
        guiGraphics.pose().popPose();
    }

    /**
     * Renders the splash text centered at the specified position.
     *
     * @param guiGraphics the graphics context to render with
     * @param x the x-coordinate to center the splash text at
     * @param y the y-coordinate to center the splash text at
     * @param font the font to use for rendering
     * @param color the color of the text
     */
    public void renderAt(GuiGraphics guiGraphics, int x, int y, Font font, int color) {

        guiGraphics.pose().pushPose();

        // Translate to the specified position
        guiGraphics.pose().translate(x, y, 0.0F);

        // Apply the same rotation as the original
        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(-20.0F));

        // Calculate the pulsing scale factor
        float f = 1.8F - Mth.abs(Mth.sin((float)(Util.getMillis() % 1000L) / 1000.0F * (float) (Math.PI * 2)) * 0.1F);
        f = f * 100.0F / (float)(font.width(this.splash) + 32);

        // Apply scaling
        guiGraphics.pose().scale(f, f, f);

        // Draw the centered text at the origin (which is now at x,y)
        // The -8 vertical offset keeps consistent vertical alignment from the original
        guiGraphics.drawCenteredString(font, this.splash, 0, -8, 16776960 | color);

        guiGraphics.pose().popPose();

    }

    /**
     * Gets the splash text.
     *
     * @return the splash text
     */
    public String getSplash() {
        return this.splash;
    }

    @NotNull
    public static MinecraftSplashRenderer getDefaultInstance() {
        SplashRenderer vanilla = Minecraft.getInstance().getSplashManager().getSplash();
        if (vanilla != null) {
            if (defaultInstance == null) {
                defaultInstance = new MinecraftSplashRenderer(((IMixinSplashRenderer)vanilla).getSplashFancyMenu());
            }
            return defaultInstance;
        }
        return fallbackInstance;
    }

}