package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The short guide the layout editor shows in a scrollable area that has no children yet: what the area is for and how to use it.<br>
 * It shows as much as fits into the area. An area that is too small for the whole guide shows what it takes elements by,
 * and the guide as a tooltip.
 */
public final class ScrollAreaEmptyGuide {

    private static final String LOCALIZATION_KEY_BASE = "fancymenu.elements.scroll_area.guide.";
    private static final int TIP_COUNT = 4;
    private static final int PADDING = 8;
    private static final int ICON_SIZE = 16;
    private static final int SECTION_GAP = 5;
    private static final int TIP_GAP = 2;
    /** The space between two lines of text. The line height of the UI font is the height of its letters, without any room around them. */
    private static final float LINE_SPACING = 2.0F;
    private static final int BULLET_INDENT = 7;
    private static final float BULLET_SIZE = 2.0F;
    private static final int BACKDROP_COLOR = 0x8C000000;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int TIP_COLOR = 0xFFD0D0D0;
    private static final int NOTE_COLOR = 0xFFA0A0A0;

    private final Component title = Component.translatable(LOCALIZATION_KEY_BASE + "title");
    private final Component[] tips = new Component[TIP_COUNT];
    private final Component note = Component.translatable(LOCALIZATION_KEY_BASE + "note");
    private final List<List<MutableComponent>> wrappedTips = new ArrayList<>();
    private final List<MutableComponent> wrappedNote = new ArrayList<>();
    private Component @Nullable [] tooltipLines;
    private float wrappedForWidth = -1.0F;
    private float wrappedForTextSize = -1.0F;
    @Nullable
    private String wrappedForLanguageProbe;

    public ScrollAreaEmptyGuide() {
        for (int i = 0; i < TIP_COUNT; i++) {
            this.tips[i] = Component.translatable(LOCALIZATION_KEY_BASE + "tip_" + (i + 1));
        }
    }

