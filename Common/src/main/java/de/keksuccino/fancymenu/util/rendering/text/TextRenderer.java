package de.keksuccino.fancymenu.util.rendering.text;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class TextRenderer {

    public static int draw(@NotNull PoseStack pose, @NotNull Font font, @NotNull Component text, float x, float y, int color) {
        pose.pushPose();
        pose.translate(x, y, 0F);
        int i = font.draw(pose, text, 0F, 0F, color);
        pose.popPose();
        return i;
    }

    public static int drawShadow(@NotNull PoseStack pose, @NotNull Font font, @NotNull Component text, float x, float y, int color) {
        pose.pushPose();
        pose.translate(x, y, 0F);
        int i = font.drawShadow(pose, text, 0F, 0F, color);
        pose.popPose();
        return i;
    }

    public static int draw(@NotNull PoseStack pose, @NotNull Font font, @NotNull String text, float x, float y, int color) {
        return draw(pose, font, Component.literal(text), x, y, color);
    }

    public static int drawShadow(@NotNull PoseStack pose, @NotNull Font font, @NotNull String text, float x, float y, int color) {
        return drawShadow(pose, font, Component.literal(text), x, y, color);
    }

}
