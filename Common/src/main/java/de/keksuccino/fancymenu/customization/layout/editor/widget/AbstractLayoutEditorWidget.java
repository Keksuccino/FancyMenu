package de.keksuccino.fancymenu.customization.layout.editor.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resources.texture.WrappedTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * {@link AbstractLayoutEditorWidget}s need a constructor that takes only one parameter of type {@link LayoutEditorScreen}.
 */
public abstract class AbstractLayoutEditorWidget extends GuiComponent implements Renderable, GuiEventListener, NarratableEntry {

    protected static final ResourceLocation HIDE_BUTTON_ICON_TEXTURE = new ResourceLocation("fancymenu", "textures/layout_editor/widgets/hide_icon.png");
    protected static final ResourceLocation EXPAND_BUTTON_ICON_TEXTURE = new ResourceLocation("fancymenu", "textures/layout_editor/widgets/expand_icon.png");
    protected static final ResourceLocation COLLAPSE_BUTTON_ICON_TEXTURE = new ResourceLocation("fancymenu", "textures/layout_editor/widgets/collapse_icon.png");

    protected final LayoutEditorScreen editor;
    private int widgetOffsetX = -50;
    private int widgetOffsetY = -50;
    private int innerWidth = 100;
    private int innerHeight = 100;
    protected SnappingSide snappingSide = SnappingSide.TOP_RIGHT;
    protected List<HeaderButton> headerButtons = new ArrayList<>();
    protected boolean hovered = false;
    protected boolean headerHovered = false;
    protected boolean visible = true;
    protected boolean expanded = true;
    protected boolean leftMouseDownHeader = false;
    protected double leftMouseDownHeaderMouseX = 0;
    protected double leftMouseDownHeaderMouseY = 0;
    protected int leftMouseDownHeaderWidgetOffsetX = 0;
    protected int leftMouseDownHeaderWidgetOffsetY = 0;

    /**
     * {@link AbstractLayoutEditorWidget}s need a constructor that takes only one parameter of type {@link LayoutEditorScreen}.
     */
    public AbstractLayoutEditorWidget(LayoutEditorScreen editor) {
        this.editor = editor;
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
        this.leftMouseDownHeader = false;
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        //Fix offset on render tick, if needed
        if (this.getAbsoluteX() < this.getMinAbsoluteX()) this.setWidgetOffsetX(this.widgetOffsetX);
        if (this.getAbsoluteX() > this.getMaxAbsoluteX()) this.setWidgetOffsetX(this.widgetOffsetX);
        if (this.getAbsoluteY() < this.getMinAbsoluteY()) this.setWidgetOffsetY(this.widgetOffsetY);
        if (this.getAbsoluteY() > this.getMaxAbsoluteY()) this.setWidgetOffsetY(this.widgetOffsetY);

        int scaledMouseX = (int) ((float)mouseX / UIBase.getFixedUIScale());
        int scaledMouseY = (int) ((float)mouseY / UIBase.getFixedUIScale());

        this.hovered = this.isMouseOver(scaledMouseX, scaledMouseY);
        this.headerHovered = this.isMouseOverHeader(scaledMouseX, scaledMouseY);

        pose.pushPose();
        pose.scale(UIBase.getFixedUIScale(), UIBase.getFixedUIScale(), UIBase.getFixedUIScale());
        RenderingUtils.resetShaderColor();

        if (this.isExpanded()) {
            this.renderBody(pose, scaledMouseX, scaledMouseY, partial, this.getScaledInnerX(), this.getScaledInnerY(), this.getInnerWidth(), this.getInnerHeight());
        }

        this.renderFrame(pose, scaledMouseX, scaledMouseY, partial);

        pose.popPose();
        RenderingUtils.resetShaderColor();

    }

    protected abstract void renderBody(@NotNull PoseStack pose, int mouseX, int mouseY, float partial, int x, int y, int width, int height);

