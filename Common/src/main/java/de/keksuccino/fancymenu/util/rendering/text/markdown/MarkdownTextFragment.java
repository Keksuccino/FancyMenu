package de.keksuccino.fancymenu.util.rendering.text.markdown;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MarkdownTextFragment implements Renderable, GuiEventListener {

    public final MarkdownRenderer parent;
    public final String text;
    public float x;
    public float y;
    public float unscaledWidth;
    public float unscaledHeight;
    public boolean naturalLineBreakAfter;
    public boolean endOfWord;
    public boolean separationLine;
    public DrawableColor textColor = null;
    public boolean shadow = true;
    public boolean bold;
    public boolean italic;
    public boolean strikethrough;
    public Hyperlink hyperlink = null;
    @NotNull
    public HeadlineType headlineType = HeadlineType.NONE;
    public QuoteContext quoteContext = null;
    public boolean hovered = false;

    public MarkdownTextFragment(@NotNull MarkdownRenderer parent, @NotNull String text) {
        this.parent = parent;
        this.text = text;
        this.unscaledHeight = this.parent.font.lineHeight;
    }

    //TODO render line under headline in full markdown renderer width (not just fragment width)

    //TODO add HEX color formatting

    //TODO add image support (  [](image_url)  )

    //TODO add code block formatting (wie bei quotes machen -> erstes fragment rendert hintergrund für vollen (mehrzeiligen) block)

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (this.separationLine) {

            this.unscaledWidth = this.parent.getRealWidth();

            //TODO render as separation line

        } else {

            this.hovered = this.isMouseOver(mouseX, mouseY);

            RenderSystem.enableBlend();
            pose.pushPose();
            pose.scale(this.getScale(), this.getScale(), this.getScale());
            if (this.shadow) {
                this.parent.font.drawShadow(pose, this.buildRenderComponent(), (int) this.getScaledX(), (int) this.getScaledY(), this.parent.textBaseColor.getColorInt());
            } else {
                this.parent.font.draw(pose, this.buildRenderComponent(), (int) this.getScaledX(), (int) this.getScaledY(), this.parent.textBaseColor.getColorInt());
            }
            pose.popPose();
            RenderingUtils.resetShaderColor();

            this.renderQuoteLine(pose);

        }

    }

    protected void renderQuoteLine(PoseStack pose) {
        if ((this.quoteContext != null) && (this.quoteContext.getQuoteEnd() != null) && (this.quoteContext.getQuoteEnd() == this)) {
            float yStart = Objects.requireNonNull(this.quoteContext.getQuoteStart()).y - 2;
            float yEnd = this.y + this.getScaledHeight() + 1;
            RenderSystem.enableBlend();
            RenderingUtils.fillF(pose, this.parent.x, yStart, this.parent.x + 2, yEnd, this.parent.quoteColor.getColorInt());
            RenderingUtils.resetShaderColor();
        }
    }

    @NotNull
    protected Component buildRenderComponent() {
        Style style = Style.EMPTY
                .withBold(this.bold)
                .withItalic(this.italic)
                .withStrikethrough(this.strikethrough);
        if (this.quoteContext != null) {
            style = style.withColor(this.parent.quoteColor.getColorInt());
        }
        if (this.textColor != null) {
            style = style.withColor(this.textColor.getColorInt());
        }
        if (this.hyperlink != null) {
            style = style.withColor(this.parent.hyperlinkColor.getColorInt());
            if (this.hyperlink.isHovered()) style = style.withUnderlined(true);
        }
        return Component.literal(this.text).setStyle(style);
    }

    protected void updateWidth() {
        this.unscaledWidth = this.parent.font.width(this.buildRenderComponent());
    }

    public float getScaledX() {
        return this.x / this.getScale();
    }

    public float getScaledY() {
        return this.y / this.getScale();
    }

    public float getScaledWidth() {
        return this.unscaledWidth * this.getScale();
    }

    public float getScaledHeight() {
        return this.unscaledHeight * this.getScale();
    }

    public float getScale() {
        if (this.headlineType == HeadlineType.BIG) return 1.2F;
        if (this.headlineType == HeadlineType.BIGGER) return 1.6F;
        if (this.headlineType == HeadlineType.BIGGEST) return 2F;
        return 1.0F;
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

    public static class QuoteContext {

        public final List<MarkdownTextFragment> quoteFragments = new ArrayList<>();

        @Nullable
        public MarkdownTextFragment getQuoteStart() {
            if (!quoteFragments.isEmpty()) return quoteFragments.get(0);
            return null;
        }

        @Nullable
        public MarkdownTextFragment getQuoteEnd() {
            if (!quoteFragments.isEmpty()) return quoteFragments.get(quoteFragments.size()-1);
            return null;
        }

    }

    public enum HeadlineType {
        NONE,
        BIG, // ###
        BIGGER, // ##
        BIGGEST // #
    }

}
