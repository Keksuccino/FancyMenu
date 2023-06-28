package de.keksuccino.fancymenu.customization.layout.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Renderable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AnchorPointOverlay extends GuiComponent implements Renderable {

    protected final LayoutEditorScreen editor;
    protected List<AbstractEditorElement> draggedElements = new ArrayList<>();

    protected Supplier<DrawableColor> colorNormal = () -> UIBase.getUIColorScheme().layout_editor_anchor_point_area_color_normal;
    protected Supplier<DrawableColor> colorHover = () -> UIBase.getUIColorScheme().layout_editor_anchor_point_area_color_hover;

    protected AnchorPointArea topLeftArea = new AnchorPointArea(ElementAnchorPoints.TOP_LEFT, 60, 60, colorNormal, colorHover);
    protected AnchorPointArea midLeftArea = new AnchorPointArea(ElementAnchorPoints.MID_LEFT, 60, 60, colorNormal, colorHover);
    protected AnchorPointArea bottomLeftArea = new AnchorPointArea(ElementAnchorPoints.BOTTOM_LEFT, 60, 60, colorNormal, colorHover);
    protected AnchorPointArea topCenteredArea = new AnchorPointArea(ElementAnchorPoints.TOP_CENTERED, 60, 60, colorNormal, colorHover);
    protected AnchorPointArea midCenteredArea = new AnchorPointArea(ElementAnchorPoints.MID_CENTERED, 40, 40, colorNormal, colorHover);
    protected AnchorPointArea bottomCenteredArea = new AnchorPointArea(ElementAnchorPoints.BOTTOM_CENTERED, 60, 60, colorNormal, colorHover);
    protected AnchorPointArea topRightArea = new AnchorPointArea(ElementAnchorPoints.TOP_RIGHT, 60, 60, colorNormal, colorHover);
    protected AnchorPointArea midRightArea = new AnchorPointArea(ElementAnchorPoints.MID_RIGHT, 60, 60, colorNormal, colorHover);
    protected AnchorPointArea bottomRightArea = new AnchorPointArea(ElementAnchorPoints.BOTTOM_RIGHT, 60, 60, colorNormal, colorHover);
    protected AnchorPointArea[] anchorPointAreas = new AnchorPointArea[] { topLeftArea, midLeftArea, bottomLeftArea, topCenteredArea, midCenteredArea, bottomCenteredArea, topRightArea, midRightArea, bottomRightArea };

    public AnchorPointOverlay(@NotNull LayoutEditorScreen editor) {
        this.editor = editor;
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        this.topLeftArea.x = -(this.topLeftArea.width / 2);
        this.topLeftArea.y = -(this.topLeftArea.height / 2);
        this.topLeftArea.render(pose, mouseX, mouseY, partial);

        this.midLeftArea.x = -(this.midLeftArea.width / 2);
        this.midLeftArea.y = (ScreenUtils.getScreenHeight() / 2) - (this.midLeftArea.height / 2);
        this.midLeftArea.render(pose, mouseX, mouseY, partial);

        this.bottomLeftArea.x = -(this.bottomLeftArea.width / 2);
        this.bottomLeftArea.y = ScreenUtils.getScreenHeight() - (this.bottomLeftArea.height / 2);
        this.bottomLeftArea.render(pose, mouseX, mouseY, partial);

        this.topCenteredArea.x = (ScreenUtils.getScreenWidth() / 2) - (this.topCenteredArea.width / 2);
        this.topCenteredArea.y = -(this.topCenteredArea.height / 2);
        this.topCenteredArea.render(pose, mouseX, mouseY, partial);

        this.midCenteredArea.x = (ScreenUtils.getScreenWidth() / 2) - (this.midCenteredArea.width / 2);
        this.midCenteredArea.y = (ScreenUtils.getScreenHeight() / 2) - (this.midCenteredArea.height / 2);
        this.midCenteredArea.render(pose, mouseX, mouseY, partial);

        this.bottomCenteredArea.x = (ScreenUtils.getScreenWidth() / 2) - (this.bottomCenteredArea.width / 2);
        this.bottomCenteredArea.y = ScreenUtils.getScreenHeight() - (this.bottomCenteredArea.height / 2);
        this.bottomCenteredArea.render(pose, mouseX, mouseY, partial);

        this.topRightArea.x = ScreenUtils.getScreenWidth() - (this.topRightArea.width / 2);
        this.topRightArea.y = -(this.topRightArea.height / 2);
        this.topRightArea.render(pose, mouseX, mouseY, partial);

        this.midRightArea.x = ScreenUtils.getScreenWidth() - (this.midRightArea.width / 2);
        this.midRightArea.y = (ScreenUtils.getScreenHeight() / 2) - (this.midRightArea.height / 2);
        this.midRightArea.render(pose, mouseX, mouseY, partial);

        this.bottomRightArea.x = ScreenUtils.getScreenWidth() - (this.bottomRightArea.width / 2);
        this.bottomRightArea.y = ScreenUtils.getScreenHeight() - (this.bottomRightArea.height / 2);
        this.bottomRightArea.render(pose, mouseX, mouseY, partial);

    }

    protected void renderConnectionLines() {
        for (AbstractEditorElement e : this.draggedElements) {
            AnchorPointArea a = this.getAreaForElement(e);
            if (a != null) {
                int xStart = e.getX() + (e.getWidth() / 2);
                int yStart = e.getY() + (e.getHeight() / 2);
                int xEnd = a.x + (a.width / 2);
                int yEnd = a.y + (a.height / 2);
                //TODO render line
            }
        }
    }

    public void updateDraggedElements() {
        this.draggedElements = this.getDraggedEditorElements();
    }

    @Nullable
    protected AnchorPointArea getAreaForElement(@NotNull AbstractEditorElement element) {
        for (AnchorPointArea a : this.anchorPointAreas) {
            if (a.anchorPoint == element.element.anchorPoint) return a;
        }
        return null;
    }

    @NotNull
    protected List<AbstractEditorElement> getDraggedEditorElements() {
        List<AbstractEditorElement> l = this.editor.getSelectedElements();
        if (!l.isEmpty() && (l.get(0).isDragged())) {
            return l;
        }
        return new ArrayList<>();
    }

    protected class AnchorPointArea extends GuiComponent implements Renderable {

        protected ElementAnchorPoint anchorPoint;
        protected int x;
        protected int y;
        protected int width;
        protected int height;
        protected boolean hovered = false;
        protected Supplier<DrawableColor> colorNormal;
        protected Supplier<DrawableColor> colorHover;

        protected AnchorPointArea(@NotNull ElementAnchorPoint anchorPoint, int width, int height, @NotNull Supplier<DrawableColor> colorNormal, @NotNull Supplier<DrawableColor> colorHover) {
            this.width = width;
            this.height = height;
            this.colorNormal = colorNormal;
            this.colorHover = colorHover;
            this.anchorPoint = anchorPoint;
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            this.hovered = UIBase.isXYInArea(mouseX, mouseY, this.x, this.y, this.width, this.height);
            this.handleHover();
            int endX = this.x + this.width;
            int endY = this.y + this.height;
            DrawableColor color = this.hovered ? this.colorHover.get() : this.colorNormal.get();
            fill(pose, this.x, this.y, endX, endY, RenderUtils.replaceAlphaInColor(color.getColorInt(), 70));
            UIBase.renderBorder(pose, this.x, this.y, endX, endY, 1, color, true, true, true, true);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        protected void handleHover() {
            if (this.hovered) {
                for (AbstractEditorElement e : AnchorPointOverlay.this.draggedElements) {
                    if (e.element.anchorPoint != this.anchorPoint) {
                        //TODO keep current pos after changing anchor
                        e.setAnchorPoint(this.anchorPoint);
                    }
                }
            }
        }

    }

}