    protected void renderFrame(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        this.renderHeader(pose, mouseX, mouseY, partial);

        //Separator between header and body
        if (this.isExpanded()) {
            RenderingUtils.resetShaderColor();
            int separatorXMin = this.getScaledAbsoluteX() + this.getBorderThickness();
            int separatorYMin =  this.getScaledAbsoluteY() + this.getBorderThickness() + this.getHeaderHeight();
            int separatorXMax = separatorXMin + this.getInnerWidth();
            int separatorYMax = separatorYMin + this.getBorderThickness();
            fill(pose, separatorXMin, separatorYMin, separatorXMax, separatorYMax, UIBase.getUIColorScheme().element_border_color_normal.getColorInt());
        }

        //Widget border
        RenderingUtils.resetShaderColor();
        if (this.isExpanded()) {
            UIBase.renderBorder(pose, this.getScaledAbsoluteX(), this.getScaledAbsoluteY(), this.getScaledAbsoluteX() + this.getAbsoluteWidth(), this.getScaledAbsoluteY() + this.getAbsoluteHeight(), this.getBorderThickness(), UIBase.getUIColorScheme().element_border_color_normal, true, true, true, true);
        } else {
            UIBase.renderBorder(pose, this.getScaledAbsoluteX(), this.getScaledAbsoluteY(), this.getScaledAbsoluteX() + this.getAbsoluteWidth(), this.getScaledAbsoluteY() + this.getBorderThickness() + this.getHeaderHeight() + this.getBorderThickness(), this.getBorderThickness(), UIBase.getUIColorScheme().element_border_color_normal, true, true, true, true);
        }

        RenderingUtils.resetShaderColor();

    }

    protected void renderHeader(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        //Background
        RenderingUtils.resetShaderColor();
        fill(pose, this.getScaledAbsoluteX() + this.getBorderThickness(), this.getScaledAbsoluteY() + this.getBorderThickness(), this.getScaledAbsoluteX() + this.getBorderThickness() + this.getInnerWidth(), this.getScaledAbsoluteY() + this.getBorderThickness() + this.getHeaderHeight(), UIBase.getUIColorScheme().element_background_color_normal.getColorInt());
        RenderingUtils.resetShaderColor();

        //Buttons
        int buttonX = this.getScaledAbsoluteX() + this.getBorderThickness() + this.getInnerWidth();
        for (HeaderButton b : this.headerButtons) {
            buttonX -= b.width;
            b.x = buttonX;
            b.y = this.getScaledAbsoluteY() + this.getBorderThickness();
            b.render(pose, mouseX, mouseY, partial);
        }

    }

    public void setWidgetOffsetX(int offsetX) {
        this.widgetOffsetX = offsetX;
        if (this.getAbsoluteX() < this.getMinAbsoluteX()) {
            int i = this.getMinAbsoluteX() - this.getAbsoluteX();
            this.widgetOffsetX += i;
        }
        if (this.getAbsoluteX() > this.getMaxAbsoluteX()) {
            int i = this.getAbsoluteX() - this.getMaxAbsoluteX();
            this.widgetOffsetX -= i;
        }
    }

    public void setWidgetOffsetY(int offsetY) {
        this.widgetOffsetY = offsetY;
        if (this.getAbsoluteY() < this.getMinAbsoluteY()) {
            int i = this.getMinAbsoluteY() - this.getAbsoluteY();
            this.widgetOffsetY += i;
        }
        if (this.getAbsoluteY() > this.getMaxAbsoluteY()) {
            int i = this.getAbsoluteY() - this.getMaxAbsoluteY();
            this.widgetOffsetY -= i;
        }
    }

    public int getWidgetOffsetX() {
        return this.widgetOffsetX;
    }

    public int getWidgetOffsetY() {
        return this.widgetOffsetY;
    }

    public int getAbsoluteX() {
        return this.snappingSide.getOriginX(this) + this.widgetOffsetX;
    }

    public int getAbsoluteY() {
        return this.snappingSide.getOriginY(this) + this.widgetOffsetY;
    }

    protected int getScreenEdgeBorderThickness() {
        return 3;
    }

    protected int getMinAbsoluteX() {
        return this.getScreenEdgeBorderThickness();
    }

    protected int getMaxAbsoluteX() {
        return ScreenUtils.getScreenWidth() - this.getScreenEdgeBorderThickness() - this.getScaledAbsoluteWidth();
    }