    /**
     * Renders the guide into the given area of the screen.
     *
     * @param hovered If the mouse is over the area, which shows the guide as a tooltip when it does not fit.
     */
    public void render(@NotNull GuiGraphics graphics, int x, int y, int width, int height, boolean hovered) {

        float titleSize = UIBase.getUITextSizeNormal();
        float titleHeight = UIBase.getUITextHeight(titleSize);
        float titleWidth = UIBase.getUITextWidth(this.title, titleSize);
        float smallSize = UIBase.getUITextSizeSmall();
        float lineHeight = UIBase.getUITextHeight(smallSize) + LINE_SPACING;

        float textWidth = width - (PADDING * 2.0F);
        float headHeight = ICON_SIZE + TIP_GAP + titleHeight;

        boolean showsHead = (titleWidth <= textWidth) && ((headHeight + (PADDING * 2.0F)) <= height);
        if (!showsHead) {
            // Too small for words, so the icon alone says that things go in here
            float iconSize = Math.min(ICON_SIZE, Math.min(width, height) - 4.0F);
            if (iconSize >= 6.0F) {
                MaterialIcons.PLACE_ITEM.render(graphics, x + ((width - iconSize) / 2.0F), y + ((height - iconSize) / 2.0F), iconSize, iconSize, TITLE_COLOR);
            }
            this.showTooltip(hovered);
            RenderingUtils.resetShaderColor(graphics);
            return;
        }

        this.updateWrappedLines(textWidth - BULLET_INDENT, smallSize);
        float tipsHeight = 0.0F;
        for (List<MutableComponent> tip : this.wrappedTips) {
            tipsHeight += (tip.size() * lineHeight) + TIP_GAP;
        }
        float noteHeight = this.wrappedNote.size() * lineHeight;
        float fullHeight = headHeight + SECTION_GAP + tipsHeight + SECTION_GAP + noteHeight;
        boolean showsAll = (fullHeight + (PADDING * 2.0F)) <= height;

        float blockHeight = showsAll ? fullHeight : headHeight;
        float blockY = y + ((height - blockHeight) / 2.0F);
        float radius = UIBase.getInterfaceCornerRoundingRadius();
        // The guide has to be readable on whatever is behind the area, so it brings its own backdrop
        if (showsAll) {
            UIBase.renderRoundedRect(graphics, x + 2.0F, blockY - (PADDING - 2.0F), width - 4.0F, blockHeight + ((PADDING - 2.0F) * 2.0F), radius, radius, radius, radius, BACKDROP_COLOR);
        } else {
            float backdropWidth = Math.min(width - 4.0F, titleWidth + (PADDING * 2.0F));
            UIBase.renderRoundedRect(graphics, x + ((width - backdropWidth) / 2.0F), blockY - (PADDING - 2.0F), backdropWidth, blockHeight + ((PADDING - 2.0F) * 2.0F), radius, radius, radius, radius, BACKDROP_COLOR);
        }

        float currentY = blockY;
        MaterialIcons.PLACE_ITEM.render(graphics, x + ((width - ICON_SIZE) / 2.0F), currentY, ICON_SIZE, ICON_SIZE, TITLE_COLOR);
        currentY += ICON_SIZE + TIP_GAP;
        UIBase.renderText(graphics, this.title, x + ((width - titleWidth) / 2.0F), currentY, TITLE_COLOR, titleSize);
        currentY += titleHeight;

        if (!showsAll) {
            this.showTooltip(hovered);
            RenderingUtils.resetShaderColor(graphics);
            return;
        }

        currentY += SECTION_GAP;
        float textX = x + PADDING;
        for (List<MutableComponent> tip : this.wrappedTips) {
            float bulletY = currentY + ((lineHeight - BULLET_SIZE) / 2.0F);
            RenderingUtils.fillF(graphics, textX, bulletY, textX + BULLET_SIZE, bulletY + BULLET_SIZE, TIP_COLOR);
            for (MutableComponent line : tip) {
                UIBase.renderText(graphics, line, textX + BULLET_INDENT, currentY, TIP_COLOR, smallSize);
                currentY += lineHeight;
            }
            currentY += TIP_GAP;
        }
        currentY += SECTION_GAP - TIP_GAP;
        for (MutableComponent line : this.wrappedNote) {
            UIBase.renderText(graphics, line, textX, currentY, NOTE_COLOR, smallSize);
            currentY += lineHeight;
        }

        RenderingUtils.resetShaderColor(graphics);

    }

    /**
     * Wraps the tips and the note to the given width. Wrapping measures every word, so it only happens again when the width,
     * the text size or the language changed.
     */
    private void updateWrappedLines(float width, float textSize) {
        String languageProbe = this.title.getString();
        if ((width == this.wrappedForWidth) && (textSize == this.wrappedForTextSize) && languageProbe.equals(this.wrappedForLanguageProbe)) return;
        this.wrappedForWidth = width;
        this.wrappedForTextSize = textSize;
        this.wrappedForLanguageProbe = languageProbe;
        this.tooltipLines = null;
        this.wrappedTips.clear();
        for (Component tip : this.tips) {
            this.wrappedTips.add(UIBase.lineWrapUIComponentsSmall(tip, Math.max(20.0F, width)));
        }
        this.wrappedNote.clear();
        this.wrappedNote.addAll(UIBase.lineWrapUIComponentsSmall(this.note, Math.max(20.0F, width + BULLET_INDENT)));
    }

    private void showTooltip(boolean hovered) {
        if (!hovered) return;
        if (this.tooltipLines == null) {
            this.tooltipLines = new Component[TIP_COUNT + 4];
            this.tooltipLines[0] = this.title;
            this.tooltipLines[1] = Component.empty();
            for (int i = 0; i < TIP_COUNT; i++) {
                this.tooltipLines[i + 2] = Component.literal("• ").append(this.tips[i]);
            }
            this.tooltipLines[TIP_COUNT + 2] = Component.empty();
            this.tooltipLines[TIP_COUNT + 3] = this.note;
        }
        TooltipHandler.INSTANCE.addRenderTickTooltip(UITooltip.of(this.tooltipLines), () -> true);
    }

}
