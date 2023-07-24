package de.keksuccino.fancymenu.customization.layout.editor.widget.widgets.layer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidget;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidgetBuilder;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LayerLayoutEditorWidget extends AbstractLayoutEditorWidget {

    //TODO FIXEN: Pos + Size von widgets wird nicht (mehr?) gespeichert (evtl. settings im allgemeinen broken)
    //TODO FIXEN: Pos + Size von widgets wird nicht (mehr?) gespeichert (evtl. settings im allgemeinen broken)
    //TODO FIXEN: Pos + Size von widgets wird nicht (mehr?) gespeichert (evtl. settings im allgemeinen broken)
    //TODO FIXEN: Pos + Size von widgets wird nicht (mehr?) gespeichert (evtl. settings im allgemeinen broken)

    private static final Logger LOGGER = LogManager.getLogger();

    protected ScrollArea scrollArea;

    public LayerLayoutEditorWidget(LayoutEditorScreen editor, AbstractLayoutEditorWidgetBuilder<?> builder) {

        super(editor, builder);

        this.displayLabel = Component.translatable("fancymenu.editor.widgets.layers");

        this.scrollArea = new ScrollArea(0, 0, 0, 0) {
            @Override
            public void updateScrollArea() {
                int grabberOffset = 5;
                this.verticalScrollBar.scrollAreaStartX = this.getInnerX() + grabberOffset;
                this.verticalScrollBar.scrollAreaStartY = this.getInnerY() + grabberOffset;
                this.verticalScrollBar.scrollAreaEndX = this.getInnerX() + this.getInnerWidth() - grabberOffset;
                this.verticalScrollBar.scrollAreaEndY = this.getInnerY() + this.getInnerHeight() - this.horizontalScrollBar.grabberHeight - grabberOffset - 1;
                this.horizontalScrollBar.scrollAreaStartX = this.getInnerX() + grabberOffset;
                this.horizontalScrollBar.scrollAreaStartY = this.getInnerY() + grabberOffset;
                this.horizontalScrollBar.scrollAreaEndX = this.getInnerX() + this.getInnerWidth() - this.verticalScrollBar.grabberWidth - grabberOffset - 1;
                this.horizontalScrollBar.scrollAreaEndY = this.getInnerY() + this.getInnerHeight() - grabberOffset;
            }
        };

        this.scrollArea.borderColor = () -> UIBase.getUIColorScheme().area_background_color;

        this.updateList(false);

    }

    public void updateList(boolean keepScroll) {
        float scroll = this.scrollArea.verticalScrollBar.getScroll();
        this.scrollArea.clearEntries();
        for (AbstractEditorElement e : this.editor.normalEditorElements) {
            this.scrollArea.addEntry(new LayerElementEntry(this.scrollArea, this, e));
        }
        if (keepScroll) this.scrollArea.verticalScrollBar.setScroll(scroll);
    }

    @Override
    protected void renderBody(@NotNull PoseStack pose, double mouseX, double mouseY, float partial) {

        RenderingUtils.resetShaderColor();
        fillF(pose, this.getRealBodyX(), this.getRealBodyY(), this.getRealBodyX() + this.getBodyWidth(), this.getRealBodyY() + this.getBodyHeight(), UIBase.getUIColorScheme().area_background_color.getColorInt());

        this.scrollArea.setX(this.getRealBodyX());
        this.scrollArea.setY(this.getRealBodyY());
        this.scrollArea.setWidth(this.getBodyWidth());
        this.scrollArea.setHeight(this.getBodyHeight());
        this.scrollArea.setApplyScissor(false);
        this.scrollArea.horizontalScrollBar.active = false;
        this.scrollArea.makeEntriesWidthOfArea = true;
        this.scrollArea.makeAllEntriesWidthOfWidestEntry = false;
        this.enableComponentScissor((int) this.getRealBodyX()-5, (int) this.getRealBodyY(), (int) this.getBodyWidth()+10, (int) this.getBodyHeight()+1, true);
        pose.pushPose();
        pose.translate(0.0F, 0.0F, 400.0F);
        this.scrollArea.render(pose, (int) mouseX, (int) mouseY, partial);
        pose.popPose();
        this.disableComponentScissor();

    }

    @Override
    protected @Nullable ResizingEdge updateHoveredResizingEdge() {
        if (this.scrollArea.isMouseInteractingWithGrabbers()) return null;
        return super.updateHoveredResizingEdge();
    }

    @Override
    protected boolean mouseClickedComponent(double realMouseX, double realMouseY, double translatedMouseX, double translatedMouseY, int button) {

        if (super.mouseClickedComponent(realMouseX, realMouseY, translatedMouseX, translatedMouseY, button)) return true;

        //Override original mouseClicked of ScrollArea, to use a combination of real and translated mouse coordinates
        if (this.scrollArea.verticalScrollBar.mouseClicked(translatedMouseX, translatedMouseY, button)) return true;
        if (this.scrollArea.horizontalScrollBar.mouseClicked(translatedMouseX, translatedMouseY, button)) return true;
        for (ScrollAreaEntry entry : this.scrollArea.getEntries()) {
            if (entry.mouseClicked(realMouseX, realMouseY, button)) return true;
        }

        return this.isVisible() && this.isMouseOver();

    }

    @Override
    protected boolean mouseReleasedComponent(double realMouseX, double realMouseY, double translatedMouseX, double translatedMouseY, int button) {
        if (super.mouseReleasedComponent(realMouseX, realMouseY, translatedMouseX, translatedMouseY, button)) return true;
        return this.scrollArea.mouseReleased(realMouseX, realMouseY, button);
    }

    @Override
    protected boolean mouseDraggedComponent(double translatedMouseX, double translatedMouseY, int button, double d1, double d2) {
        if (super.mouseDraggedComponent(translatedMouseX, translatedMouseY, button, d1, d2)) return true;
        return this.scrollArea.mouseDragged(translatedMouseX, translatedMouseY, button, d1, d2);
    }

    @Override
    protected boolean mouseScrolledComponent(double realMouseX, double realMouseY, double translatedMouseX, double translatedMouseY, double scrollDelta) {
        if (super.mouseScrolledComponent(realMouseX, realMouseY, translatedMouseX, translatedMouseY, scrollDelta)) return true;
        return this.scrollArea.mouseScrolled(realMouseX, realMouseY, scrollDelta);
    }

    public static class LayerElementEntry extends ScrollAreaEntry {

        protected static final ResourceLocation MOVE_UP_TEXTURE = new ResourceLocation("fancymenu", "textures/layout_editor/widgets/layer/move_up.png");
        protected static final ResourceLocation MOVE_DOWN_TEXTURE = new ResourceLocation("fancymenu", "textures/layout_editor/widgets/layer/move_down.png");
        protected static final ResourceLocation MOVE_TO_TOP_TEXTURE = new ResourceLocation("fancymenu", "textures/layout_editor/widgets/layer/move_to_top.png");
        protected static final ResourceLocation MOVE_TO_BOTTOM_TEXTURE = new ResourceLocation("fancymenu", "textures/layout_editor/widgets/layer/move_to_bottom.png");

        protected AbstractEditorElement element;
        protected LayerLayoutEditorWidget layerWidget;
        protected boolean moveUpButtonHovered = false;
        protected boolean moveDownButtonHovered = false;
        protected Font font = Minecraft.getInstance().font;

        public LayerElementEntry(ScrollArea parent, LayerLayoutEditorWidget layerWidget, @NotNull AbstractEditorElement element) {
            super(parent, 50, 28);
            this.element = element;
            this.layerWidget = layerWidget;
            this.playClickSound = false;
            this.selectable = false;
            this.selectOnClick = false;
        }

        @Override
        public void renderEntry(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

            this.moveUpButtonHovered = this.isMoveUpButtonMouseOver(mouseX, mouseY);
            this.moveDownButtonHovered = this.isMoveDownButtonMouseOver(mouseX, mouseY);

            RenderSystem.enableBlend();

            UIBase.getUIColorScheme().setUITextureShaderColor(1.0F);
            RenderingUtils.bindTexture(MOVE_UP_TEXTURE);
            blitF(pose, this.x, this.y, 0.0F, 0.0F, this.getButtonWidth(), this.getButtonHeight(), this.getButtonWidth(), this.getButtonHeight());

            UIBase.getUIColorScheme().setUITextureShaderColor(1.0F);
            RenderingUtils.bindTexture(MOVE_DOWN_TEXTURE);
            blitF(pose, this.x, this.y + this.getButtonHeight(), 0.0F, 0.0F, this.getButtonWidth(), this.getButtonHeight(), this.getButtonWidth(), this.getButtonHeight());

            RenderingUtils.resetShaderColor();

            this.layerWidget.enableComponentScissor((int)(this.x + this.getButtonWidth() + 1), (int) this.y, (int) (this.getWidth() - this.getButtonWidth() - 4), (int) this.getHeight(), true);
            UIBase.drawElementLabelF(pose, this.font, this.element.element.builder.getDisplayName(this.element.element), (int)(this.getX() + this.getButtonWidth() + 3), (int)(this.getY() + (this.getHeight() / 2f) - (this.font.lineHeight / 2f)));
            this.layerWidget.disableComponentScissor();

        }

        public boolean isMoveUpButtonHovered() {
            return this.moveUpButtonHovered;
        }

        public boolean isMoveDownButtonHovered() {
            return this.moveDownButtonHovered;
        }

        public boolean isMoveUpButtonMouseOver(double mouseX, double mouseY) {
            if (this.parent.isMouseInteractingWithGrabbers()) return false;
            if (!this.parent.isInnerAreaHovered()) return false;
            return isXYInArea(mouseX, mouseY, this.x, this.y, this.getButtonWidth(), this.getButtonHeight());
        }

        public boolean isMoveDownButtonMouseOver(double mouseX, double mouseY) {
            if (this.parent.isMouseInteractingWithGrabbers()) return false;
            if (!this.parent.isInnerAreaHovered()) return false;
            return isXYInArea(mouseX, mouseY, this.x, this.y + this.getButtonHeight(), this.getButtonWidth(), this.getButtonHeight());
        }

        public float getButtonHeight() {
            return 14f;
        }

        public float getButtonWidth() {
            return 30f;
        }

        @Override
        public void onClick(ScrollAreaEntry entry) {
            if (FancyMenu.getOptions().playUiClickSounds.getValue()) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            if (this.isMoveUpButtonHovered()) {
                this.layerWidget.editor.moveElementUp(this.element);
                MainThreadTaskExecutor.executeInMainThread(() -> this.layerWidget.updateList(true), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            } else if (this.isMoveDownButtonHovered()) {
                this.layerWidget.editor.moveElementDown(this.element);
                MainThreadTaskExecutor.executeInMainThread(() -> this.layerWidget.updateList(true), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            } else {
                if (!Screen.hasControlDown()) this.layerWidget.editor.deselectAllElements();
                this.element.setSelected(true);
            }
        }

    }

}