    protected int getMinAbsoluteY() {
        int scaledMenuBarHeight = (int)(this.editor.menuBar.getHeight() * UIBase.calculateFixedScale(this.editor.menuBar.getScale()));
        return scaledMenuBarHeight + this.getScreenEdgeBorderThickness();
    }

    protected int getMaxAbsoluteY() {
        return ScreenUtils.getScreenHeight() - this.getScreenEdgeBorderThickness() - this.getScaledAbsoluteHeight();
    }

    public int getScaledAbsoluteX() {
        return (int) ((float)this.getAbsoluteX() / UIBase.getFixedUIScale());
    }

    public int getScaledAbsoluteY() {
        return (int) ((float)this.getAbsoluteY() / UIBase.getFixedUIScale());
    }

    public int getScaledInnerX() {
        return this.getScaledAbsoluteX() + this.getBorderThickness();
    }

    public int getScaledInnerY() {
        return this.getScaledAbsoluteY() + this.getBorderThickness() + this.getHeaderHeight() + this.getBorderThickness();
    }

    public void setInnerWidth(int innerWidth) {
        this.innerWidth = innerWidth;
    }

    public void setInnerHeight(int innerHeight) {
        this.innerHeight = innerHeight;
    }

    public int getInnerWidth() {
        return this.innerWidth;
    }

    public int getInnerHeight() {
        return this.innerHeight;
    }

    public int getAbsoluteWidth() {
        return this.innerWidth + (this.getBorderThickness() * 2);
    }

    public int getAbsoluteHeight() {
        if (!this.isExpanded()) return this.getBorderThickness() + this.getHeaderHeight() + this.getBorderThickness();
        return this.getBorderThickness() + this.getHeaderHeight() + this.getBorderThickness() + this.innerHeight + this.getBorderThickness();
    }

    public int getScaledAbsoluteWidth() {
        return (int) ((float)this.getAbsoluteWidth() * UIBase.getFixedUIScale());
    }

    public int getScaledAbsoluteHeight() {
        return (int) ((float)this.getAbsoluteHeight() * UIBase.getFixedUIScale());
    }

    public int getHeaderHeight() {
        return 15;
    }

    public int getBorderThickness() {
        return 1;
    }

    public boolean isHovered() {
        if (!this.isVisible()) return false;
        if (!this.isExpanded()) return this.isHeaderHovered();
        return this.hovered;
    }

