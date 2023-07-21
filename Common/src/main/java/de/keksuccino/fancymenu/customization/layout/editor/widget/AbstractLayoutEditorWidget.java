package de.keksuccino.fancymenu.customization.layout.editor.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.UIComponent;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resources.texture.WrappedTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
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

    protected static final ResourceLocation HIDE_BUTTON_ICON_TEXTURE = new ResourceLocation("fancymenu", "textures/layout_editor/widgets/hide_icon.png");
    protected static final ResourceLocation EXPAND_BUTTON_ICON_TEXTURE = new ResourceLocation("fancymenu", "textures/layout_editor/widgets/expand_icon.png");
    protected static final ResourceLocation COLLAPSE_BUTTON_ICON_TEXTURE = new ResourceLocation("fancymenu", "textures/layout_editor/widgets/collapse_icon.png");

    protected final AbstractLayoutEditorWidgetBuilder<?> builder;
    protected final LayoutEditorScreen editor;
    protected Component displayLabel = Component.literal("Widget");
    private float unscaledWidgetOffsetX = 0;
    private float unscaledWidgetOffsetY = 0;
    private float innerWidth = 100;
    private float innerHeight = 100;
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

    public AbstractLayoutEditorWidget(@NotNull LayoutEditorScreen editor, @NotNull AbstractLayoutEditorWidgetBuilder<?> builder) {
        this.editor = Objects.requireNonNull(editor);
        this.builder = Objects.requireNonNull(builder);
        this.init();
    }

    protected void init() {

        this.headerButtons.add(new HeaderButton(this, consumes -> WrappedTexture.of(HIDE_BUTTON_ICON_TEXTURE), button -> {
            this.setVisible(false);
        }));

        this.headerButtons.add(new HeaderButton(this, consumes -> WrappedTexture.of(this.isExpanded() ? COLLAPSE_BUTTON_ICON_TEXTURE : EXPAND_BUTTON_ICON_TEXTURE), button -> {
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
    public void renderComponent(@NotNull PoseStack pose, double mouseX, double mouseY, float partial) {

        float x = this.getRenderX();
        float y = this.getRenderY();

        //Fix offset on render tick, if needed
        if (this.getComponentX() < this.getMinComponentX()) this.setUnscaledWidgetOffsetX(this.unscaledWidgetOffsetX, false);
        if (this.getComponentX() > this.getMaxComponentX()) this.setUnscaledWidgetOffsetX(this.unscaledWidgetOffsetX, false);
        if (this.getComponentY() < this.getMinComponentY()) this.setUnscaledWidgetOffsetY(this.unscaledWidgetOffsetY, false);
        if (this.getComponentY() > this.getMaxComponentY()) this.setUnscaledWidgetOffsetY(this.unscaledWidgetOffsetY, false);

        this.hovered = this.isMouseOver();
        this.headerHovered = this.isMouseOverHeader();
        this.hoveredResizeEdge = this.updateHoveredResizingEdge();

        this.updateCursor();

        RenderingUtils.resetShaderColor();
        if (this.isExpanded()) {
            this.renderBody(pose, mouseX, mouseY, partial, x + this.getBorderThickness(), y +  this.getBorderThickness() + this.getHeaderHeight() + this.getBorderThickness(), this.getInnerWidth(), this.getInnerHeight());
        }
        this.renderFrame(pose, mouseX, mouseY, partial, x, y, this.getComponentWidth(), this.getComponentHeight());
        RenderingUtils.resetShaderColor();

    }

    protected abstract void renderBody(@NotNull PoseStack pose, double mouseX, double mouseY, float partial, float x, float y, float width, float height);

    protected void renderFrame(@NotNull PoseStack pose, double mouseX, double mouseY, float partial, float x, float y, float width, float height) {

        this.renderHeader(pose, mouseX, mouseY, partial, x, y, width, height);

        //Separator between header and body
        if (this.isExpanded()) {
            RenderingUtils.resetShaderColor();
            float separatorXMin = x + this.getBorderThickness();
            float separatorYMin =  y + this.getBorderThickness() + this.getHeaderHeight();
            float separatorXMax = separatorXMin + this.getInnerWidth();
            float separatorYMax = separatorYMin + this.getBorderThickness();
            fillF(pose, separatorXMin, separatorYMin, separatorXMax, separatorYMax, UIBase.getUIColorScheme().element_border_color_normal.getColorInt());
        }

        //Widget border
        RenderingUtils.resetShaderColor();
        if (this.isExpanded()) {
            UIBase.renderBorder(pose, x, y, x + width, y + height, this.getBorderThickness(), UIBase.getUIColorScheme().element_border_color_normal.getColorInt(), true, true, true, true);
        } else {
            UIBase.renderBorder(pose, x, y, x + width, y + this.getBorderThickness() + this.getHeaderHeight() + this.getBorderThickness(), this.getBorderThickness(), UIBase.getUIColorScheme().element_border_color_normal.getColorInt(), true, true, true, true);
        }

        RenderingUtils.resetShaderColor();

    }

    protected void renderHeader(@NotNull PoseStack pose, double mouseX, double mouseY, float partial, float x, float y, float width, float height) {

        //Background
        RenderingUtils.resetShaderColor();
        fillF(pose, x + this.getBorderThickness(), y + this.getBorderThickness(), x + this.getBorderThickness() + this.getInnerWidth(), y + this.getBorderThickness() + this.getHeaderHeight(), UIBase.getUIColorScheme().element_background_color_normal.getColorInt());
        RenderingUtils.resetShaderColor();

        //Buttons
        float buttonX = x + this.getBorderThickness() + this.getInnerWidth();
        for (HeaderButton b : this.headerButtons) {
            buttonX -= b.width;
            b.x = buttonX;
            b.y = y + this.getBorderThickness();
            b.render(pose, partial);
        }

        this.renderLabel(pose, mouseX, mouseY, partial, x, y, width, height);

    }

    protected void renderLabel(@NotNull PoseStack pose, double mouseX, double mouseY, float partial, float x, float y, float width, float height) {
        float headerX = x + this.getBorderThickness();
        float headerY = y + this.getBorderThickness();
        float labelDisplayWidth = Math.max(1, this.getInnerWidth() - this.getCombinedHeaderButtonWidth() - 3);
        float scissorX = x + this.getBorderThickness() - 1;
        float scissorY = y + this.getBorderThickness() - 1;
        RenderingUtils.resetShaderColor();
        RenderSystem.enableBlend();
        pose.pushPose();
        this.enableComponentScissor((int) scissorX, (int) scissorY, (int) labelDisplayWidth + 1, (int) this.getHeaderHeight() + 2, true);
        UIBase.drawElementLabelF(pose, Minecraft.getInstance().font, this.displayLabel, headerX + 3, headerY + (this.getHeaderHeight() / 2f) - (Minecraft.getInstance().font.lineHeight / 2f));
        this.disableComponentScissor();
        pose.popPose();
        RenderingUtils.resetShaderColor();
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
        if (this.isComponentAreaHovered(this.getComponentX() - halfHoverAreaThickness, this.getComponentY(), hoverAreaThickness, this.getComponentHeight(), false)) {
            return ResizingEdge.LEFT;
        }
        if (this.isComponentAreaHovered(this.getComponentX(), this.getComponentY() - halfHoverAreaThickness, this.getComponentWidth(), hoverAreaThickness, false)) {
            return ResizingEdge.TOP;
        }
        if (this.isComponentAreaHovered(this.getComponentX() + this.getComponentWidth() - halfHoverAreaThickness, this.getComponentY(), hoverAreaThickness, this.getComponentHeight(), false)) {
            return ResizingEdge.RIGHT;
        }
        if (this.isComponentAreaHovered(this.getComponentX(), this.getComponentY() + this.getComponentHeight() - halfHoverAreaThickness, this.getComponentWidth(), hoverAreaThickness, false)) {
            return ResizingEdge.BOTTOM;
        }
        return null;
    }

    public void setUnscaledWidgetOffsetX(float offsetX, boolean forceSet) {
        if (!forceSet) {
            if ((offsetX > this.unscaledWidgetOffsetX) && (this.getComponentX() == this.getMaxComponentX())) return;
            if ((offsetX < this.unscaledWidgetOffsetX) && (this.getComponentX() == this.getMinComponentX())) return;
        }
        this.unscaledWidgetOffsetX = offsetX;
        if (!forceSet) {
            if (this.getComponentX() < this.getMinComponentX()) {
                float i = this.getMinComponentX() - this.getComponentX();
                this.unscaledWidgetOffsetX += i;
            }
            if (this.getComponentX() > this.getMaxComponentX()) {
                float i = this.getComponentX() - this.getMaxComponentX();
                this.unscaledWidgetOffsetX -= i;
            }
        }
    }

    public void setUnscaledWidgetOffsetY(float offsetY, boolean forceSet) {
        if (!forceSet) {
            if ((offsetY > this.unscaledWidgetOffsetY) && (this.getComponentY() == this.getMaxComponentY())) return;
            if ((offsetY < this.unscaledWidgetOffsetY) && (this.getComponentY()) == this.getMinComponentY()) return;
        }
        this.unscaledWidgetOffsetY = offsetY;
        if (!forceSet) {
            if (this.getComponentY() < this.getMinComponentY()) {
                float i = this.getMinComponentY() - this.getComponentY();
                this.unscaledWidgetOffsetY += i;
            }
            if (this.getComponentY() > this.getMaxComponentY()) {
                float i = this.getComponentY() - this.getMaxComponentY();
                this.unscaledWidgetOffsetY -= i;
            }
        }
    }

    @Override
    public float getComponentX() {
        return this.getOriginX() + this.unscaledWidgetOffsetX;
    }

    @Override
    public float getComponentY() {
        return this.getOriginY() + this.unscaledWidgetOffsetY;
    }

    @Override
    public float getComponentWidth() {
        return this.innerWidth + (this.getBorderThickness() * 2);
    }

    @Override
    public float getComponentHeight() {
        if (!this.isExpanded()) return this.getBorderThickness() + this.getHeaderHeight() + this.getBorderThickness();
        return this.getBorderThickness() + this.getHeaderHeight() + this.getBorderThickness() + this.innerHeight + this.getBorderThickness();
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
        return this.getMinComponentY();
    }

    protected float getScreenEdgeBorderThickness() {
        return 10;
    }

    protected float getMinComponentX() {
        return this.getScreenEdgeBorderThickness();
    }

    protected float getMaxComponentX() {
        Minecraft.getInstance().getWindow();
        return this.getScreenWidth() - this.getScreenEdgeBorderThickness() - this.getComponentWidth();
    }

    protected float getMinComponentY() {
        return this.editor.menuBar.getHeight() + this.getScreenEdgeBorderThickness();
    }

    protected float getMaxComponentY() {
        return this.getScreenHeight() - this.getScreenEdgeBorderThickness() - this.getComponentHeight();
    }

    public void setInnerWidth(float innerWidth) {
        this.innerWidth = innerWidth;
    }

    public void setInnerHeight(float innerHeight) {
        this.innerHeight = innerHeight;
    }

    public float getInnerWidth() {
        return this.innerWidth;
    }

    public float getInnerHeight() {
        return this.innerHeight;
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

    public boolean isMouseOverHeader() {
        if (!this.isVisible()) return false;
        return this.isComponentAreaHovered(this.getComponentX(), this.getComponentY(), this.getComponentWidth(), this.getHeaderHeight() + (this.getBorderThickness() * 2), false);
    }

    @Override
    protected boolean mouseClickedComponent(double mouseX, double mouseY, int button) {
        if (this.isVisible()) {
            this.activeResizeEdge = this.hoveredResizeEdge;
            if ((this.activeResizeEdge == null) && this.isHeaderHovered() && !this.isHeaderButtonHovered()) {
                this.leftMouseDownHeader = true;
            }
            if ((this.activeResizeEdge != null) || this.leftMouseDownHeader) {
                this.leftMouseDownMouseX = mouseX;
                this.leftMouseDownMouseY = mouseY;
                this.leftMouseDownWidgetOffsetX = this.unscaledWidgetOffsetX;
                this.leftMouseDownWidgetOffsetY = this.unscaledWidgetOffsetY;
                this.leftMouseDownInnerWidth = this.innerWidth;
                this.leftMouseDownInnerHeight = this.innerHeight;
                return true;
            }
            for (HeaderButton b : this.headerButtons) {
                if (b.mouseClicked(mouseX, mouseY, button)) return true;
            }
        }
        return this.isVisible() && this.isMouseOver();
    }

    @Override
    protected boolean mouseReleasedComponent(double mouseX, double mouseY, int button) {
        this.leftMouseDownHeader = false;
        this.activeResizeEdge = null;
        return false;
    }

    @Override
    protected boolean mouseDraggedComponent(double mouseX, double mouseY, int button, double d1, double d2) {
        if (this.isVisible()) {
            double offsetX = (mouseX - this.leftMouseDownMouseX);
            double offsetY = (mouseY - this.leftMouseDownMouseY);
            if (this.activeResizeEdge != null) {
                this.handleResize((float) offsetX, (float) offsetY);
                return true;
            } else if (this.leftMouseDownHeader) {
                this.setUnscaledWidgetOffsetX((int)(this.leftMouseDownWidgetOffsetX + offsetX), false);
                this.setUnscaledWidgetOffsetY((int)(this.leftMouseDownWidgetOffsetY + offsetY), false);
                return true;
            }
        }
        return false;
    }

    protected void handleResize(float dragOffsetX, float dragOffsetY) {
        if ((this.activeResizeEdge == ResizingEdge.LEFT) || (this.activeResizeEdge == ResizingEdge.RIGHT)) {
            float i = (this.activeResizeEdge == ResizingEdge.LEFT) ? (this.leftMouseDownInnerWidth - dragOffsetX) : (this.leftMouseDownInnerWidth + dragOffsetX);
            if (i >= (this.getCombinedHeaderButtonWidth() + 10)) {
                this.innerWidth = i;
                this.unscaledWidgetOffsetX = this.leftMouseDownWidgetOffsetX + ((this.activeResizeEdge == ResizingEdge.LEFT) ? dragOffsetX : 0);
            }
        }
        if ((this.activeResizeEdge == ResizingEdge.TOP) || (this.activeResizeEdge == ResizingEdge.BOTTOM)) {
            float i = (this.activeResizeEdge == ResizingEdge.TOP) ? (this.leftMouseDownInnerHeight - dragOffsetY) : (this.leftMouseDownInnerHeight + dragOffsetY);
            if (i >= (this.getHeaderHeight() + 10)) {
                this.innerHeight = i;
                this.unscaledWidgetOffsetY = this.leftMouseDownWidgetOffsetY + ((this.activeResizeEdge == ResizingEdge.TOP) ? dragOffsetY : 0);
            }
        }
    }

    @Override
    protected boolean mouseScrolledComponent(double mouseX, double mouseY, double scrollDelta) {
        return false;
    }

    @Override
    protected void mouseMovedComponent(double mouseX, double mouseY) {
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

        public void render(@NotNull PoseStack pose, float partial) {

            this.hovered = this.isMouseOver();

            this.renderHoverBackground(pose);

            ITexture icon = this.iconSupplier.get(this);
            if ((icon != null) && (icon.getResourceLocation() != null)) {
                UIBase.getUIColorScheme().setUITextureShaderColor(1.0F);
                RenderSystem.enableBlend();
                RenderingUtils.bindTexture(icon.getResourceLocation());
                blitF(pose, this.x, this.y, 0.0F, 0.0F, (int) this.width, (int) this.parent.getHeaderHeight(), (int) this.width, (int) this.parent.getHeaderHeight());
                RenderingUtils.resetShaderColor();
            }

        }

        protected void renderHoverBackground(PoseStack pose) {
            if (this.isMouseOver()) {
                RenderingUtils.resetShaderColor();
                fillF(pose, this.x, this.y, this.x + this.width, this.y + this.parent.getHeaderHeight(), UIBase.getUIColorScheme().element_background_color_hover.getColorInt());
                RenderingUtils.resetShaderColor();
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
//            LOGGER.info("##################### HEADER BUTTON MOUSE OVER: mX: {} | mY: {} | x: {} | y: {} | width: {} | height: {}", this.parent.getComponentMouseX(), (this).parent.getComponentMouseY(), this.x, this.y, this.width, this.parent.getHeaderHeight());
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
