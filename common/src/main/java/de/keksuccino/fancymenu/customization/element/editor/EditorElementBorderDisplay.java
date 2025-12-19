package de.keksuccino.fancymenu.customization.element.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.Supplier;

public class EditorElementBorderDisplay implements Renderable {

    private static final int LINE_BACKGROUND_HORIZONTAL_PADDING_FANCYMENU = 2;
    private static final int LINE_BACKGROUND_VERTICAL_PADDING_FANCYMENU = 1;
    private static final int DISPLAY_OUTSIDE_PADDING_FANCYMENU = 2;
    private static final int DISPLAY_INSIDE_PADDING_FANCYMENU = 2;

    public final AbstractEditorElement editorElement;
    public Font font = Minecraft.getInstance().font;
    public final DisplayPosition defaultPosition;
    public final List<DisplayPosition> alternativePositions = new ArrayList<>();
    public DisplayPosition currentPosition;
    protected final Map<String, Supplier<Component>> lines = new LinkedHashMap<>();
    protected List<Component> renderLines = new ArrayList<>();
    protected int width = 0;
    protected int height = 0;
    protected boolean renderInsideFallback = false;

    public EditorElementBorderDisplay(@NotNull AbstractEditorElement editorElement, @NotNull DisplayPosition defaultPosition, @Nullable DisplayPosition... alternativePositions) {
        this.defaultPosition = defaultPosition;
        this.currentPosition = defaultPosition;
        this.editorElement = editorElement;
        if (alternativePositions != null) {
            this.alternativePositions.addAll(Arrays.asList(alternativePositions));
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.updateDisplay();

        this.renderDisplayLines(graphics);

    }

    protected void renderDisplayLines(GuiGraphics graphics) {

        int eleX = this.editorElement.getX();
        int eleY = this.editorElement.getY();
        int eleW = this.editorElement.getWidth();
        int eleH = this.editorElement.getHeight();
        int padding = this.renderInsideFallback ? DISPLAY_INSIDE_PADDING_FANCYMENU : DISPLAY_OUTSIDE_PADDING_FANCYMENU;
        int x = eleX + (this.renderInsideFallback ? padding : 0);
        int y = this.renderInsideFallback ? eleY + padding : eleY - this.getHeight() - padding;
        boolean leftAligned = true;
        if (this.currentPosition == DisplayPosition.TOP_RIGHT) {
            x = eleX + eleW - this.getWidth() - (this.renderInsideFallback ? padding : 0);
            leftAligned = false;
        }
        if (this.currentPosition == DisplayPosition.RIGHT_TOP) {
            x = this.renderInsideFallback ? eleX + eleW - this.getWidth() - padding : eleX + eleW + padding;
            y = this.renderInsideFallback ? eleY + padding : eleY;
        }
        if (this.currentPosition == DisplayPosition.RIGHT_BOTTOM) {
            x = this.renderInsideFallback ? eleX + eleW - this.getWidth() - padding : eleX + eleW + padding;
            y = this.renderInsideFallback ? eleY + eleH - this.getHeight() - padding : eleY + eleH - this.getHeight();
        }
        if (this.currentPosition == DisplayPosition.BOTTOM_RIGHT) {
            x = eleX + eleW - this.getWidth() - (this.renderInsideFallback ? padding : 0);
            y = this.renderInsideFallback ? eleY + eleH - this.getHeight() - padding : eleY + eleH + padding;
            leftAligned = false;
        }
        if (this.currentPosition == DisplayPosition.BOTTOM_LEFT) {
            y = this.renderInsideFallback ? eleY + eleH - this.getHeight() - padding : eleY + eleH + padding;
        }
        if (this.currentPosition == DisplayPosition.LEFT_BOTTOM) {
            x = this.renderInsideFallback ? eleX + padding : eleX - this.getWidth() - padding;
            y = this.renderInsideFallback ? eleY + eleH - this.getHeight() - padding : eleY + eleH - this.getHeight();
            leftAligned = false;
        }
        if (this.currentPosition == DisplayPosition.LEFT_TOP) {
            x = this.renderInsideFallback ? eleX + padding : eleX - this.getWidth() - padding;
            y = this.renderInsideFallback ? eleY + padding : eleY;
            leftAligned = false;
        }

        float scale = this.getScale();
        int lineY = y;
        int backgroundColor = UIBase.getUIColorTheme().layout_editor_element_border_display_line_background_color.getColorInt();
        int textColor = UIBase.getUIColorTheme().layout_editor_element_border_display_line_text_color.getColorInt();
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, scale);
        for (Component c : this.renderLines) {
            int lineWidth = this.font.width(c);
            int lineX = leftAligned ? x : x + (this.getWidth() - (int)((float)lineWidth * scale));
            int scaledLineX = (int)(lineX / scale);
            int scaledLineY = (int)(lineY / scale);
            int backgroundLeft = scaledLineX - LINE_BACKGROUND_HORIZONTAL_PADDING_FANCYMENU;
            int backgroundTop = scaledLineY - LINE_BACKGROUND_VERTICAL_PADDING_FANCYMENU;
            int backgroundRight = scaledLineX + lineWidth + LINE_BACKGROUND_HORIZONTAL_PADDING_FANCYMENU;
            int backgroundBottom = scaledLineY + this.font.lineHeight + LINE_BACKGROUND_VERTICAL_PADDING_FANCYMENU;
            graphics.fill(backgroundLeft, backgroundTop, backgroundRight, backgroundBottom, backgroundColor);
            graphics.drawString(this.font, c, scaledLineX, scaledLineY, textColor, false);
            lineY += (this.font.lineHeight + 2) * scale;
        }
        graphics.pose().popPose();

    }