    public boolean isHeaderHovered() {
        if (!this.isVisible()) return false;
        return this.headerHovered;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public AbstractLayoutEditorWidget setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public boolean isExpanded() {
        return this.expanded;
    }

    public AbstractLayoutEditorWidget setExpanded(boolean expanded) {
        this.expanded = expanded;
        return this;
    }

    public boolean isMouseOverHeader(double mouseX, double mouseY) {
        if (!this.isVisible()) return false;
        return UIBase.isXYInArea(mouseX, mouseY, this.getScaledAbsoluteX(), this.getScaledAbsoluteY(), this.getAbsoluteWidth(), this.getHeaderHeight() + (this.getBorderThickness() * 2));
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!this.isVisible()) return false;
        if (!this.isExpanded()) return this.isMouseOverHeader(mouseX, mouseY);
        return UIBase.isXYInArea(mouseX, mouseY, this.getScaledAbsoluteX(), this.getScaledAbsoluteY(), this.getAbsoluteWidth(), this.getAbsoluteHeight());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int scaledMouseX = (int) ((float)mouseX / UIBase.getFixedUIScale());
        int scaledMouseY = (int) ((float)mouseY / UIBase.getFixedUIScale());
        if (this.isVisible()) {
            if (this.isHeaderHovered()) {
                this.leftMouseDownHeader = true;
                this.leftMouseDownHeaderMouseX = mouseX;
                this.leftMouseDownHeaderMouseY = mouseY;
                this.leftMouseDownHeaderWidgetOffsetX = this.widgetOffsetX;
                this.leftMouseDownHeaderWidgetOffsetY = this.widgetOffsetY;
            }
            for (HeaderButton b : this.headerButtons) {
                if (b.mouseClicked(scaledMouseX, scaledMouseY, button)) return true;
            }
        }
        return this.isVisible() && this.isMouseOver(scaledMouseX, scaledMouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.leftMouseDownHeader = false;
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double $$3, double $$4) {

        if (this.isVisible() && this.leftMouseDownHeader) {
            double offsetX = mouseX - this.leftMouseDownHeaderMouseX;
            double offsetY = mouseY - this.leftMouseDownHeaderMouseY;
            this.setWidgetOffsetX((int)(this.leftMouseDownHeaderWidgetOffsetX + offsetX));
            this.setWidgetOffsetY((int)(this.leftMouseDownHeaderWidgetOffsetY + offsetY));
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

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput var1) {
    }

    protected static class HeaderButton extends GuiComponent implements Renderable, GuiEventListener {

        protected AbstractLayoutEditorWidget parent;
        protected int x;
        protected int y;
        protected int width = 15;
        @NotNull
        protected Consumer<HeaderButton> clickAction;
        @NotNull
        protected ConsumingSupplier<HeaderButton, ITexture> iconSupplier;

        protected HeaderButton(AbstractLayoutEditorWidget parent, @NotNull ConsumingSupplier<HeaderButton, ITexture> iconSupplier, @NotNull Consumer<HeaderButton> clickAction) {
            this.parent = parent;
            this.iconSupplier = iconSupplier;
            this.clickAction = clickAction;
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

            this.renderHoverBackground(pose, mouseX, mouseY);

            ITexture icon = this.iconSupplier.get(this);
            if ((icon != null) && (icon.getResourceLocation() != null)) {
                RenderingUtils.resetShaderColor();
                RenderSystem.enableBlend();
                RenderingUtils.bindTexture(icon.getResourceLocation());
                blit(pose, this.x, this.y, 0.0F, 0.0F, this.width, this.parent.getHeaderHeight(), this.width, this.parent.getHeaderHeight());
                RenderingUtils.resetShaderColor();
            }

        }

        protected void renderHoverBackground(PoseStack pose, int mouseX, int mouseY) {
            if (this.isMouseOver(mouseX, mouseY)) {
                RenderingUtils.resetShaderColor();
                fill(pose, this.x, this.y, this.x + this.width, this.y + this.parent.getHeaderHeight(), UIBase.getUIColorScheme().element_background_color_hover.getColorInt());
                RenderingUtils.resetShaderColor();
            }
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
            if ((button == 0) && this.isMouseOver(mouseX, mouseY)) {
                if (FancyMenu.getOptions().playUiClickSounds.getValue()) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                this.clickAction.accept(this);
                return true;
            }
            return GuiEventListener.super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return UIBase.isXYInArea(mouseX, mouseY, this.x, this.y, this.width, this.parent.getHeaderHeight());
        }

    }

    public enum SnappingSide {

        TOP_LEFT("top-left", false, widget -> 0, AbstractLayoutEditorWidget::getMinAbsoluteY),
        TOP_RIGHT("top-right", false, widget -> ScreenUtils.getScreenWidth(), AbstractLayoutEditorWidget::getMinAbsoluteY);

        public final String name;
        public final boolean horizontal;
        private final ConsumingSupplier<AbstractLayoutEditorWidget, Integer> originXSupplier;
        private final ConsumingSupplier<AbstractLayoutEditorWidget, Integer> originYSupplier;

        SnappingSide(String name, boolean horizontal, ConsumingSupplier<AbstractLayoutEditorWidget, Integer> originXSupplier, ConsumingSupplier<AbstractLayoutEditorWidget, Integer> originYSupplier) {
            this.name = name;
            this.horizontal = horizontal;
            this.originXSupplier = originXSupplier;
            this.originYSupplier = originYSupplier;
        }

        public int getOriginX(AbstractLayoutEditorWidget widget) {
            return this.originXSupplier.get(widget);
        }

        public int getOriginY(AbstractLayoutEditorWidget widget) {
            return this.originYSupplier.get(widget);
        }

        @Nullable
        public static SnappingSide getByName(@NotNull String name) {
            for (SnappingSide s : SnappingSide.values()) {
                if (s.name.equals(name)) return s;
            }
            return null;
        }

    }

}
