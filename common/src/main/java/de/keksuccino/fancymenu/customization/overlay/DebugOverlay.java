package de.keksuccino.fancymenu.customization.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.HideableElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

@SuppressWarnings("all")
public class DebugOverlay implements Renderable, NarratableEntry, ContainerEventHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    protected final List<DebugOverlayLine> lines = new ArrayList<>();
    @NotNull
    protected Supplier<Integer> topYOffsetSupplier = () -> 0;
    @NotNull
    protected Supplier<Integer> bottomYOffsetSupplier = () -> 0;
    @NotNull
    protected Font font = Minecraft.getInstance().font;
    protected int lineSpacerHeight = 2;
    protected int lineBorderWidth = 2;
    @NotNull
    protected DrawableColor lineBackgroundColor = DrawableColor.of(new Color(0, 0, 0, 230));
    @NotNull
    protected DrawableColor lineTextColor = DrawableColor.WHITE;
    protected List<AbstractElement> currentScreenElements = new ArrayList<>();
    @Nullable
    protected ContextMenu rightClickMenu = null;
    protected final List<GuiEventListener> children = new ArrayList<>();

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (Minecraft.getInstance().screen == null) return;

        RenderSystem.disableDepthTest();
        RenderingUtils.setDepthTestLocked(true);

        this.renderWidgetOverlays(graphics, Minecraft.getInstance().screen, mouseX, mouseY, partial);

        float uiScale = UIBase.getFixedUIRenderScale();
        int scaledMouseX = (int)((float)mouseX / uiScale);
        int scaledMouseY = (int)((float)mouseY / uiScale);

        int leftX = 0;
        int rightX = (int)((float)Minecraft.getInstance().screen.width / uiScale);

        int topLeftY = (int)((float)this.topYOffsetSupplier.get() / uiScale);
        int topRightY = (int)((float)this.topYOffsetSupplier.get() / uiScale);
        int bottomLeftY = (int)((float)this.bottomYOffsetSupplier.get() / uiScale);
        int bottomRightY = (int)((float)this.bottomYOffsetSupplier.get() / uiScale);

        RenderSystem.enableBlend();
        graphics.pose().pushPose();
        graphics.pose().scale(uiScale, uiScale, uiScale);

        for (DebugOverlayLine line : this.lines) {

            boolean isLeft = (line.linePosition == LinePosition.TOP_LEFT) || (line.linePosition == LinePosition.BOTTOM_LEFT);
            int width;
            int height;
            Component text = null;
            if (line instanceof DebugOverlayGraphLine graphLine) {
                width = graphLine.getRenderWidth(this.lineBorderWidth);
                height = graphLine.getRenderHeight(this.lineBorderWidth, this.lineSpacerHeight);
            } else {
                text = line.textSupplier.get(line);
                float textWidth = UIBase.getUITextWidthNormal(text);
                float textHeight = UIBase.getUITextHeightNormal();
                width = (int)Math.ceil(textWidth) + (this.lineBorderWidth * 2);
                height = (int)Math.ceil(textHeight) + (this.lineSpacerHeight * 2);
                if (line instanceof DebugOverlaySpacerLine s) height = s.height;
            }
            int x = isLeft ? leftX : rightX - width;
            int y = topLeftY;
            if (line.linePosition == LinePosition.TOP_RIGHT) y = topRightY;
            if (line.linePosition == LinePosition.BOTTOM_LEFT) y = bottomLeftY;
            if (line.linePosition == LinePosition.BOTTOM_RIGHT) y = bottomRightY;

            line.lastX = x;
            line.lastY = y;
            line.lastWidth = width;
            line.lastHeight = height;
            line.hovered = line.isMouseOver(scaledMouseX, scaledMouseY);

            if (line instanceof DebugOverlayGraphLine graphLine) {
                this.renderGraphLine(graphics, graphLine, x, y, width, height);
            } else if (!(line instanceof DebugOverlaySpacerLine)) {

                this.renderLineBackground(graphics, x, y, width, height);

                UIBase.renderText(graphics, text, x + this.lineBorderWidth, y + this.lineSpacerHeight, this.lineTextColor.getColorInt(), UIBase.getUITextSizeNormal());

            }

            //Update line Y positions
            if (line.linePosition == LinePosition.TOP_LEFT) topLeftY += height;
            if (line.linePosition == LinePosition.TOP_RIGHT) topRightY += height;
            if (line.linePosition == LinePosition.BOTTOM_LEFT) bottomLeftY -= height;
            if (line.linePosition == LinePosition.BOTTOM_RIGHT) bottomRightY -= height;

        }

        graphics.pose().popPose();

        RenderingUtils.resetShaderColor(graphics);

        //Close right-click context menu if context menu of overlay menu bar is open
        if ((CustomizationOverlay.getCurrentMenuBarInstance() != null) && CustomizationOverlay.getCurrentMenuBarInstance().isEntryContextMenuOpen()) {
            this.closeRightClickContextMenu();
        }
        //Render right-click context menu
        if (this.rightClickMenu != null) {
            RenderSystem.enableBlend();
            graphics.pose().pushPose();
            this.rightClickMenu.render(graphics, mouseX, mouseY, partial);
            graphics.pose().popPose();
        }

        RenderingUtils.resetShaderColor(graphics);

        RenderingUtils.setDepthTestLocked(false);

    }

    protected void renderWidgetOverlays(@NotNull GuiGraphics graphics, @NotNull Screen current, int mouseX, int mouseY, float partial) {

        ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(current);
        if (layer == null) return;

        this.currentScreenElements.clear();

        if (!ScreenCustomization.isCustomizationEnabledForScreen(current)) return;

        List<AbstractElement> widgets = new ArrayList<>(layer.vanillaWidgetElements);
        for (AbstractElement e : widgets) {
            if ((e instanceof HideableElement h) && h.isHidden()) continue;
            this.currentScreenElements.add(e);
            UIBase.renderBorder(graphics, e.getAbsoluteX(), e.getAbsoluteY(), e.getAbsoluteX() + e.getAbsoluteWidth(), e.getAbsoluteY() + e.getAbsoluteHeight(), 1, UIBase.getUITheme().layout_editor_element_border_color_normal, true, true, true, true);
        }

        RenderingUtils.resetShaderColor(graphics);

    }

    protected void renderLineBackground(@NotNull GuiGraphics graphics, int x, int y, int width, int height) {
        RenderSystem.enableBlend();
        graphics.fill(x, y, x + width, y + height, this.lineBackgroundColor.getColorInt());
        RenderingUtils.resetShaderColor(graphics);
    }

    protected void renderGraphLine(@NotNull GuiGraphics graphics, @NotNull DebugOverlayGraphLine graphLine, int x, int y, int width, int height) {
        RenderSystem.enableBlend();

        int backgroundColor = graphLine.getBackgroundColor(this.lineBackgroundColor.getColorInt());
        graphics.fill(x, y, x + width, y + height, backgroundColor);

        if (this.lineBorderWidth > 0) {
            int borderColor = graphLine.getBorderColor(this.lineTextColor.getColorInt());
            UIBase.renderBorder(graphics, x, y, x + width, y + height, this.lineBorderWidth, borderColor, true, true, true, true);
        }

        int innerX = x + this.lineBorderWidth;
        int innerY = y + this.lineBorderWidth + this.lineSpacerHeight;
        int innerWidth = graphLine.getGraphWidth();
        int innerHeight = graphLine.getGraphHeight();

        int innerBackground = graphLine.getInnerBackgroundColor(backgroundColor);
        graphics.fill(innerX, innerY, innerX + innerWidth, innerY + innerHeight, innerBackground);

        graphLine.pushSample();

        int gridColor = graphLine.getGridColor();
        for (int i = 1; i <= 3; i++) {
            float yLine = innerY + (innerHeight * (i / 4.0F));
            RenderingUtils.fillF(graphics, innerX, yLine, innerX + innerWidth, yLine + 1.0F, gridColor);
        }

        int sampleCount = graphLine.getSampleCount();
        if (sampleCount > 0) {
            int offset = Math.max(0, innerWidth - sampleCount);
            float lastX = 0.0F;
            float lastY = 0.0F;
            boolean hasLast = false;
            for (int i = 0; i < sampleCount; i++) {
                float sample = graphLine.getSample(i);
                float sampleHeight = sample * innerHeight;
                float sampleX = innerX + offset + i;
                float sampleY = innerY + innerHeight - sampleHeight;
                int fillColor = graphLine.getFillColor(sample);
                RenderingUtils.fillF(graphics, sampleX, sampleY, sampleX + 1.0F, innerY + innerHeight, fillColor);

                int lineColor = graphLine.getLineColor(sample);
                if (hasLast) {
                    RenderingUtils.fillF(graphics, lastX, lastY, sampleX + 1.0F, lastY + 1.0F, lineColor);
                    if (sampleY != lastY) {
                        float minY = Math.min(sampleY, lastY);
                        float maxY = Math.max(sampleY, lastY);
                        RenderingUtils.fillF(graphics, sampleX, minY, sampleX + 1.0F, maxY + 1.0F, lineColor);
                    }
                }
                lastX = sampleX;
                lastY = sampleY;
                hasLast = true;
            }
            float dotX = lastX - 1.0F;
            float dotY = lastY - 1.0F;
            float lastSample = graphLine.getSample(sampleCount - 1);
            RenderingUtils.fillF(graphics, dotX, dotY, dotX + 3.0F, dotY + 3.0F, graphLine.getHighlightColor(lastSample));
        }

        RenderingUtils.resetShaderColor(graphics);
    }

    public DebugOverlay setLineTextColor(@NotNull DrawableColor color) {
        this.lineTextColor = color;
        return this;
    }

    public DebugOverlay setLineBackgroundColor(@NotNull DrawableColor color) {
        this.lineBackgroundColor = color;
        return this;
    }

    public DebugOverlay setLineBorderWidth(int width) {
        this.lineBorderWidth = width;
        return this;
    }

    public DebugOverlay setLineSpacerHeight(int height) {
        this.lineSpacerHeight = height;
        return this;
    }

    public DebugOverlay setFont(@NotNull Font font) {
        this.font = font;
        return this;
    }

    public DebugOverlay setTopYOffsetSupplier(@NotNull Supplier<Integer> yOffsetSupplier) {
        this.topYOffsetSupplier = yOffsetSupplier;
        return this;
    }

    public DebugOverlay setBottomYOffsetSupplier(@NotNull Supplier<Integer> yOffsetSupplier) {
        this.bottomYOffsetSupplier = yOffsetSupplier;
        return this;
    }

    public DebugOverlaySpacerLine addSpacerLine(@NotNull String identifier, @NotNull LinePosition position, int height) {
        return (DebugOverlaySpacerLine) this.addLine(new DebugOverlaySpacerLine(identifier, height)).setPosition(position);
    }

    public DebugOverlaySpacerLine addSpacerLineBefore(@NotNull String addBeforeIdentifier, @NotNull String identifier, @NotNull LinePosition position, int height) {
        return (DebugOverlaySpacerLine) this.addLineBefore(addBeforeIdentifier, new DebugOverlaySpacerLine(identifier, height).setPosition(position));
    }

    public DebugOverlaySpacerLine addSpacerLineAfter(@NotNull String addAfterIdentifier, @NotNull String identifier, @NotNull LinePosition position, int height) {
        return (DebugOverlaySpacerLine) this.addLineAfter(addAfterIdentifier, new DebugOverlaySpacerLine(identifier, height).setPosition(position));
    }

    public DebugOverlaySpacerLine addSpacerLineAt(int index, @NotNull String identifier, @NotNull LinePosition position, int height) {
        return (DebugOverlaySpacerLine) this.addLineAt(index, new DebugOverlaySpacerLine(identifier, height).setPosition(position));
    }

    public DebugOverlayLine addLine(@NotNull String identifier, @NotNull LinePosition position, @NotNull ConsumingSupplier<DebugOverlayLine, Component> textSupplier) {
        return this.addLine(new DebugOverlayLine(identifier).setTextSupplier(textSupplier).setPosition(position));
    }

    public DebugOverlayGraphLine addGraphLine(@NotNull String identifier, @NotNull LinePosition position, @NotNull DoubleSupplier valueSupplier, double minValue, double maxValue) {
        return this.addGraphLine(identifier, position, valueSupplier, minValue, maxValue, DebugOverlayGraphLine.DEFAULT_GRAPH_WIDTH, DebugOverlayGraphLine.DEFAULT_GRAPH_HEIGHT);
    }

    public DebugOverlayGraphLine addGraphLine(@NotNull String identifier, @NotNull LinePosition position, @NotNull DoubleSupplier valueSupplier, double minValue, double maxValue, int graphWidth, int graphHeight) {
        DebugOverlayGraphLine line = new DebugOverlayGraphLine(identifier, graphWidth, graphHeight, valueSupplier, minValue, maxValue);
        line.setPosition(position);
        return this.addLine(line);
    }

    public DebugOverlayLine addLineBefore(@NotNull String addBeforeIdentifier, @NotNull String identifier, @NotNull LinePosition position, @NotNull ConsumingSupplier<DebugOverlayLine, Component> textSupplier) {
        return this.addLineBefore(addBeforeIdentifier, new DebugOverlayLine(identifier).setTextSupplier(textSupplier).setPosition(position));
    }

    public DebugOverlayLine addLineAfter(@NotNull String addAfterIdentifier, @NotNull String identifier, @NotNull LinePosition position, @NotNull ConsumingSupplier<DebugOverlayLine, Component> textSupplier) {
        return this.addLineAfter(addAfterIdentifier, new DebugOverlayLine(identifier).setTextSupplier(textSupplier).setPosition(position));
    }

    public DebugOverlayLine addLineAt(int index, @NotNull String identifier, @NotNull LinePosition position, @NotNull ConsumingSupplier<DebugOverlayLine, Component> textSupplier) {
        return this.addLineAt(index, new DebugOverlayLine(identifier).setTextSupplier(textSupplier).setPosition(position));
    }

    @SuppressWarnings("all")
    public <T extends DebugOverlayLine> T addLineBefore(@NotNull String addBeforeIdentifier, @NotNull T line) {
        if (this.lineExists(line.identifier)) {
            throw new RuntimeException("[FANCYMENU] Line identifier already exists: " + line.identifier);
        }
        DebugOverlayLine target = this.getLine(addBeforeIdentifier);
        int index = this.indexOfLine(addBeforeIdentifier);
        if (index == -1) index = this.lines.size();
        return (T) this.addLineAt(index, line.setPosition((target != null) ? target.linePosition : LinePosition.TOP_LEFT));
    }

    @SuppressWarnings("all")
    public <T extends DebugOverlayLine> T addLineAfter(@NotNull String addAfterIdentifier, @NotNull T line) {
        if (this.lineExists(line.identifier)) {
            throw new RuntimeException("[FANCYMENU] Line identifier already exists: " + line.identifier);
        }
        DebugOverlayLine target = this.getLine(addAfterIdentifier);
        int index = this.indexOfLine(addAfterIdentifier)+1;
        if (index == 0) index = this.lines.size();
        return (T) this.addLineAt(index, line.setPosition((target != null) ? target.linePosition : LinePosition.TOP_LEFT));
    }

    public <T extends DebugOverlayLine> T addLineAt(int index, @NotNull T line) {
        if (this.lineExists(line.identifier)) {
            throw new RuntimeException("[FANCYMENU] Line identifier already exists: " + line.identifier);
        }
        this.lines.add(Math.min(this.lines.size(), Math.max(0, index)), line);
        return line;
    }

    public <T extends DebugOverlayLine> T addLine(@NotNull T line) {
        if (this.lineExists(line.identifier)) {
            throw new RuntimeException("[FANCYMENU] Line identifier already exists: " + line.identifier);
        }
        this.lines.add(line);
        return line;
    }

    public void removeLine(@NotNull String identifier) {
        DebugOverlayLine line = this.getLine(identifier);
        if (line != null) this.lines.remove(line);
    }

    @Nullable
    public DebugOverlayLine getLine(@NotNull String identifier) {
        for (DebugOverlayLine line : this.lines) {
            if (line.identifier.equals(identifier)) return line;
        }
        return null;
    }

    /**
     * Returns the index of the given line or -1 if the line was not found in the line list.
     */
    public int indexOfLine(@NotNull String identifier) {
        DebugOverlayLine line = this.getLine(identifier);
        if (line != null) return this.lines.indexOf(line);
        return -1;
    }

    public boolean lineExists(@NotNull String identifier) {
        return this.getLine(identifier) != null;
    }

    public DebugOverlay resetOverlay() {
        this.lines.forEach(line -> line.hovered = false);
        this.closeRightClickContextMenu();
        return this;
    }

    public DebugOverlay openRightClickContextMenu(@NotNull ContextMenu menu) {
        this.closeRightClickContextMenu();
        this.rightClickMenu = Objects.requireNonNull(menu);
        this.children.add(0, this.rightClickMenu);
        this.rightClickMenu.openMenuAtMouse();
        return this;
    }

    public DebugOverlay closeRightClickContextMenu() {
        if (this.rightClickMenu != null) {
            this.rightClickMenu.closeMenu();
            this.children.remove(this.rightClickMenu);
            this.rightClickMenu = null;
        }
        return this;
    }

    @NotNull
    protected ContextMenu buildContextMenuForElement(@NotNull AbstractElement element) {

        Objects.requireNonNull(element);

        ContextMenu menu = new ContextMenu();

        if (element instanceof VanillaWidgetElement v) {
            menu.addClickableEntry("copy_vanilla_widget_locator", Component.translatable("fancymenu.elements.vanilla_button.copy_locator"), (menu1, entry) -> {
                        if (v.widgetMeta != null) {
                            Minecraft.getInstance().keyboardHandler.setClipboard(v.widgetMeta.getLocator());
                        }
                        MainThreadTaskExecutor.executeInMainThread(() -> this.closeRightClickContextMenu(), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                    })
                    .setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.vanilla_button.copy_locator.desc")))
                    .setIcon(MaterialIcons.CONTENT_COPY);
        }

        menu.addClickableEntry("copy_id", Component.translatable("fancymenu.elements.copyid"), (menu1, entry) -> {
                    Minecraft.getInstance().keyboardHandler.setClipboard(element.getInstanceIdentifier());
                    MainThreadTaskExecutor.executeInMainThread(() -> this.closeRightClickContextMenu(), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                }).setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.copyid.desc")))
                .setIcon(MaterialIcons.CONTENT_COPY);

        return menu;

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (ContainerEventHandler.super.mouseClicked(mouseX, mouseY, button)) return true;
        this.closeRightClickContextMenu();
        float uiScale = UIBase.getFixedUIRenderScale();
        int scaledMouseX = (int)(mouseX / uiScale);
        int scaledMouseY = (int)(mouseY / uiScale);
        for (DebugOverlayLine line : this.lines) {
            if (line.onClick(button, scaledMouseX, scaledMouseY)) return true;
        }
        if (button == 1) {
            for (AbstractElement e : this.currentScreenElements) {
                if (RenderingUtils.isXYInArea(mouseX, mouseY, e.getAbsoluteX(), e.getAbsoluteY(), e.getAbsoluteWidth(), e.getAbsoluteHeight())) {
                    this.openRightClickContextMenu(this.buildContextMenuForElement(e));
                    return true;
                }
            }
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

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput var1) {
    }

    @Override
    public List<GuiEventListener> children() {
        return this.children;
    }

    @Override
    public boolean isDragging() {
        return false;
    }

    @Override
    public void setDragging(boolean var1) {
    }

    @Nullable
    @Override
    public GuiEventListener getFocused() {
        return null;
    }

    @Override
    public void setFocused(@Nullable GuiEventListener var1) {
    }

    public static class DebugOverlaySpacerLine extends DebugOverlayLine {

        protected final  int height;

        protected DebugOverlaySpacerLine(@NotNull String identifier, int height) {
            super(identifier);
            this.height = height;
        }

    }

    public static class DebugOverlayLine {

        protected final String identifier;
        @NotNull
        protected LinePosition linePosition = LinePosition.TOP_LEFT;
        @NotNull
        protected ConsumingSupplier<DebugOverlayLine, Component> textSupplier = consumes -> Component.empty();
        protected boolean clickable = false;
        @NotNull
        protected Consumer<DebugOverlayLine> clickAction = line -> {};
        protected long lastClicked = -1;
        protected int lastX;
        protected int lastY;
        protected int lastWidth;
        protected int lastHeight;
        protected boolean hovered = false;

        protected DebugOverlayLine(@NotNull String identifier) {
            this.identifier = Objects.requireNonNull(identifier);
        }

        @NotNull
        public String getIdentifier() {
            return this.identifier;
        }

        public DebugOverlayLine setPosition(@NotNull LinePosition position) {
            this.linePosition = position;
            return this;
        }

        @NotNull
        public LinePosition getPosition() {
            return this.linePosition;
        }

        public DebugOverlayLine setTextSupplier(@NotNull ConsumingSupplier<DebugOverlayLine, Component> textSupplier) {
            this.textSupplier = textSupplier;
            return this;
        }

        @NotNull
        public ConsumingSupplier<DebugOverlayLine, Component> getTextSupplier() {
            return this.textSupplier;
        }

        public DebugOverlayLine setClickAction(@Nullable Consumer<DebugOverlayLine> clickAction) {
            this.clickable = clickAction != null;
            this.clickAction = (clickAction != null) ? clickAction : line -> {};
            return this;
        }

        public boolean recentlyClicked() {
            return System.currentTimeMillis() < (this.lastClicked + 2000);
        }

        public boolean isHovered() {
            return this.hovered;
        }

        protected boolean onClick(int button, int mouseX, int mouseY) {
            if (!this.clickable) return false;
            if (this.isMouseOver(mouseX, mouseY) && (button == 0)) {
                this.clickAction.accept(this);
                this.lastClicked = System.currentTimeMillis();
                return true;
            }
            return false;
        }

        protected boolean isMouseOver(int mouseX, int mouseY) {
            return RenderingUtils.isXYInArea(mouseX, mouseY, this.lastX, this.lastY, this.lastWidth, this.lastHeight);
        }

    }

    public static class DebugOverlayGraphLine extends DebugOverlayLine {

        public static final int DEFAULT_GRAPH_WIDTH = 120;
        public static final int DEFAULT_GRAPH_HEIGHT = 26;

        private static final int GRADIENT_LOW = 0xFF4CAF50;
        private static final int GRADIENT_MID = 0xFFFFC107;
        private static final int GRADIENT_HIGH = 0xFFF44336;

        private final DoubleSupplier valueSupplier;
        private final double minValue;
        private final double maxValue;
        private final int graphWidth;
        private final int graphHeight;
        private final float[] samples;
        private int sampleIndex = 0;
        private int sampleCount = 0;
        private int sampleIntervalMs = 150;
        private long lastSampleTime = 0L;
        private boolean invertColorGradient = false;
        @Nullable
        private Integer backgroundColorOverride = null;
        @Nullable
        private Integer borderColorOverride = null;
        private int gridColor = RenderingUtils.replaceAlphaInColor(0xFFFFFFFF, 40);
        @Nullable
        private Integer highlightColorOverride = null;

        protected DebugOverlayGraphLine(@NotNull String identifier, int graphWidth, int graphHeight, @NotNull DoubleSupplier valueSupplier, double minValue, double maxValue) {
            super(identifier);
            this.valueSupplier = Objects.requireNonNull(valueSupplier);
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.graphWidth = Math.max(2, graphWidth);
            this.graphHeight = Math.max(2, graphHeight);
            this.samples = new float[this.graphWidth];
        }

        public DebugOverlayGraphLine setSampleIntervalMs(int sampleIntervalMs) {
            this.sampleIntervalMs = Math.max(10, sampleIntervalMs);
            return this;
        }

        public DebugOverlayGraphLine setInvertColorGradient(boolean invert) {
            this.invertColorGradient = invert;
            return this;
        }

        public DebugOverlayGraphLine setBackgroundColor(@Nullable Integer backgroundColor) {
            this.backgroundColorOverride = backgroundColor;
            return this;
        }

        public DebugOverlayGraphLine setBorderColor(@Nullable Integer borderColor) {
            this.borderColorOverride = borderColor;
            return this;
        }

        public DebugOverlayGraphLine setGridColor(int gridColor) {
            this.gridColor = gridColor;
            return this;
        }

        public DebugOverlayGraphLine setHighlightColor(int highlightColor) {
            this.highlightColorOverride = highlightColor;
            return this;
        }

        protected int getGraphWidth() {
            return this.graphWidth;
        }

        protected int getGraphHeight() {
            return this.graphHeight;
        }

        protected int getRenderWidth(int borderWidth) {
            return this.graphWidth + (borderWidth * 2);
        }

        protected int getRenderHeight(int borderWidth, int padding) {
            return this.graphHeight + (borderWidth * 2) + (padding * 2);
        }

        protected int getSampleCount() {
            return this.sampleCount;
        }

        protected float getSample(int index) {
            int start = this.sampleIndex - this.sampleCount;
            if (start < 0) start += this.samples.length;
            int idx = (start + index) % this.samples.length;
            return this.samples[idx];
        }

        protected int getBackgroundColor(int fallbackColor) {
            return this.backgroundColorOverride != null ? this.backgroundColorOverride : fallbackColor;
        }

        protected int getBorderColor(int fallbackColor) {
            return this.borderColorOverride != null ? this.borderColorOverride : RenderingUtils.replaceAlphaInColor(fallbackColor, 120);
        }

        protected int getInnerBackgroundColor(int fallbackColor) {
            return RenderingUtils.replaceAlphaInColor(fallbackColor, 180);
        }

        protected int getGridColor() {
            return this.gridColor;
        }

        protected int getFillColor(float normalized) {
            return RenderingUtils.replaceAlphaInColor(getLineColor(normalized), 90);
        }

        protected int getLineColor(float normalized) {
            float value = this.invertColorGradient ? 1.0F - normalized : normalized;
            value = Math.min(1.0F, Math.max(0.0F, value));
            if (value <= 0.5F) {
                return lerpColor(GRADIENT_LOW, GRADIENT_MID, value / 0.5F);
            }
            return lerpColor(GRADIENT_MID, GRADIENT_HIGH, (value - 0.5F) / 0.5F);
        }

        protected int getHighlightColor(float normalized) {
            return this.highlightColorOverride != null
                    ? this.highlightColorOverride
                    : RenderingUtils.replaceAlphaInColor(getLineColor(normalized), 220);
        }

        protected void pushSample() {
            long now = System.currentTimeMillis();
            if ((now - this.lastSampleTime) < this.sampleIntervalMs) return;
            this.lastSampleTime = now;
            double value = this.valueSupplier.getAsDouble();
            double range = this.maxValue - this.minValue;
            float normalized;
            if (range <= 0.0D) {
                normalized = 0.0F;
            } else {
                normalized = (float)((value - this.minValue) / range);
            }
            normalized = Math.min(1.0F, Math.max(0.0F, normalized));
            this.samples[this.sampleIndex] = normalized;
            this.sampleIndex = (this.sampleIndex + 1) % this.samples.length;
            if (this.sampleCount < this.samples.length) this.sampleCount++;
        }

        private static int lerpColor(int colorA, int colorB, float t) {
            int aA = (colorA >> 24) & 0xFF;
            int rA = (colorA >> 16) & 0xFF;
            int gA = (colorA >> 8) & 0xFF;
            int bA = colorA & 0xFF;

            int aB = (colorB >> 24) & 0xFF;
            int rB = (colorB >> 16) & 0xFF;
            int gB = (colorB >> 8) & 0xFF;
            int bB = colorB & 0xFF;

            int a = (int)(aA + (aB - aA) * t);
            int r = (int)(rA + (rB - rA) * t);
            int g = (int)(gA + (gB - gA) * t);
            int b = (int)(bA + (bB - bA) * t);

            return (a << 24) | (r << 16) | (g << 8) | b;
        }

    }

    public enum LinePosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

}
