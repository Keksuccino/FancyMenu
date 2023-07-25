package de.keksuccino.fancymenu.customization.layout.editor.widget.widgets.layer;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidget;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidgetBuilder;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinScreen;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.ExtendedEditBox;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

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

    @Override
    public void refresh() {
        super.refresh();
        this.updateList(false);
    }

    public void updateList(boolean keepScroll) {
        float scroll = this.scrollArea.verticalScrollBar.getScroll();
        this.scrollArea.clearEntries();
        if (this.editor.layout.renderElementsBehindVanilla) {
            this.scrollArea.addEntry(new VanillaLayerElementEntry(this.scrollArea, this));
            this.scrollArea.addEntry(new SeparatorEntry(this.scrollArea));
        }
        for (AbstractEditorElement e : Lists.reverse(new ArrayList<>(this.editor.normalEditorElements))) {
            this.scrollArea.addEntry(new LayerElementEntry(this.scrollArea, this, e));
            this.scrollArea.addEntry(new SeparatorEntry(this.scrollArea));
        }
        if (!this.editor.layout.renderElementsBehindVanilla) {
            this.scrollArea.addEntry(new VanillaLayerElementEntry(this.scrollArea, this));
            this.scrollArea.addEntry(new SeparatorEntry(this.scrollArea));
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
    public void tick() {
        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof LayerElementEntry l) {
                l.editLayerNameBox.tick();
            }
        }
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

    @Override
    public void editorElementAdded(@NotNull AbstractEditorElement element) {
        this.updateList(false);
    }

    @Override
    public void editorElementRemovedOrHidden(@NotNull AbstractEditorElement element) {
        this.updateList(false);
    }

    public static class LayerElementEntry extends ScrollAreaEntry {

        protected static final ResourceLocation MOVE_UP_TEXTURE = new ResourceLocation("fancymenu", "textures/layout_editor/widgets/layers/move_up.png");
        protected static final ResourceLocation MOVE_DOWN_TEXTURE = new ResourceLocation("fancymenu", "textures/layout_editor/widgets/layers/move_down.png");

        protected AbstractEditorElement element;
        protected LayerLayoutEditorWidget layerWidget;
        protected boolean moveUpButtonHovered = false;
        protected boolean moveDownButtonHovered = false;
        protected Font font = Minecraft.getInstance().font;
        protected ExtendedEditBox editLayerNameBox;
        protected boolean displayEditLayerNameBox = false;
        protected boolean layerNameHovered = false;
        protected long lastLeftClick = -1L;

        public LayerElementEntry(ScrollArea parent, LayerLayoutEditorWidget layerWidget, @NotNull AbstractEditorElement element) {
            super(parent, 50, 28);
            this.element = element;
            this.layerWidget = layerWidget;
            this.playClickSound = false;
            this.selectable = false;
            this.selectOnClick = false;
            this.editLayerNameBox = new ExtendedEditBox(this.font, 0, 0, 0, 0, Component.empty());
            this.editLayerNameBox.setCanLoseFocus(false);
        }

        @Override
        public void renderEntry(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

            this.moveUpButtonHovered = this.isMoveUpButtonMouseOver(mouseX, mouseY);
            this.moveDownButtonHovered = this.isMoveDownButtonMouseOver(mouseX, mouseY);
            this.layerNameHovered = this.isLayerNameMouseOver(mouseX, mouseY);

            RenderSystem.enableBlend();

            if (this.element.isSelected() || this.element.isMultiSelected()) {
                RenderingUtils.resetShaderColor();
                fillF(pose, this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), UIBase.getUIColorScheme().element_background_color_hover.getColorInt());
            }

            UIBase.getUIColorScheme().setUITextureShaderColor(this.layerWidget.editor.canBeMovedUp(this.element) ? 1.0f : 0.3f);
            RenderingUtils.bindTexture(MOVE_UP_TEXTURE);
            blitF(pose, this.x, this.y, 0.0F, 0.0F, this.getButtonWidth(), this.getButtonHeight(), this.getButtonWidth(), this.getButtonHeight());

            UIBase.getUIColorScheme().setUITextureShaderColor(this.layerWidget.editor.canBeMovedDown(this.element) ? 1.0f : 0.3f);
            RenderingUtils.bindTexture(MOVE_DOWN_TEXTURE);
            blitF(pose, this.x, this.y + this.getButtonHeight(), 0.0F, 0.0F, this.getButtonWidth(), this.getButtonHeight(), this.getButtonWidth(), this.getButtonHeight());

            RenderingUtils.resetShaderColor();

            if (!this.displayEditLayerNameBox) {
                this.layerWidget.enableComponentScissor((int) (this.x + this.getButtonWidth() + 1f), (int) this.y, (int) (this.getWidth() - this.getButtonWidth() - 4f), (int) this.getHeight(), true);
                UIBase.drawElementLabelF(pose, this.font, Component.literal(this.getLayerName()), (int) (this.getX() + this.getButtonWidth() + 3f), (int) (this.getY() + (this.getHeight() / 2f) - (this.font.lineHeight / 2f)));
                this.layerWidget.disableComponentScissor();
            } else {
                UIBase.applyDefaultWidgetSkinTo(this.editLayerNameBox);
                this.editLayerNameBox.setX((int)(this.getX() + this.getButtonWidth() + 3f));
                this.editLayerNameBox.setY((int)(this.getY() + (this.getHeight() / 2f) - ((this.font.lineHeight + 2) / 2f)));
//                this.editLayerNameBox.setWidth((int) (this.getX() + this.getWidth() - 3f) - this.editLayerNameBox.getX());
                this.editLayerNameBox.setWidth((int)this.getLayerNameWidth());
                ((IMixinAbstractWidget)this.editLayerNameBox).setHeightFancyMenu(this.font.lineHeight);
                this.editLayerNameBox.render(pose, mouseX, mouseY, partial);
            }

        }

        @SuppressWarnings("all")
        public String getLayerName() {
            if (this.element.element.customElementLayerName != null) return this.element.element.customElementLayerName;
            return this.element.element.builder.getDisplayName(this.element.element).getString();
        }

        public float getLayerNameWidth() {
            float x = this.getX() + this.getButtonWidth() + 3f;
            float textWidth = this.font.width(this.getLayerName());
            if ((x + textWidth) > (this.getX() + this.getWidth() - 3f)) {
                textWidth = (this.getX() + this.getWidth() - 3f) - x;
            }
            return textWidth;
        }

        public boolean isMoveUpButtonHovered() {
            return this.moveUpButtonHovered;
        }

        public boolean isMoveDownButtonHovered() {
            return this.moveDownButtonHovered;
        }

        public boolean isLayerNameHovered() {
            return this.layerNameHovered;
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

        public boolean isLayerNameMouseOver(double mouseX, double mouseY) {
            if (this.parent.isMouseInteractingWithGrabbers()) return false;
            if (!this.parent.isInnerAreaHovered()) return false;
            float x = this.getX() + this.getButtonWidth() + 3f;
            float textWidth = this.getLayerNameWidth();
            return isXYInArea(mouseX, mouseY, x, this.getY() + (this.getHeight() / 2f) - (this.font.lineHeight / 2f), textWidth, this.font.lineHeight);
        }

        public float getButtonHeight() {
            return 14f;
        }

        public float getButtonWidth() {
            return 30f;
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            if (button == 0) {
                if (this.isMoveUpButtonHovered()) {
                    if (this.layerWidget.editor.canBeMovedUp(this.element)) {
                        if (FancyMenu.getOptions().playUiClickSounds.getValue()) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        this.layerWidget.editor.history.saveSnapshot();
                        if (!this.element.isSelected()) this.layerWidget.editor.deselectAllElements();
                        this.element.setSelected(true);
                        this.layerWidget.editor.moveElementUp(this.element);
                        MainThreadTaskExecutor.executeInMainThread(() -> this.layerWidget.updateList(true), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                    }
                } else if (this.isMoveDownButtonHovered()) {
                    if (this.layerWidget.editor.canBeMovedDown(this.element)) {
                        if (FancyMenu.getOptions().playUiClickSounds.getValue()) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        this.layerWidget.editor.history.saveSnapshot();
                        if (!this.element.isSelected()) this.layerWidget.editor.deselectAllElements();
                        this.element.setSelected(true);
                        this.layerWidget.editor.moveElementDown(this.element);
                        MainThreadTaskExecutor.executeInMainThread(() -> this.layerWidget.updateList(true), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                    }
                } else if (this.isLayerNameHovered()) {
                    long now = System.currentTimeMillis();
                    if ((this.lastLeftClick + 400) > now) {
                        this.editLayerNameBox.setValue(this.getLayerName());
                        this.editLayerNameBox.setFocused(true);
                        this.displayEditLayerNameBox = true;
                        if (this.layerWidget.editor.children().isEmpty() || (this.layerWidget.editor.children().get(0) != this.editLayerNameBox)) {
                            MainThreadTaskExecutor.executeInMainThread(() -> {
                                this.layerWidget.editor.children().remove(this.editLayerNameBox);
                                ((IMixinScreen)this.layerWidget.editor).getChildrenFancyMenu().add(0, this.layerWidget);
                            }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                        }
                    }
                    this.lastLeftClick = now;
                } else {
                    if (FancyMenu.getOptions().playUiClickSounds.getValue()) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    if (!Screen.hasControlDown()) this.layerWidget.editor.deselectAllElements();
                    this.element.setSelected(!this.element.isSelected());
                }
            }
            if (button == 1) {
                if (!this.element.isSelected()) this.layerWidget.editor.deselectAllElements();
                this.element.setSelected(true);
                MainThreadTaskExecutor.executeInMainThread(() -> this.layerWidget.editor.openElementContextMenuAtMouseIfPossible(), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            }
            if (!this.isLayerNameHovered()) {
                if (this.displayEditLayerNameBox) {
                    this.element.element.customElementLayerName = this.editLayerNameBox.getValue();
                    if (this.element.element.customElementLayerName.replace(" ", "").isEmpty()) this.element.element.customElementLayerName = null;
                }
                this.editLayerNameBox.setFocused(false);
                this.displayEditLayerNameBox = false;
                MainThreadTaskExecutor.executeInMainThread(() -> ((IMixinScreen)this.layerWidget.editor).getChildrenFancyMenu().remove(this.editLayerNameBox), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            }
        }

    }

    public static class VanillaLayerElementEntry extends ScrollAreaEntry {

        protected static final ResourceLocation MOVE_TO_TOP_TEXTURE = new ResourceLocation("fancymenu", "textures/layout_editor/widgets/layers/move_top.png");
        protected static final ResourceLocation MOVE_BEHIND_TEXTURE = new ResourceLocation("fancymenu", "textures/layout_editor/widgets/layers/move_bottom.png");

        protected LayerLayoutEditorWidget layerWidget;
        protected boolean moveTopBottomButtonHovered = false;
        protected Font font = Minecraft.getInstance().font;

        public VanillaLayerElementEntry(ScrollArea parent, LayerLayoutEditorWidget layerWidget) {
            super(parent, 50, 28);
            this.layerWidget = layerWidget;
            this.playClickSound = false;
            this.selectable = false;
            this.selectOnClick = false;
        }

        @Override
        public void renderEntry(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

            this.moveTopBottomButtonHovered = this.isMoveTopBottomButtonHovered(mouseX, mouseY);

            RenderSystem.enableBlend();

            UIBase.getUIColorScheme().setUITextureShaderColor(1.0F);
            RenderingUtils.bindTexture(this.layerWidget.editor.layout.renderElementsBehindVanilla ? MOVE_BEHIND_TEXTURE : MOVE_TO_TOP_TEXTURE);
            blitF(pose, this.x, this.y, 0.0F, 0.0F, this.getButtonWidth(), this.getButtonHeight(), this.getButtonWidth(), this.getButtonHeight());

            RenderingUtils.resetShaderColor();

            this.layerWidget.enableComponentScissor((int)(this.x + this.getButtonWidth() + 1f), (int) this.y, (int) (this.getWidth() - this.getButtonWidth() - 4f), (int) this.getHeight(), true);
            UIBase.drawElementLabelF(pose, this.font, Component.translatable("fancymenu.editor.widgets.layers.vanilla_elements").setStyle(Style.EMPTY.withColor(UIBase.getUIColorScheme().warning_text_color.getColorInt())), (int)(this.getX() + this.getButtonWidth() + 3f), (int)(this.getY() + (this.getHeight() / 2f) - (this.font.lineHeight / 2f)));
            this.layerWidget.disableComponentScissor();

        }

        public boolean isMoveTopBottomButtonHovered() {
            return this.moveTopBottomButtonHovered;
        }

        public boolean isMoveTopBottomButtonHovered(double mouseX, double mouseY) {
            if (this.parent.isMouseInteractingWithGrabbers()) return false;
            if (!this.parent.isInnerAreaHovered()) return false;
            return isXYInArea(mouseX, mouseY, this.x, this.y, this.getButtonWidth(), this.getButtonHeight());
        }

        public float getButtonHeight() {
            return 28f;
        }

        public float getButtonWidth() {
            return 30f;
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            if (button == 0) {
                if (this.isMoveTopBottomButtonHovered()) {
                    if (FancyMenu.getOptions().playUiClickSounds.getValue()) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    this.layerWidget.editor.history.saveSnapshot();
                    this.layerWidget.editor.layout.renderElementsBehindVanilla = !this.layerWidget.editor.layout.renderElementsBehindVanilla;
                    this.layerWidget.editor.deselectAllElements();
                    MainThreadTaskExecutor.executeInMainThread(() -> this.layerWidget.updateList(true), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                }
            }
        }

    }

    public static class SeparatorEntry extends ScrollAreaEntry {

        public SeparatorEntry(ScrollArea parent) {
            super(parent, 50f, 1f);
            this.selectable = false;
            this.selectOnClick = false;
        }

        @Override
        public void renderEntry(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            RenderSystem.enableBlend();
            fillF(pose, this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), UIBase.getUIColorScheme().element_border_color_normal.getColorInt());
            RenderingUtils.resetShaderColor();
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return false;
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
        }

    }

}
