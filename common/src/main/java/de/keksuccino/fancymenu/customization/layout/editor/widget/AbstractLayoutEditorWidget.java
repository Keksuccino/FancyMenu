package de.keksuccino.fancymenu.customization.layout.editor.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.GuiBlurRenderer;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.SmoothRectangleRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.rendering.ui.menubar.v2.MenuBar;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public abstract class AbstractLayoutEditorWidget extends AbstractContainerEventHandler implements Renderable, NarratableEntry {

    private static final Logger LOGGER = LogManager.getLogger();

    protected final AbstractLayoutEditorWidgetBuilder<?> builder;
    protected final LayoutEditorScreen editor;
    protected final Minecraft minecraft = Minecraft.getInstance();
    protected final List<GuiEventListener> children = new ArrayList<>();
    @NotNull
    protected Component displayLabel = Component.literal("Widget");
    private float unscaledWidgetOffsetX = 0;
    private float unscaledWidgetOffsetY = 0;
    private float bodyWidth = 100;
    private float bodyHeight = 100;
    protected SnappingSide snappingSide = SnappingSide.TOP_RIGHT;
    protected List<TitleBarButton> titleBarButtons = new ArrayList<>();
    protected boolean titleBarHovered = false;
    protected boolean expanded = true;
    protected ResizingEdge activeResizeEdge = null;
    protected ResizingEdge hoveredResizeEdge = null;
    protected boolean leftMouseDownTitleBar = false;
    protected double leftMouseDownMouseX = 0;
    protected double leftMouseDownMouseY = 0;
    protected float leftMouseDownWidgetOffsetX = 0;
    protected float leftMouseDownWidgetOffsetY = 0;
    protected float leftMouseDownInnerWidth = 0;
    protected float leftMouseDownInnerHeight = 0;
    protected ResourceSupplier<ITexture> hideButtonIconTextureSupplier = ResourceSupplier.image(ResourceSource.of("fancymenu:textures/layout_editor/widgets/hide_icon.png", ResourceSourceType.LOCATION).getSourceWithPrefix());
    protected ResourceSupplier<ITexture> expandButtonIconTextureSupplier = ResourceSupplier.image(ResourceSource.of("fancymenu:textures/layout_editor/widgets/expand_icon.png", ResourceSourceType.LOCATION).getSourceWithPrefix());
    protected ResourceSupplier<ITexture> collapseButtonIconTextureSupplier = ResourceSupplier.image(ResourceSource.of("fancymenu:textures/layout_editor/widgets/collapse_icon.png", ResourceSourceType.LOCATION).getSourceWithPrefix());
    protected boolean hovered = false;
    protected boolean visible = true;
    protected float posZ = 0.0F;
    private double lastMouseX = 0;
    private double lastMouseY = 0;
    private boolean hasMousePosition = false;

    public AbstractLayoutEditorWidget(@NotNull LayoutEditorScreen editor, @NotNull AbstractLayoutEditorWidgetBuilder<?> builder) {
        this.editor = Objects.requireNonNull(editor);
        this.builder = Objects.requireNonNull(builder);
        this.init();
    }

    protected void init() {

        this.children.clear();
        this.titleBarButtons.clear();

        this.addTitleBarButton(new TitleBarButton(this, consumes -> hideButtonIconTextureSupplier.get(), button -> this.setVisible(false)));

        this.addTitleBarButton(new TitleBarButton(this, consumes -> this.isExpanded() ? this.collapseButtonIconTextureSupplier.get() : this.expandButtonIconTextureSupplier.get(), button -> this.setExpanded(!this.isExpanded())));

    }

    public void refresh() {
        this.activeResizeEdge = null;
        this.hoveredResizeEdge = null;
        this.leftMouseDownTitleBar = false;
        this.setDragging(false);
    }

    @NotNull
    public AbstractLayoutEditorWidgetBuilder<?> getBuilder() {
        return this.builder;
    }

    @Override
    public @NotNull List<GuiEventListener> children() {
        return this.children;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.isVisible()) {
            return;
        }

        this.updateMousePosition(mouseX, mouseY);

        double renderScale = getRenderScaleSafe();
        double inputScale = getInputScaleSafe(renderScale);
        double uiMouseX = mouseX * inputScale;
        double uiMouseY = mouseY * inputScale;

        this.clampOffsetsToScreen();

        double localMouseX = uiMouseX - this.getTranslatedX();
        double localMouseY = uiMouseY - this.getTranslatedY();

        this.layoutTitleBarButtons();

        this.hovered = isPointInArea(localMouseX, localMouseY, 0.0F, 0.0F, this.getWidth(), this.getHeight());
        this.titleBarHovered = isPointInArea(localMouseX, localMouseY, 0.0F, 0.0F, this.getWidth(), this.getTitleBarHeight() + (this.getBorderThickness() * 2));
        this.hoveredResizeEdge = this.updateHoveredResizingEdge(localMouseX, localMouseY);

        this.updateCursor();

        RenderSystem.disableDepthTest();
        RenderingUtils.setDepthTestLocked(true);

        try {
            graphics.pose().pushPose();
            graphics.pose().scale((float) renderScale, (float) renderScale, 1.0F);
            graphics.pose().translate(this.getTranslatedX(), this.getTranslatedY(), this.posZ);

            if (this.isExpanded()) {
                this.renderBackground(graphics, partial);
                this.renderBodyViewport(graphics, localMouseX, localMouseY, renderScale, partial);
            }
            this.renderFrame(graphics, localMouseX, localMouseY, partial);

            graphics.pose().popPose();

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Error while rendering a layout editor widget!", ex);
        }

        RenderingUtils.setDepthTestLocked(false);

    }

    protected abstract void renderBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial);

    protected void renderBackground(@NotNull GuiGraphics graphics, float partial) {
        float x = this.getRealBodyX();
        float y = this.getBorderThickness() + this.getTitleBarHeight();
        float width = this.getBodyWidth();
        float height = this.getBodyHeight() + this.getBorderThickness();
        if (width <= 0 || height <= 0) {
            return;
        }
        float cornerRadius = UIBase.getInterfaceCornerRoundingRadius();
        if (UIBase.shouldBlur()) {
            GuiBlurRenderer.renderBlurAreaWithIntensityRoundAllCornersScaled(graphics, x, y, width, height, UIBase.getBlurRadius(), 0.0F, 0.0F, cornerRadius, cornerRadius, this.getBackgroundColor(), partial);
        } else {
            SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(graphics, x, y, width, height, 0.0F, 0.0F, cornerRadius, cornerRadius, this.getBackgroundColor().getColorInt(), partial);
        }
    }

    protected void renderFrame(@NotNull GuiGraphics graphics, double localMouseX, double localMouseY, float partial) {

        this.renderTitleBar(graphics, localMouseX, localMouseY, partial);

        //Separator between title bar and body (match PiPWindow logic)
        float titleBarHeight = this.getTitleBarHeight();
        if (this.isExpanded() && titleBarHeight > 0.0F) {
            double renderScale = getRenderScaleSafe();
            float divider = (float) Math.max(1.0F, renderScale);
            float dividerHeight = divider / (float) renderScale;
            UIBase.resetShaderColor(graphics);
            float titleBarX = this.getBorderThickness();
            float titleBarY = this.getBorderThickness();
            float titleBarRight = titleBarX + this.getBodyWidth();
            float bottom = titleBarY + titleBarHeight;
            RenderingUtils.fillF(graphics, titleBarX, bottom - dividerHeight, titleBarRight, bottom, this.getBorderColor().getColorInt());
        }

        //Widget border
        UIBase.resetShaderColor(graphics);
        float frameHeight = this.isExpanded() ? this.getHeight() : this.getBorderThickness() + this.getTitleBarHeight() + this.getBorderThickness();
        float roundingRadius = UIBase.getInterfaceCornerRoundingRadius();
        float smoothBorderThickness = this.getBorderThickness();
        float smoothBorderCorner = roundingRadius > 0.0F ? (roundingRadius + this.getBorderThickness()) : 0.0F;
        SmoothRectangleRenderer.renderSmoothBorderRoundAllCornersScaled(
                graphics,
                0.0F,
                0.0F,
                this.getWidth(),
                frameHeight,
                smoothBorderThickness,
                smoothBorderCorner,
                smoothBorderCorner,
                smoothBorderCorner,
                smoothBorderCorner,
                this.getBorderColor().getColorInt(),
                partial
        );

        UIBase.resetShaderColor(graphics);

    }

    protected void renderBodyViewport(@NotNull GuiGraphics graphics, double localMouseX, double localMouseY, double renderScale, float partial) {
        int scissorWidth = Math.max(0, Math.round((float) (this.getBodyWidth() * renderScale)));
        int scissorHeight = Math.max(0, Math.round((float) (this.getBodyHeight() * renderScale)));
        if (scissorWidth <= 0 || scissorHeight <= 0) {
            return;
        }
        int scissorX = Math.round((float) ((this.getTranslatedX() + this.getRealBodyX()) * renderScale));
        int scissorY = Math.round((float) ((this.getTranslatedY() + this.getRealBodyY()) * renderScale));
        double bodyMouseX = localMouseX - this.getRealBodyX();
        double bodyMouseY = localMouseY - this.getRealBodyY();
        if (!isPointInArea(localMouseX, localMouseY, this.getRealBodyX(), this.getRealBodyY(), this.getBodyWidth(), this.getBodyHeight())) {
            bodyMouseX = -100000;
            bodyMouseY = -100000;
        }
        graphics.enableScissor(scissorX, scissorY, scissorX + scissorWidth, scissorY + scissorHeight);
        graphics.pose().pushPose();
        graphics.pose().translate(this.getRealBodyX(), this.getRealBodyY(), 0.0F);
        try {
            LayoutEditorWidgetRenderContext.beginBodyRender(scissorX, scissorY, renderScale);
            this.renderBody(graphics, (int)Math.floor(bodyMouseX), (int)Math.floor(bodyMouseY), partial);
        } finally {
            LayoutEditorWidgetRenderContext.endBodyRender();
            graphics.pose().popPose();
            graphics.disableScissor();
        }
    }

    protected void renderTitleBar(@NotNull GuiGraphics graphics, double localMouseX, double localMouseY, float partial) {

        //Background
        UIBase.resetShaderColor(graphics);
        float innerX = this.getBorderThickness();
        float innerY = this.getBorderThickness();
        float innerWidth = this.getBodyWidth();
        float innerHeight = this.getTitleBarHeight();
        if (innerWidth > 0 && innerHeight > 0) {
            float cornerRadius = UIBase.getInterfaceCornerRoundingRadius();
            boolean expanded = this.isExpanded();
            float bottomLeft = expanded ? 0.0F : cornerRadius;
            float bottomRight = expanded ? 0.0F : cornerRadius;
            if (UIBase.shouldBlur()) {
                GuiBlurRenderer.renderBlurAreaWithIntensityRoundAllCornersScaled(graphics, innerX, innerY, innerWidth, innerHeight, UIBase.getBlurRadius(), cornerRadius, cornerRadius, bottomRight, bottomLeft, this.getTitleBarColor(), partial);
            } else {
                SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(
                        graphics,
                        innerX,
                        innerY,
                        innerWidth,
                        innerHeight,
                        cornerRadius,
                        cornerRadius,
                        bottomRight,
                        bottomLeft,
                        this.getTitleBarColor().getColorInt(),
                        partial
                );
            }
        }
        UIBase.resetShaderColor(graphics);

        //Buttons
        this.layoutTitleBarButtons();
        for (TitleBarButton b : this.titleBarButtons) {
            b.render(graphics, partial, localMouseX, localMouseY);
        }

        this.renderLabel(graphics, partial);

    }

    protected void renderLabel(@NotNull GuiGraphics graphics, float partial) {
        float titleBarX = this.getBorderThickness();
        float titleBarY = this.getBorderThickness();
        float labelDisplayWidth = Math.max(1, this.getBodyWidth() - this.getCombinedTitleBarButtonWidth() - 3);
        float scissorX = this.getBorderThickness() - 1;
        float scissorY = this.getBorderThickness() - 1;
        UIBase.resetShaderColor(graphics);
        RenderSystem.enableBlend();
        graphics.pose().pushPose();
        double renderScale = getRenderScaleSafe();
        int scissorScreenX = Math.round((float) ((this.getTranslatedX() + scissorX) * renderScale));
        int scissorScreenY = Math.round((float) ((this.getTranslatedY() + scissorY) * renderScale));
        int scissorScreenWidth = Math.round((float) (labelDisplayWidth * renderScale));
        int scissorScreenHeight = Math.round((float) ((this.getTitleBarHeight() + 2) * renderScale));
        graphics.enableScissor(scissorScreenX, scissorScreenY, scissorScreenX + scissorScreenWidth, scissorScreenY + scissorScreenHeight);
        UIBase.renderText(graphics, this.displayLabel, (int)(titleBarX + 3), (int)(titleBarY + (this.getTitleBarHeight() / 2f) - (UIBase.getUITextHeightNormal() / 2f)));
        graphics.disableScissor();
        graphics.pose().popPose();
        UIBase.resetShaderColor(graphics);
    }

    protected void addTitleBarButton(@NotNull AbstractLayoutEditorWidget.TitleBarButton button) {
        this.titleBarButtons.add(button);
    }

    protected void layoutTitleBarButtons() {
        float buttonX = this.getBorderThickness() + this.getBodyWidth();
        for (TitleBarButton b : this.titleBarButtons) {
            buttonX -= b.width;
            b.x = buttonX;
            b.y = this.getBorderThickness();
        }
    }

    @NotNull
    protected DrawableColor getElementHoverColor() {
        if (UIBase.shouldBlur()) return UIBase.getUITheme().ui_blur_interface_widget_background_color_hover_type_1;
        return UIBase.getUITheme().ui_interface_widget_background_color_hover_type_1;
    }

    @NotNull
    protected DrawableColor getTitleBarColor() {
        if (UIBase.shouldBlur()) return UIBase.getUITheme().ui_blur_interface_title_bar_tint;
        return UIBase.getUITheme().ui_interface_title_bar_color;
    }

    @NotNull
    protected DrawableColor getBorderColor() {
        if (UIBase.shouldBlur()) return UIBase.getUITheme().ui_blur_interface_border_color;
        return UIBase.getUITheme().ui_interface_border_color;
    }

    @NotNull
    protected DrawableColor getBackgroundColor() {
        if (UIBase.shouldBlur()) return UIBase.getUITheme().ui_blur_interface_background_tint;
        return UIBase.getUITheme().ui_interface_background_color;
    }

    protected void updateCursor() {
        if (this.hoveredResizeEdge == null) {
            return;
        }
        switch (this.hoveredResizeEdge) {
            case TOP, BOTTOM -> CursorHandler.setClientTickCursor(CursorHandler.CURSOR_RESIZE_VERTICAL);
            case LEFT, RIGHT -> CursorHandler.setClientTickCursor(CursorHandler.CURSOR_RESIZE_HORIZONTAL);
            case TOP_LEFT, BOTTOM_RIGHT -> CursorHandler.setClientTickCursor(CursorHandler.CURSOR_RESIZE_NWSE);
            case TOP_RIGHT, BOTTOM_LEFT -> CursorHandler.setClientTickCursor(CursorHandler.CURSOR_RESIZE_NESW);
            default -> {
            }
        }
    }

    @Nullable
    protected ResizingEdge updateHoveredResizingEdge(double localMouseX, double localMouseY) {
        if (!this.isVisible()) return null;
        if (!this.isExpanded()) return null;
        if (this.leftMouseDownTitleBar) return null;
        if (this.activeResizeEdge != null) return this.activeResizeEdge;
        //It's important to check this AFTER possibly returning the active edge
        if (this.isTitleBarButtonHovered(localMouseX, localMouseY)) return null;
        float hoverAreaThickness = 10.0f;
        float halfHoverAreaThickness = hoverAreaThickness / 2f;
        boolean left = isPointInArea(localMouseX, localMouseY, -halfHoverAreaThickness, 0.0F, hoverAreaThickness, this.getHeight());
        boolean right = isPointInArea(localMouseX, localMouseY, this.getWidth() - halfHoverAreaThickness, 0.0F, hoverAreaThickness, this.getHeight());
        boolean top = isPointInArea(localMouseX, localMouseY, 0.0F, -halfHoverAreaThickness, this.getWidth(), hoverAreaThickness);
        boolean bottom = isPointInArea(localMouseX, localMouseY, 0.0F, this.getHeight() - halfHoverAreaThickness, this.getWidth(), hoverAreaThickness);
        if (left && top) {
            return ResizingEdge.TOP_LEFT;
        }
        if (right && top) {
            return ResizingEdge.TOP_RIGHT;
        }
        if (left && bottom) {
            return ResizingEdge.BOTTOM_LEFT;
        }
        if (right && bottom) {
            return ResizingEdge.BOTTOM_RIGHT;
        }
        if (left) {
            return ResizingEdge.LEFT;
        }
        if (top) {
            return ResizingEdge.TOP;
        }
        if (right) {
            return ResizingEdge.RIGHT;
        }
        if (bottom) {
            return ResizingEdge.BOTTOM;
        }
        return null;
    }

    public void setUnscaledWidgetOffsetX(float offsetX, boolean forceSet) {
        if (!forceSet) {
            if ((offsetX > this.unscaledWidgetOffsetX) && (this.getTranslatedX() == this.getMaxTranslatedX())) return;
            if ((offsetX < this.unscaledWidgetOffsetX) && (this.getTranslatedX() == this.getMinTranslatedX())) return;
        }
        this.unscaledWidgetOffsetX = offsetX;
        if (!forceSet) {
            if (this.getTranslatedX() < this.getMinTranslatedX()) {
                float i = this.getMinTranslatedX() - this.getTranslatedX();
                this.unscaledWidgetOffsetX += i;
            }
            if (this.getTranslatedX() > this.getMaxTranslatedX()) {
                float i = this.getTranslatedX() - this.getMaxTranslatedX();
                this.unscaledWidgetOffsetX -= i;
            }
        }
    }

    public void setUnscaledWidgetOffsetY(float offsetY, boolean forceSet) {
        if (!forceSet) {
            if ((offsetY > this.unscaledWidgetOffsetY) && (this.getTranslatedY() == this.getMaxTranslatedY())) return;
            if ((offsetY < this.unscaledWidgetOffsetY) && (this.getTranslatedY()) == this.getMinTranslatedY()) return;
        }
        this.unscaledWidgetOffsetY = offsetY;
        if (!forceSet) {
            if (this.getTranslatedY() < this.getMinTranslatedY()) {
                float i = this.getMinTranslatedY() - this.getTranslatedY();
                this.unscaledWidgetOffsetY += i;
            }
            if (this.getTranslatedY() > this.getMaxTranslatedY()) {
                float i = this.getTranslatedY() - this.getMaxTranslatedY();
                this.unscaledWidgetOffsetY -= i;
            }
        }
    }

    public float getTranslatedX() {
        return this.getOriginX() + this.unscaledWidgetOffsetX;
    }

    public float getTranslatedY() {
        return this.getOriginY() + this.unscaledWidgetOffsetY;
    }

    public float getWidth() {
        return this.bodyWidth + (this.getBorderThickness() * 2);
    }

    public float getHeight() {
        if (!this.isExpanded()) return this.getBorderThickness() + this.getTitleBarHeight() + this.getBorderThickness();
        return this.getBorderThickness() + this.getTitleBarHeight() + this.getBorderThickness() + this.bodyHeight + this.getBorderThickness();
    }

    public float getCombinedTitleBarButtonWidth() {
        float i = 0;
        for (TitleBarButton b : this.titleBarButtons) {
            i += b.width;
        }
        return i;
    }

    public float getUnscaledWidgetOffsetX() {
        return this.unscaledWidgetOffsetX;
    }

    public float getUnscaledWidgetOffsetY() {
        return this.unscaledWidgetOffsetY;
    }

    protected float getOriginX() {
        if (this.snappingSide == SnappingSide.TOP_RIGHT) return this.getScreenWidth();
        return 0;
    }

    protected float getOriginY() {
        return this.getMinTranslatedY();
    }

    protected float getScreenEdgeBorderThickness() {
        return 10;
    }

    protected float getMinTranslatedX() {
        return this.getScreenEdgeBorderThickness();
    }

    protected float getMaxTranslatedX() {
        return this.getScreenWidth() - this.getScreenEdgeBorderThickness() - this.getWidth();
    }

    protected float getMinTranslatedY() {
        return MenuBar.PIXEL_SIZE + this.getScreenEdgeBorderThickness();
    }

    protected float getMaxTranslatedY() {
        return this.getScreenHeight() - this.getScreenEdgeBorderThickness() - this.getHeight();
    }

    protected float getScreenWidth() {
        double renderScale = getRenderScaleSafe();
        if (renderScale <= 0.0) {
            return this.minecraft.getWindow().getGuiScaledWidth();
        }
        return (float) (this.minecraft.getWindow().getGuiScaledWidth() / renderScale);
    }

    protected float getScreenHeight() {
        double renderScale = getRenderScaleSafe();
        if (renderScale <= 0.0) {
            return this.minecraft.getWindow().getGuiScaledHeight();
        }
        return (float) (this.minecraft.getWindow().getGuiScaledHeight() / renderScale);
    }

    public float getRealBodyX() {
        return this.getBorderThickness();
    }

    public float getRealBodyY() {
        return this.getBorderThickness() + this.getTitleBarHeight() + this.getBorderThickness();
    }

    public void setBodyWidth(float innerWidth) {
        this.bodyWidth = innerWidth;
    }

    public void setBodyHeight(float innerHeight) {
        this.bodyHeight = innerHeight;
    }

    public float getBodyWidth() {
        return this.bodyWidth;
    }

    public float getBodyHeight() {
        return this.bodyHeight;
    }

    public float getTitleBarHeight() {
        return 15;
    }

    public float getBorderThickness() {
        return 1;
    }

    public boolean isHovered() {
        if (!this.isVisible()) return false;
        if (!this.isExpanded()) return this.isTitleBarHovered();
        return this.hovered;
    }

    public boolean isTitleBarHovered() {
        if (!this.isVisible()) return false;
        return this.titleBarHovered;
    }

    public boolean isTitleBarButtonHovered() {
        if (!this.isVisible()) return false;
        for (TitleBarButton b : this.titleBarButtons) {
            if (b.isHovered()) return true;
        }
        return false;
    }

    public boolean isExpanded() {
        return this.expanded;
    }

    public AbstractLayoutEditorWidget setExpanded(boolean expanded) {
        this.expanded = expanded;
        return this;
    }

    @NotNull
    public Component getDisplayLabel() {
        return this.displayLabel;
    }

    public boolean isMouseOverTitleBar() {
        if (!this.isVisible()) return false;
        if (!this.hasMousePosition) return false;
        return this.isMouseOverTitleBar(this.lastMouseX, this.lastMouseY);
    }

    public boolean isMouseOverTitleBar(double mouseX, double mouseY) {
        if (!this.isVisible()) return false;
        double renderScale = getRenderScaleSafe();
        if (renderScale <= 0.0) renderScale = 1.0;
        double uiMouseX = mouseX / renderScale;
        double uiMouseY = mouseY / renderScale;
        double localMouseX = uiMouseX - this.getTranslatedX();
        double localMouseY = uiMouseY - this.getTranslatedY();
        return isPointInArea(localMouseX, localMouseY, 0.0F, 0.0F, this.getWidth(), this.getTitleBarHeight() + (this.getBorderThickness() * 2));
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!this.isVisible()) return false;
        double renderScale = getRenderScaleSafe();
        if (renderScale <= 0.0) renderScale = 1.0;
        double uiMouseX = mouseX / renderScale;
        double uiMouseY = mouseY / renderScale;
        double localMouseX = uiMouseX - this.getTranslatedX();
        double localMouseY = uiMouseY - this.getTranslatedY();
        return isPointInArea(localMouseX, localMouseY, 0.0F, 0.0F, this.getWidth(), this.getHeight());
    }

    public boolean isMouseOver() {
        if (!this.hasMousePosition) return false;
        return this.isMouseOver(this.lastMouseX, this.lastMouseY);
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void setFocused(boolean focused) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isVisible()) {
            return false;
        }

        this.updateMousePosition(mouseX, mouseY);
        this.clampOffsetsToScreen();

        double renderScale = getRenderScaleSafe();
        double inputScale = getInputScaleSafe(renderScale);
        double uiMouseX = mouseX * inputScale;
        double uiMouseY = mouseY * inputScale;
        double localMouseX = uiMouseX - this.getTranslatedX();
        double localMouseY = uiMouseY - this.getTranslatedY();
        double bodyMouseX = localMouseX - this.getRealBodyX();
        double bodyMouseY = localMouseY - this.getRealBodyY();

        this.layoutTitleBarButtons();

        if ((button == 0) && this.handleTitleBarButtonClick(localMouseX, localMouseY)) {
            return true;
        }

        this.activeResizeEdge = this.updateHoveredResizingEdge(localMouseX, localMouseY);
        if ((button == 0) && (this.activeResizeEdge == null) && isPointInArea(localMouseX, localMouseY, 0.0F, 0.0F, this.getWidth(), this.getTitleBarHeight() + (this.getBorderThickness() * 2)) && !this.isTitleBarButtonHovered(localMouseX, localMouseY)) {
            this.leftMouseDownTitleBar = true;
        }
        if ((this.activeResizeEdge != null) || this.leftMouseDownTitleBar) {
            this.leftMouseDownMouseX = uiMouseX;
            this.leftMouseDownMouseY = uiMouseY;
            this.leftMouseDownWidgetOffsetX = this.unscaledWidgetOffsetX;
            this.leftMouseDownWidgetOffsetY = this.unscaledWidgetOffsetY;
            this.leftMouseDownInnerWidth = this.bodyWidth;
            this.leftMouseDownInnerHeight = this.bodyHeight;
            return true;
        }

        if (this.isExpanded() && isPointInArea(localMouseX, localMouseY, this.getRealBodyX(), this.getRealBodyY(), this.getBodyWidth(), this.getBodyHeight())) {
            if (this.mouseClickedBody(bodyMouseX, bodyMouseY, button)) {
                return true;
            }
            return true;
        }

        return this.isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean wasWidgetDrag = this.leftMouseDownTitleBar || this.activeResizeEdge != null;
        this.leftMouseDownTitleBar = false;
        this.activeResizeEdge = null;
        if (!this.isExpanded()) {
            return wasWidgetDrag;
        }
        double renderScale = getRenderScaleSafe();
        double inputScale = getInputScaleSafe(renderScale);
        double uiMouseX = mouseX * inputScale;
        double uiMouseY = mouseY * inputScale;
        double localMouseX = uiMouseX - this.getTranslatedX();
        double localMouseY = uiMouseY - this.getTranslatedY();
        double bodyMouseX = localMouseX - this.getRealBodyX();
        double bodyMouseY = localMouseY - this.getRealBodyY();
        return wasWidgetDrag || this.mouseReleasedBody(bodyMouseX, bodyMouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double d1, double d2) {
        if (!this.isVisible()) {
            return false;
        }

        double renderScale = getRenderScaleSafe();
        double inputScale = getInputScaleSafe(renderScale);
        double uiMouseX = mouseX * inputScale;
        double uiMouseY = mouseY * inputScale;
        double uiDragX = d1 * inputScale;
        double uiDragY = d2 * inputScale;
        double localMouseX = uiMouseX - this.getTranslatedX();
        double localMouseY = uiMouseY - this.getTranslatedY();
        double bodyMouseX = localMouseX - this.getRealBodyX();
        double bodyMouseY = localMouseY - this.getRealBodyY();

        if ((button == 0) && this.activeResizeEdge != null) {
            double offsetX = (uiMouseX - this.leftMouseDownMouseX);
            double offsetY = (uiMouseY - this.leftMouseDownMouseY);
            this.handleResize((float) offsetX, (float) offsetY);
            return true;
        } else if ((button == 0) && this.leftMouseDownTitleBar) {
            double offsetX = (uiMouseX - this.leftMouseDownMouseX);
            double offsetY = (uiMouseY - this.leftMouseDownMouseY);
            this.setUnscaledWidgetOffsetX((float) (this.leftMouseDownWidgetOffsetX + offsetX), false);
            this.setUnscaledWidgetOffsetY((float) (this.leftMouseDownWidgetOffsetY + offsetY), false);
            return true;
        }

        if (this.isExpanded()) {
            return this.mouseDraggedBody(bodyMouseX, bodyMouseY, button, uiDragX, uiDragY);
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        if (!this.isVisible()) {
            return false;
        }
        if (!this.isExpanded()) {
            return false;
        }
        double renderScale = getRenderScaleSafe();
        double inputScale = getInputScaleSafe(renderScale);
        double uiMouseX = mouseX * inputScale;
        double uiMouseY = mouseY * inputScale;
        double localMouseX = uiMouseX - this.getTranslatedX();
        double localMouseY = uiMouseY - this.getTranslatedY();
        double bodyMouseX = localMouseX - this.getRealBodyX();
        double bodyMouseY = localMouseY - this.getRealBodyY();
        return this.mouseScrolledBody(bodyMouseX, bodyMouseY, scrollDeltaX, scrollDeltaY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.updateMousePosition(mouseX, mouseY);
        if (!this.isVisible()) {
            return;
        }
        if (!this.isExpanded()) {
            return;
        }
        double renderScale = getRenderScaleSafe();
        double inputScale = getInputScaleSafe(renderScale);
        double uiMouseX = mouseX * inputScale;
        double uiMouseY = mouseY * inputScale;
        double localMouseX = uiMouseX - this.getTranslatedX();
        double localMouseY = uiMouseY - this.getTranslatedY();
        double bodyMouseX = localMouseX - this.getRealBodyX();
        double bodyMouseY = localMouseY - this.getRealBodyY();
        this.mouseMovedBody(bodyMouseX, bodyMouseY);
    }

    @Override
    public boolean keyPressed(int keycode, int scancode, int modifiers) {
        for (GuiEventListener child : this.children()) {
            if (child.keyPressed(keycode, scancode, modifiers)) return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keycode, int scancode, int modifiers) {
        for (GuiEventListener child : this.children()) {
            if (child.keyReleased(keycode, scancode, modifiers)) return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        for (GuiEventListener child : this.children()) {
            if (child.charTyped(c, modifiers)) return true;
        }
        return false;
    }

    protected boolean mouseClickedBody(double mouseX, double mouseY, int button) {
        for (GuiEventListener child : this.children()) {
            if (child.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(child);
                if (button == 0) {
                    this.setDragging(true);
                }
                return true;
            }
        }
        return false;
    }

    protected boolean mouseReleasedBody(double mouseX, double mouseY, int button) {
        this.setDragging(false);
        for (GuiEventListener child : this.children()) {
            if (child.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    protected boolean mouseDraggedBody(double mouseX, double mouseY, int button, double d1, double d2) {
        if (this.isDragging() && (button == 0)) {
            for (GuiEventListener child : this.children()) {
                if (child.mouseDragged(mouseX, mouseY, button, d1, d2)) return true;
            }
        }
        return false;
    }

    protected boolean mouseScrolledBody(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        for (GuiEventListener child : this.children()) {
            if (child.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY)) return true;
        }
        return false;
    }

    protected void mouseMovedBody(double mouseX, double mouseY) {
    }

    protected void handleResize(float dragOffsetX, float dragOffsetY) {
        if (this.activeResizeEdge == null) {
            return;
        }
        if (this.activeResizeEdge.hasLeftEdge() || this.activeResizeEdge.hasRightEdge()) {
            float i = this.activeResizeEdge.hasLeftEdge() ? (this.leftMouseDownInnerWidth - dragOffsetX) : (this.leftMouseDownInnerWidth + dragOffsetX);
            if (i >= (this.getCombinedTitleBarButtonWidth() + 10)) {
                this.bodyWidth = i;
                if (this.activeResizeEdge.hasLeftEdge()) {
                    this.unscaledWidgetOffsetX = this.leftMouseDownWidgetOffsetX + dragOffsetX;
                }
            }
        }
        if (this.activeResizeEdge.hasTopEdge() || this.activeResizeEdge.hasBottomEdge()) {
            float i = this.activeResizeEdge.hasTopEdge() ? (this.leftMouseDownInnerHeight - dragOffsetY) : (this.leftMouseDownInnerHeight + dragOffsetY);
            if (i >= (this.getTitleBarHeight() + 10)) {
                this.bodyHeight = i;
                if (this.activeResizeEdge.hasTopEdge()) {
                    this.unscaledWidgetOffsetY = this.leftMouseDownWidgetOffsetY + dragOffsetY;
                }
            }
        }
    }

    @NotNull
    public List<AbstractLayoutEditorWidget> getAllWidgetsExceptThis() {
        List<AbstractLayoutEditorWidget> widgets = new ArrayList<>(this.editor.layoutEditorWidgets);
        widgets.removeIf(widget -> widget == this);
        return widgets;
    }

    public void editorElementOrderChanged(@NotNull AbstractEditorElement<?,?> element, boolean movedUp) {
    }

    public void editorElementAdded(@NotNull AbstractEditorElement<?,?> element) {
    }

    public void editorElementRemovedOrHidden(@NotNull AbstractEditorElement<?,?> element) {
    }

    public void tick() {
    }

    private boolean handleTitleBarButtonClick(double localMouseX, double localMouseY) {
        for (TitleBarButton button : this.titleBarButtons) {
            if (button.isMouseOver(localMouseX, localMouseY)) {
                if (FancyMenu.getOptions().playUiClickSounds.getValue()) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                button.clickAction.accept(button);
                return true;
            }
        }
        return false;
    }

    private void clampOffsetsToScreen() {
        if (this.getTranslatedX() < this.getMinTranslatedX()) this.setUnscaledWidgetOffsetX(this.unscaledWidgetOffsetX, false);
        if (this.getTranslatedX() > this.getMaxTranslatedX()) this.setUnscaledWidgetOffsetX(this.unscaledWidgetOffsetX, false);
        if (this.getTranslatedY() < this.getMinTranslatedY()) this.setUnscaledWidgetOffsetY(this.unscaledWidgetOffsetY, false);
        if (this.getTranslatedY() > this.getMaxTranslatedY()) this.setUnscaledWidgetOffsetY(this.unscaledWidgetOffsetY, false);
    }

    private void updateMousePosition(double mouseX, double mouseY) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        this.hasMousePosition = true;
    }

    private boolean isTitleBarButtonHovered(double localMouseX, double localMouseY) {
        for (TitleBarButton button : this.titleBarButtons) {
            if (button.isMouseOver(localMouseX, localMouseY)) {
                return true;
            }
        }
        return false;
    }

    private double getRenderScaleSafe() {
        double scale = UIBase.calculateFixedScale(UIBase.getUIScale());
        if (!Double.isFinite(scale) || scale <= 0.0) {
            return 1.0;
        }
        return scale;
    }

    private double getInputScaleSafe(double renderScale) {
        if (!Double.isFinite(renderScale) || renderScale <= 0.0) {
            return 1.0;
        }
        return 1.0 / renderScale;
    }

    private boolean isPointInArea(double mouseX, double mouseY, float areaX, float areaY, float areaWidth, float areaHeight) {
        return mouseX >= areaX && mouseX < areaX + areaWidth && mouseY >= areaY && mouseY < areaY + areaHeight;
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput var1) {
    }

    protected class TitleBarButton {

        protected AbstractLayoutEditorWidget parent;
        protected float x;
        protected float y;
        protected float width = 15;
        @NotNull
        protected Consumer<TitleBarButton> clickAction;
        @NotNull
        protected ConsumingSupplier<TitleBarButton, ITexture> iconSupplier;
        protected boolean hovered = false;

        protected TitleBarButton(AbstractLayoutEditorWidget parent, @NotNull ConsumingSupplier<TitleBarButton, ITexture> iconSupplier, @NotNull Consumer<TitleBarButton> clickAction) {
            this.parent = parent;
            this.iconSupplier = iconSupplier;
            this.clickAction = clickAction;
        }

        public void render(@NotNull GuiGraphics graphics, float partial, double localMouseX, double localMouseY) {

            this.hovered = this.isMouseOver(localMouseX, localMouseY);

            this.renderHoverBackground(graphics, partial);

            ITexture icon = this.iconSupplier.get(this);
            if (icon != null) {
                ResourceLocation location = icon.getResourceLocation();
                if (location != null) {
                    UIBase.getUITheme().setUITextureShaderColor(graphics, 1.0F);
                    RenderSystem.enableBlend();
                    RenderingUtils.blitF(graphics, location, this.x, this.y, 0.0F, 0.0F, (int) this.width, (int) this.parent.getTitleBarHeight(), (int) this.width, (int) this.parent.getTitleBarHeight());
                    UIBase.resetShaderColor(graphics);
                }
            }

        }

        protected void renderHoverBackground(GuiGraphics graphics, float partial) {
            if (this.isHovered()) {
                UIBase.resetShaderColor(graphics);
                float radius = UIBase.getInterfaceCornerRoundingRadius();
                float topLeft = 0.0F;
                float topRight = 0.0F;
                float bottomRight = 0.0F;
                float bottomLeft = 0.0F;
                float rightEdge = this.parent.getBorderThickness() + this.parent.getBodyWidth();
                boolean isRightmost = Math.abs((this.x + this.width) - rightEdge) <= 0.01F;
                if (isRightmost) {
                    topRight = radius;
                    if (!this.parent.isExpanded()) {
                        bottomRight = radius;
                    }
                }
                if (topLeft > 0.0F || topRight > 0.0F || bottomRight > 0.0F || bottomLeft > 0.0F) {
                    SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(
                            graphics,
                            this.x,
                            this.y,
                            this.width,
                            this.parent.getTitleBarHeight(),
                            topLeft,
                            topRight,
                            bottomRight,
                            bottomLeft,
                            getElementHoverColor().getColorInt(),
                            partial
                    );
                } else {
                    UIBase.fillF(graphics, this.x, this.y, this.x + this.width, this.y + this.parent.getTitleBarHeight(), getElementHoverColor().getColorInt());
                }
                UIBase.resetShaderColor(graphics);
            }
        }

        public boolean isHovered() {
            return this.hovered;
        }

        public boolean isMouseOver(double localMouseX, double localMouseY) {
            return isPointInArea(localMouseX, localMouseY, this.x, this.y, this.width, this.parent.getTitleBarHeight());
        }

    }

    public enum SnappingSide {

        TOP_LEFT("top-left"),
        TOP_RIGHT("top-right");

        public final String name;

        SnappingSide(String name) {
            this.name = name;
        }

        @Nullable
        public static SnappingSide getByName(@NotNull String name) {
            for (SnappingSide s : SnappingSide.values()) {
                if (s.name.equals(name)) return s;
            }
            return null;
        }

    }

    public enum ResizingEdge {
        LEFT(true, false, false, false),
        RIGHT(false, true, false, false),
        TOP(false, false, true, false),
        BOTTOM(false, false, false, true),
        TOP_LEFT(true, false, true, false),
        TOP_RIGHT(false, true, true, false),
        BOTTOM_LEFT(true, false, false, true),
        BOTTOM_RIGHT(false, true, false, true);

        private final boolean leftEdge;
        private final boolean rightEdge;
        private final boolean topEdge;
        private final boolean bottomEdge;

        ResizingEdge(boolean leftEdge, boolean rightEdge, boolean topEdge, boolean bottomEdge) {
            this.leftEdge = leftEdge;
            this.rightEdge = rightEdge;
            this.topEdge = topEdge;
            this.bottomEdge = bottomEdge;
        }

        public boolean hasLeftEdge() {
            return this.leftEdge;
        }

        public boolean hasRightEdge() {
            return this.rightEdge;
        }

        public boolean hasTopEdge() {
            return this.topEdge;
        }

        public boolean hasBottomEdge() {
            return this.bottomEdge;
        }
    }

}
