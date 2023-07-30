package de.keksuccino.fancymenu.util.rendering.text.markdown;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

public class MarkdownTextFragment implements Renderable, GuiEventListener {

    public final MarkdownRenderer parent;
    public final String text;
    public float x;
    public float y;
    public float unscaledWidth;
    public float unscaledHeight;
    public boolean naturalLineBreakAfter;
    public boolean shadow = true;
    public boolean bold;
    public boolean italic;
    public boolean strikethrough;
    public Hyperlink hyperlink = null;
    @NotNull
    public HeadlineType headlineType = HeadlineType.NONE;
    public boolean quote;
    public boolean quoteStart;
    public boolean hovered = false;
    public float scale = 1.0F;

    public MarkdownTextFragment(@NotNull MarkdownRenderer parent, @NotNull String text) {
        this.parent = parent;
        this.text = text;
        this.unscaledHeight = this.parent.font.lineHeight;
    }

    //TODO render line under headline in full markdown renderer width (not just fragment width)

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (this.isSeparationLine()) {

            this.unscaledWidth = this.parent.getRealWidth();

            //TODO render as separation line

        } else {

            //Update scale
            if (this.headlineType == HeadlineType.NONE) this.scale = 1.0F;
            if (this.headlineType == HeadlineType.BIG) this.scale = 1.5F;
            if (this.headlineType == HeadlineType.BIGGER) this.scale = 2F;
            if (this.headlineType == HeadlineType.BIGGEST) this.scale = 2.5F;

            this.hovered = this.isMouseOver(mouseX, mouseY);

            RenderSystem.enableBlend();
            pose.pushPose();
            pose.scale(this.scale, this.scale, this.scale);
            if (this.shadow) {
                this.parent.font.drawShadow(pose, this.buildRenderComponent(), (int) this.getScaledX(), (int) this.getScaledY(), this.parent.textBaseColor.getColorInt());
            } else {
                this.parent.font.draw(pose, this.buildRenderComponent(), (int) this.getScaledX(), (int) this.getScaledY(), this.parent.textBaseColor.getColorInt());
            }
            pose.popPose();
            RenderingUtils.resetShaderColor();

        }

    }

    @NotNull
    protected Component buildRenderComponent() {
        Style style = Style.EMPTY
                .withBold(this.bold)
                .withItalic(this.italic)
                .withStrikethrough(this.strikethrough);
        if (this.hyperlink != null) {
            style = style.withColor(this.parent.hyperlinkColor.getColorInt());
            if (this.hyperlink.isHovered()) style = style.withUnderlined(true);
        }
        return Component.literal(this.text).setStyle(style);
    }

    public boolean isSeparationLine() {
        return this.text.startsWith("---");
    }

    protected void updateWidth() {
        this.unscaledWidth = this.parent.font.width(this.buildRenderComponent());
    }

    public float getScaledX() {
        return this.x / this.scale;
    }

    public float getScaledY() {
        return this.y / this.scale;
    }

    public float getScaledWidth() {
        return this.unscaledWidth * this.scale;
    }

    public float getScaledHeight() {
        return this.unscaledHeight * this.scale;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return RenderingUtils.isXYInArea(mouseX, mouseY, this.getScaledX(), this.getScaledY(), this.getScaledWidth(), this.getScaledHeight());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((this.hyperlink != null) && this.hyperlink.isHovered()) {
            WebUtils.openWebLink(this.hyperlink.link);
            return true;
        }
        return false;
    }

    @Override
    public void setFocused(boolean var1) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    public static class Hyperlink {

        public String link = null;
        public final List<MarkdownTextFragment> hyperlinkFragments = new ArrayList<>();

        public boolean isHovered() {
            for (MarkdownTextFragment f : this.hyperlinkFragments) {
                if (f.hovered) return true;
            }
            return false;
        }

    }

    public enum HeadlineType {
        NONE,
        BIG, // ###
        BIGGER, // ##
        BIGGEST // #
    }

}