    public void addLine(String identifier, Supplier<Component> lineSupplier) {
        this.lines.put(identifier, lineSupplier);
    }

    public void removeLine(String identifier) {
        this.lines.remove(identifier);
    }

    public void clearLines() {
        this.lines.clear();
    }

    public boolean hasLine(String identifier) {
        return this.lines.containsKey(identifier);
    }

    protected void updateDisplay() {
        this.width = 0;
        this.height = 0;
        this.renderLines.clear();
        for (Supplier<Component> s : this.lines.values()) {
            Component c = s.get();
            if (c != null) {
                int w = this.font.width(c);
                if (w > this.width) {
                    this.width = w;
                }
                this.height += this.font.lineHeight + 2;
                this.renderLines.add(c);
            }
        }
        this.height = (this.height > 0) ? this.height - 2 : 0;
        List<DisplayPosition> possiblePositions = this.getPossiblePositions();
        this.renderInsideFallback = possiblePositions.isEmpty();
        this.currentPosition = this.findPosition(possiblePositions);
    }

    protected float getScale() {
        return UIBase.getFixedUIScale();
    }

    public int getWidth() {
        return (int) ((float)this.width * this.getScale());
    }

    public int getHeight() {
        return (int) ((float)this.height * this.getScale());
    }

    @NotNull
    protected DisplayPosition findPosition() {
        return this.findPosition(this.getPossiblePositions());
    }

    @NotNull
    protected DisplayPosition findPosition(@NotNull List<DisplayPosition> possiblePositions) {
        List<DisplayPosition> allowedPositions = new ArrayList<>(this.alternativePositions);
        allowedPositions.add(0, this.defaultPosition);
        for (DisplayPosition p : allowedPositions) {
            if (possiblePositions.contains(p)) {
                return p;
            }
        }
        return this.defaultPosition;
    }

    protected List<DisplayPosition> getPossiblePositions() {
        List<DisplayPosition> positions = new ArrayList<>();
        int screenW = AbstractElement.getScreenWidth();
        int screenH = AbstractElement.getScreenHeight();
        int eleX = this.editorElement.getX();
        int eleY = this.editorElement.getY();
        int eleW = this.editorElement.getWidth();
        int eleH = this.editorElement.getHeight();
        if (eleX >= this.getWidth()) {
            positions.add(DisplayPosition.LEFT_TOP);
            positions.add(DisplayPosition.LEFT_BOTTOM);
        }
        if (eleY >= this.getHeight()) {
            positions.add(DisplayPosition.TOP_LEFT);
            positions.add(DisplayPosition.TOP_RIGHT);
        }
        if ((screenW - (eleX + eleW)) >= this.getWidth()) {
            positions.add(DisplayPosition.RIGHT_TOP);
            positions.add(DisplayPosition.RIGHT_BOTTOM);
        }
        if ((screenH - (eleY + eleH)) >= this.getHeight()) {
            positions.add(DisplayPosition.BOTTOM_LEFT);
            positions.add(DisplayPosition.BOTTOM_RIGHT);
        }
        return positions;
    }

    public enum DisplayPosition {
        TOP_LEFT,
        TOP_RIGHT,
        RIGHT_TOP,
        RIGHT_BOTTOM,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        LEFT_TOP,
        LEFT_BOTTOM
    }

}
