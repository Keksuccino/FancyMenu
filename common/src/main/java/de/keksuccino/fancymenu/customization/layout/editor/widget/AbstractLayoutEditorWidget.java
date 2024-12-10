package de.keksuccino.fancymenu.customization.layout.editor.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.UIComponent;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public abstract class AbstractLayoutEditorWidget extends UIComponent {

    private static final Logger LOGGER = LogManager.getLogger();

    protected final AbstractLayoutEditorWidgetBuilder<?> builder;
    protected final LayoutEditorScreen editor;
    @NotNull
    protected Component displayLabel = Component.literal("Widget");
    private float unscaledWidgetOffsetX = 0;
    private float unscaledWidgetOffsetY = 0;
    private float bodyWidth = 100;
    private float bodyHeight = 100;
    protected SnappingSide snappingSide = SnappingSide.TOP_RIGHT;
    protected List<HeaderButton> headerButtons = new ArrayList<>();
    protected boolean headerHovered = false;
    protected boolean expanded = true;
    protected ResizingEdge activeResizeEdge = null;
    protected ResizingEdge hoveredResizeEdge = null;
    protected boolean leftMouseDownHeader = false;
    protected double leftMouseDownMouseX = 0;
    protected double leftMouseDownMouseY = 0;
    protected float leftMouseDownWidgetOffsetX = 0;
    protected float leftMouseDownWidgetOffsetY = 0;
    protected float leftMouseDownInnerWidth = 0;
    protected float leftMouseDownInnerHeight = 0;
    protected ResourceSupplier<ITexture> hideButtonIconTextureSupplier = ResourceSupplier.image(ResourceSource.of("fancymenu:textures/layout_editor/widgets/hide_icon.png", ResourceSourceType.LOCATION).getSourceWithPrefix());
    protected ResourceSupplier<ITexture> expandButtonIconTextureSupplier = ResourceSupplier.image(ResourceSource.of("fancymenu:textures/layout_editor/widgets/expand_icon.png", ResourceSourceType.LOCATION).getSourceWithPrefix());
    protected ResourceSupplier<ITexture> collapseButtonIconTextureSupplier = ResourceSupplier.image(ResourceSource.of("fancymenu:textures/layout_editor/widgets/collapse_icon.png", ResourceSourceType.LOCATION).getSourceWithPrefix());

    public AbstractLayoutEditorWidget(@NotNull LayoutEditorScreen editor, @NotNull AbstractLayoutEditorWidgetBuilder<?> builder) {
        this.editor = Objects.requireNonNull(editor);
        this.builder = Objects.requireNonNull(builder);
        this.init();
    }

    protected void init() {

        this.children.clear();
        this.headerButtons.clear();

        this.addHeaderButton(new HeaderButton(this, consumes -> hideButtonIconTextureSupplier.get(), button -> {
            this.setVisible(false);
        }));

        this.addHeaderButton(new HeaderButton(this, consumes -> this.isExpanded() ? this.collapseButtonIconTextureSupplier.get() : this.expandButtonIconTextureSupplier.get(), button -> {
            this.setExpanded(!this.isExpanded());
        }));

    }

    public void refresh() {
        this.activeResizeEdge = null;
        this.hoveredResizeEdge = null;
        this.leftMouseDownHeader = false;
    }

    @NotNull
    public AbstractLayoutEditorWidgetBuilder<?> getBuilder() {
        return this.builder;
    }

    @Override
    public void renderComponent(@NotNull GuiGraphics graphics, double mouseX, double mouseY, float partial) {

        float x = this.getRealX();
        float y = this.getRealY();

        //Fix offset on render tick, if needed
        if (this.getTranslatedX() < this.getMinTranslatedX()) this.setUnscaledWidgetOffsetX(this.unscaledWidgetOffsetX, false);
        if (this.getTranslatedX() > this.getMaxTranslatedX()) this.setUnscaledWidgetOffsetX(this.unscaledWidgetOffsetX, false);
        if (this.getTranslatedY() < this.getMinTranslatedY()) this.setUnscaledWidgetOffsetY(this.unscaledWidgetOffsetY, false);
        if (this.getTranslatedY() > this.getMaxTranslatedY()) this.setUnscaledWidgetOffsetY(this.unscaledWidgetOffsetY, false);

        this.hovered = this.isMouseOver();
        this.headerHovered = this.isMouseOverHeader();
        this.hoveredResizeEdge = this.updateHoveredResizingEdge();

        this.updateCursor();

        if (this.isExpanded()) {
            this.renderBody(graphics, mouseX, mouseY, partial);
        }
        this.renderFrame(graphics, mouseX, mouseY, partial, x, y, this.getWidth(), this.getHeight());

    }

    protected abstract void renderBody(@NotNull GuiGraphics graphics, double mouseX, double mouseY, float partial);

    protected void renderFrame(@NotNull GuiGraphics graphics, double mouseX, double mouseY, float partial, float x, float y, float width, float height) {

        this.renderHeader(graphics, mouseX, mouseY, partial, x, y, width, height);

        //Separator between header and body
        if (this.isExpanded()) {
            float separatorXMin = x + this.getBorderThickness();
            float separatorYMin =  y + this.getBorderThickness() + this.getHeaderHeight();
            float separatorXMax = separatorXMin + this.getBodyWidth();
            float separatorYMax = separatorYMin + this.getBorderThickness();
            fillF(graphics, separatorXMin, separatorYMin, separatorXMax, separatorYMax, UIBase.getUIColorTheme().element_border_color_normal.getColorInt());
        }

        //Widget border
        if (this.isExpanded()) {
            UIBase.renderBorder(graphics, x, y, x + width, y + height, this.getBorderThickness(), UIBase.getUIColorTheme().element_border_color_normal.getColorInt(), true, true, true, true);
        } else {
            UIBase.renderBorder(graphics, x, y, x + width, y + this.getBorderThickness() + this.getHeaderHeight() + this.getBorderThickness(), this.getBorderThickness(), UIBase.getUIColorTheme().element_border_color_normal.getColorInt(), true, true, true, true);
        }

    }

    protected void renderHeader(@NotNull GuiGraphics graphics, double mouseX, double mouseY, float partial, float x, float y, float width, float height) {

        //Background
        fillF(graphics, x + this.getBorderThickness(), y + this.getBorderThickness(), x + this.getBorderThickness() + this.getBodyWidth(), y + this.getBorderThickness() + this.getHeaderHeight(), UIBase.getUIColorTheme().element_background_color_normal.getColorInt());

        //Buttons
        float buttonX = x + this.getBorderThickness() + this.getBodyWidth();
        for (HeaderButton b : this.headerButtons) {
            buttonX -= b.width;
            b.x = buttonX;
            b.y = y + this.getBorderThickness();
            b.render(graphics, partial);
        }

        this.renderLabel(graphics, mouseX, mouseY, partial, x, y, width, height);

    }

    protected void renderLabel(@NotNull GuiGraphics graphics, double mouseX, double mouseY, float partial, float x, float y, float width, float height) {
        float headerX = x + this.getBorderThickness();
        float headerY = y + this.getBorderThickness();
        float labelDisplayWidth = Math.max(1, this.getBodyWidth() - this.getCombinedHeaderButtonWidth() - 3);
        float scissorX = x + this.getBorderThickness() - 1;
        float scissorY = y + this.getBorderThickness() - 1;
        RenderSystem.enableBlend();
        graphics.pose().pushPose();
        this.enableComponentScissor(graphics, (int) scissorX, (int) scissorY, (int) labelDisplayWidth + 1, (int) this.getHeaderHeight() + 2, true);
        UIBase.drawElementLabel(graphics, Minecraft.getInstance().font, this.displayLabel, (int)(headerX + 3), (int)(headerY + (this.getHeaderHeight() / 2f) - (Minecraft.getInstance().font.lineHeight / 2f)));
        this.disableComponentScissor(graphics);
        graphics.pose().popPose();
    }

    protected void addHeaderButton(@NotNull HeaderButton button) {
        this.children.add(button);
        this.headerButtons.add(button);
    }

    protected void updateCursor() {
        if ((this.hoveredResizeEdge == ResizingEdge.TOP) || (this.hoveredResizeEdge == ResizingEdge.BOTTOM)) {
            CursorHandler.setClientTickCursor(CursorHandler.CURSOR_RESIZE_VERTICAL);
        } else if ((this.hoveredResizeEdge == ResizingEdge.LEFT) || (this.hoveredResizeEdge == ResizingEdge.RIGHT)) {
            CursorHandler.setClientTickCursor(CursorHandler.CURSOR_RESIZE_HORIZONTAL);
        }
    }

    @Nullable
    protected ResizingEdge updateHoveredResizingEdge() {
        if (!this.isVisible()) return null;
        if (!this.isExpanded()) return null;
        if (this.leftMouseDownHeader) return null;
        if (this.activeResizeEdge != null) return this.activeResizeEdge;
        //It's important to check this AFTER possibly returning the active edge
        if (this.isHeaderButtonHovered()) return null;
        float hoverAreaThickness = 10.0f;
        float halfHoverAreaThickness = hoverAreaThickness / 2f;
        if (this.isComponentAreaHovered(this.getTranslatedX() - halfHoverAreaThickness, this.getTranslatedY(), hoverAreaThickness, this.getHeight(), false)) {
            return ResizingEdge.LEFT;
        }
        if (this.isComponentAreaHovered(this.getTranslatedX(), this.getTranslatedY() - halfHoverAreaThickness, this.getWidth(), hoverAreaThickness, false)) {
            return ResizingEdge.TOP;
        }
        if (this.isComponentAreaHovered(this.getTranslatedX() + this.getWidth() - halfHoverAreaThickness, this.getTranslatedY(), hoverAreaThickness, this.getHeight(), false)) {
            return ResizingEdge.RIGHT;
        }
        if (this.isComponentAreaHovered(this.getTranslatedX(), this.getTranslatedY() + this.getHeight() - halfHoverAreaThickness, this.getWidth(), hoverAreaThickness, false)) {
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

    @Override
    public float getTranslatedX() {
        return this.getOriginX() + this.unscaledWidgetOffsetX;
    }

    @Override
    public float getTranslatedY() {
        return this.getOriginY() + this.unscaledWidgetOffsetY;
    }

    @Override
    public float getWidth() {
        return this.bodyWidth + (this.getBorderThickness() * 2);
    }

    @Override
    public float getHeight() {
        if (!this.isExpanded()) return this.getBorderThickness() + this.getHeaderHeight() + this.getBorderThickness();
        return this.getBorderThickness() + this.getHeaderHeight() + this.getBorderThickness() + this.bodyHeight + this.getBorderThickness();
    }

    public float getCombinedHeaderButtonWidth() {
        float i = 0;
        for (HeaderButton b : this.headerButtons) {
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
        return this.editor.menuBar.getHeight() + this.getScreenEdgeBorderThickness();
    }

    protected float getMaxTranslatedY() {
        return this.getScreenHeight() - this.getScreenEdgeBorderThickness() - this.getHeight();
    }

    public float getRealBodyX() {
        return this.getBorderThickness();
    }

    public float getRealBodyY() {
        return this.getBorderThickness() + this.getHeaderHeight() + this.getBorderThickness();
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

    public float getHeaderHeight() {
        return 15;
    }

    public float getBorderThickness() {
        return 1;
    }

    @Override
    public boolean isHovered() {
        if (!this.isVisible()) return false;
        if (!this.isExpanded()) return this.isHeaderHovered();
        return this.hovered;
    }

    public boolean isHeaderHovered() {
        if (!this.isVisible()) return false;
        return this.headerHovered;
    }

    public boolean isHeaderButtonHovered() {
        if (!this.isVisible()) return false;
        for (HeaderButton b : this.headerButtons) {
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

    public boolean isMouseOverHeader() {
        if (!this.isVisible()) return false;
        return this.isComponentAreaHovered(this.getTranslatedX(), this.getTranslatedY(), this.getWidth(), this.getHeaderHeight() + (this.getBorderThickness() * 2), false);
    }

    @Override
    protected boolean mouseClickedComponent(double realMouseX, double realMouseY, double translatedMouseX, double translatedMouseY, int button) {
        if (this.isVisible()) {
            this.activeResizeEdge = this.hoveredResizeEdge;
            if ((this.activeResizeEdge == null) && this.isHeaderHovered() && !this.isHeaderButtonHovered()) {
                this.leftMouseDownHeader = true;
            }
            if ((this.activeResizeEdge != null) || this.leftMouseDownHeader) {
                this.leftMouseDownMouseX = translatedMouseX;
                this.leftMouseDownMouseY = translatedMouseY;
                this.leftMouseDownWidgetOffsetX = this.unscaledWidgetOffsetX;
                this.leftMouseDownWidgetOffsetY = this.unscaledWidgetOffsetY;
                this.leftMouseDownInnerWidth = this.bodyWidth;
                this.leftMouseDownInnerHeight = this.bodyHeight;
                return true;
            }
            return super.mouseClickedComponent(realMouseX, realMouseY, translatedMouseX, translatedMouseY, button);
        }
        return false;
    }

    @Override
    protected boolean mouseReleasedComponent(double realMouseX, double realMouseY, double translatedMouseX, double translatedMouseY, int button) {
        this.leftMouseDownHeader = false;
        this.activeResizeEdge = null;
        return super.mouseReleasedComponent(realMouseX, realMouseY, translatedMouseX, translatedMouseY, button);
    }

    @Override
    protected boolean mouseDraggedComponent(double translatedMouseX, double translatedMouseY, int button, double d1, double d2) {
        if (this.isVisible()) {
            double offsetX = (translatedMouseX - this.leftMouseDownMouseX);
            double offsetY = (translatedMouseY - this.leftMouseDownMouseY);
            if (this.activeResizeEdge != null) {
                this.handleResize((float) offsetX, (float) offsetY);
                return true;
            } else if (this.leftMouseDownHeader) {
                this.setUnscaledWidgetOffsetX((int)(this.leftMouseDownWidgetOffsetX + offsetX), false);
                this.setUnscaledWidgetOffsetY((int)(this.leftMouseDownWidgetOffsetY + offsetY), false);
                return true;
            }
        }
        return super.mouseDraggedComponent(translatedMouseX, translatedMouseY, button, d1, d2);
    }

    protected void handleResize(float dragOffsetX, float dragOffsetY) {
        if ((this.activeResizeEdge == ResizingEdge.LEFT) || (this.activeResizeEdge == ResizingEdge.RIGHT)) {
            float i = (this.activeResizeEdge == ResizingEdge.LEFT) ? (this.leftMouseDownInnerWidth - dragOffsetX) : (this.leftMouseDownInnerWidth + dragOffsetX);
            if (i >= (this.getCombinedHeaderButtonWidth() + 10)) {
                this.bodyWidth = i;
                this.unscaledWidgetOffsetX = this.leftMouseDownWidgetOffsetX + ((this.activeResizeEdge == ResizingEdge.LEFT) ? dragOffsetX : 0);
            }
        }
        if ((this.activeResizeEdge == ResizingEdge.TOP) || (this.activeResizeEdge == ResizingEdge.BOTTOM)) {
            float i = (this.activeResizeEdge == ResizingEdge.TOP) ? (this.leftMouseDownInnerHeight - dragOffsetY) : (this.leftMouseDownInnerHeight + dragOffsetY);
            if (i >= (this.getHeaderHeight() + 10)) {
                this.bodyHeight = i;
                this.unscaledWidgetOffsetY = this.leftMouseDownWidgetOffsetY + ((this.activeResizeEdge == ResizingEdge.TOP) ? dragOffsetY : 0);
            }
        }
    }

    @NotNull
    public List<AbstractLayoutEditorWidget> getAllWidgetsExceptThis() {
        List<AbstractLayoutEditorWidget> widgets = new ArrayList<>(this.editor.layoutEditorWidgets);
        widgets.removeIf(widget -> widget == this);
        return widgets;
    }

    public void editorElementOrderChanged(@NotNull AbstractEditorElement element, boolean movedUp) {
    }

    public void editorElementAdded(@NotNull AbstractEditorElement element) {
    }

    public void editorElementRemovedOrHidden(@NotNull AbstractEditorElement element) {
    }

    public void tick() {
    }

    protected static class HeaderButton extends UIBase implements GuiEventListener {

        protected AbstractLayoutEditorWidget parent;
        protected float x;
        protected float y;
        protected float width = 15;
        @NotNull
        protected Consumer<HeaderButton> clickAction;
        @NotNull
        protected ConsumingSupplier<HeaderButton, ITexture> iconSupplier;
        protected boolean hovered = false;

        protected HeaderButton(AbstractLayoutEditorWidget parent, @NotNull ConsumingSupplier<HeaderButton, ITexture> iconSupplier, @NotNull Consumer<HeaderButton> clickAction) {
            this.parent = parent;
            this.iconSupplier = iconSupplier;
            this.clickAction = clickAction;
        }

        public void render(@NotNull GuiGraphics graphics, float partial) {

            this.hovered = this.isMouseOver();

            this.renderHoverBackground(graphics);

            ITexture icon = this.iconSupplier.get(this);
            if (icon != null) {
                ResourceLocation location = icon.getResourceLocation();
                if (location != null) {
                    RenderSystem.enableBlend();
                    blitF(graphics, RenderType::guiTextured, location, this.x, this.y, 0.0F, 0.0F, (int) this.width, (int) this.parent.getHeaderHeight(), (int) this.width, (int) this.parent.getHeaderHeight(), UIBase.getUIColorTheme().ui_texture_color.getColorInt());
                }
            }

        }

        protected void renderHoverBackground(GuiGraphics graphics) {
            if (this.isMouseOver()) {
                fillF(graphics, this.x, this.y, this.x + this.width, this.y + this.parent.getHeaderHeight(), UIBase.getUIColorTheme().element_background_color_hover.getColorInt());
            }
        }

        public boolean isHovered() {
            return this.hovered;
        }

        @Override
        public void setFocused(boolean var1) {
        }

        @Override
        public boolean isFocused() {
            return false;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if ((button == 0) && this.isMouseOver()) {
                if (FancyMenu.getOptions().playUiClickSounds.getValue()) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                this.clickAction.accept(this);
                return true;
            }
            return GuiEventListener.super.mouseClicked(mouseX, mouseY, button);
        }

        public boolean isMouseOver() {
            return this.parent.isComponentAreaHovered(this.x, this.y, this.width, this.parent.getHeaderHeight(), true);
        }

        @Deprecated
        @Override
        public boolean isMouseOver(double ignoredMouseX, double ignoredMouseY) {
            return this.isMouseOver();
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
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    }

}
