package de.keksuccino.fancymenu.customization.layout.editor.widget.widgets.layer;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidget;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidgetBuilder;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Objects;

public class LayerLayoutEditorWidget extends AbstractLayoutEditorWidget {

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

        this.scrollArea.borderColor = () -> UIBase.getUIColorTheme().area_background_color;

        this.updateList(false);

    }

    @Override
    public void refresh() {
        super.refresh();
        this.updateList(false);
    }

    public void updateList(boolean keepScroll) {
        float scroll = this.scrollArea.verticalScrollBar.getScroll();
        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof LayerElementEntry l) {
                this.children.remove(l.editLayerNameBox);
            }
        }
        this.scrollArea.clearEntries();
        if (this.editor.layout.renderElementsBehindVanilla) {
            this.scrollArea.addEntry(new VanillaLayerElementEntry(this.scrollArea, this));
            this.scrollArea.addEntry(new SeparatorEntry(this.scrollArea));
        }
        for (AbstractEditorElement e : Lists.reverse(new ArrayList<>(this.editor.normalEditorElements))) {
            LayerElementEntry layer = new LayerElementEntry(this.scrollArea, this, e);
            this.children.add(layer.editLayerNameBox);
            this.scrollArea.addEntry(layer);
            this.scrollArea.addEntry(new SeparatorEntry(this.scrollArea));
        }
        if (!this.editor.layout.renderElementsBehindVanilla) {
            this.scrollArea.addEntry(new VanillaLayerElementEntry(this.scrollArea, this));
            this.scrollArea.addEntry(new SeparatorEntry(this.scrollArea));
        }
        if (keepScroll) this.scrollArea.verticalScrollBar.setScroll(scroll);
    }

    @Override
    protected void renderBody(@NotNull GuiGraphics graphics, double mouseX, double mouseY, float partial) {

        fillF(graphics, this.getRealBodyX(), this.getRealBodyY(), this.getRealBodyX() + this.getBodyWidth(), this.getRealBodyY() + this.getBodyHeight(), UIBase.getUIColorTheme().area_background_color.getColorInt());

        this.scrollArea.setX(this.getRealBodyX());
        this.scrollArea.setY(this.getRealBodyY());
        this.scrollArea.setWidth(this.getBodyWidth());
        this.scrollArea.setHeight(this.getBodyHeight());
        this.scrollArea.setApplyScissor(false);
        this.scrollArea.horizontalScrollBar.active = false;
        this.scrollArea.makeEntriesWidthOfArea = true;
        this.scrollArea.makeAllEntriesWidthOfWidestEntry = false;
        this.enableComponentScissor(graphics, (int) this.getRealBodyX()-5, (int) this.getRealBodyY(), (int) this.getBodyWidth()+10, (int) this.getBodyHeight()+1, true);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 400.0F);
        this.scrollArea.render(graphics, (int) mouseX, (int) mouseY, partial);
        graphics.pose().popPose();
        this.disableComponentScissor(graphics);

    }

    @Override
    protected @Nullable ResizingEdge updateHoveredResizingEdge() {
        if (this.scrollArea.isMouseInteractingWithGrabbers()) return null;
        return super.updateHoveredResizingEdge();
    }

    @Override
    protected boolean mouseClickedComponent(double realMouseX, double realMouseY, double translatedMouseX, double translatedMouseY, int button) {

        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof LayerElementEntry l) {
                if (!l.isLayerNameHovered()) {
                    l.stopEditingLayerName();
                }
            }
        }

        if (this.isVisible()) {
            if (super.mouseClickedComponent(realMouseX, realMouseY, translatedMouseX, translatedMouseY, button)) return true;
            if (this.isExpanded()) {
                //Override original mouseClicked of ScrollArea, to use a combination of real and translated mouse coordinates
                if (this.scrollArea.verticalScrollBar.mouseClicked(translatedMouseX, translatedMouseY, button)) return true;
                if (this.scrollArea.horizontalScrollBar.mouseClicked(translatedMouseX, translatedMouseY, button)) return true;
                for (ScrollAreaEntry entry : this.scrollArea.getEntries()) {
                    if (entry.mouseClicked(realMouseX, realMouseY, button)) return true;
                }
            }
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
    protected boolean mouseScrolledComponent(double realMouseX, double realMouseY, double translatedMouseX, double translatedMouseY, double scrollDeltaX, double scrollDeltaY) {
        if (super.mouseScrolledComponent(realMouseX, realMouseY, translatedMouseX, translatedMouseY, scrollDeltaX, scrollDeltaY)) return true;
        return this.scrollArea.mouseScrolled(realMouseX, realMouseY, scrollDeltaX, scrollDeltaY);
    }

    @Override
    public void editorElementAdded(@NotNull AbstractEditorElement element) {
        this.updateList(false);
    }

    @Override
    public void editorElementRemovedOrHidden(@NotNull AbstractEditorElement element) {
        this.updateList(false);
    }

    @Override
    public void editorElementOrderChanged(@NotNull AbstractEditorElement element, boolean movedUp) {
        this.updateList(false);
    }

    public static class LayerElementEntry extends ScrollAreaEntry {

        protected static final ResourceLocation MOVE_UP_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/layout_editor/widgets/layers/move_up.png");
        protected static final ResourceLocation MOVE_DOWN_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/layout_editor/widgets/layers/move_down.png");

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
            this.editLayerNameBox = new ExtendedEditBox(this.font, 0, 0, 0, 0, Component.empty()) {
                @Override
                public boolean keyPressed(int keycode, int scancode, int modifiers) {
                    if (this.isVisible() && LayerElementEntry.this.displayEditLayerNameBox) {
                        if (keycode == InputConstants.KEY_ENTER) {
                            LayerElementEntry.this.stopEditingLayerName();
                            return true;
                        }
                    }
                    return super.keyPressed(keycode, scancode, modifiers);
                }
            };
            this.editLayerNameBox.setVisible(false);
            this.editLayerNameBox.setMaxLength(10000);
        }

        @Override
        public void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            this.moveUpButtonHovered = this.isMoveUpButtonMouseOver(mouseX, mouseY);
            this.moveDownButtonHovered = this.isMoveDownButtonMouseOver(mouseX, mouseY);
            this.layerNameHovered = this.isLayerNameMouseOver(mouseX, mouseY);

            RenderSystem.enableBlend();

            if (this.element.isSelected() || this.element.isMultiSelected()) {
                fillF(graphics, this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), UIBase.getUIColorTheme().element_background_color_hover.getColorInt());
            }

            blitF(graphics, MOVE_UP_TEXTURE, this.x, this.y, 0.0F, 0.0F, this.getButtonWidth(), this.getButtonHeight(), this.getButtonWidth(), this.getButtonHeight(), UIBase.getUIColorTheme().ui_texture_color.getColorIntWithAlpha(this.layerWidget.editor.canMoveLayerUp(this.element) ? 1.0f : 0.3f));

            blitF(graphics, MOVE_DOWN_TEXTURE, this.x, this.y + this.getButtonHeight(), 0.0F, 0.0F, this.getButtonWidth(), this.getButtonHeight(), this.getButtonWidth(), this.getButtonHeight(), UIBase.getUIColorTheme().ui_texture_color.getColorIntWithAlpha(this.layerWidget.editor.canMoveLayerUp(this.element) ? 1.0f : 0.3f));

            if (!this.displayEditLayerNameBox) {
                this.layerWidget.enableComponentScissor(graphics, (int) (this.x + this.getButtonWidth() + 1f), (int) this.y, (int) (this.getWidth() - this.getButtonWidth() - 4f), (int) this.getHeight(), true);
                UIBase.drawElementLabel(graphics, this.font, Component.literal(this.getLayerName()), (int)this.getLayerNameX(), (int)this.getLayerNameY());
                this.layerWidget.disableComponentScissor(graphics);
            } else {
                UIBase.applyDefaultWidgetSkinTo(this.editLayerNameBox);
                this.editLayerNameBox.setX((int)this.getLayerNameX());
                this.editLayerNameBox.setY((int)this.getLayerNameY() - 1);
                this.editLayerNameBox.setWidth((int) Math.min(this.getMaxLayerNameWidth(), this.font.width(this.editLayerNameBox.getValue() + 13)));
                if (this.editLayerNameBox.getWidth() < this.getMaxLayerNameWidth()) {
                    this.editLayerNameBox.setDisplayPosition(0);
                }
                ((IMixinAbstractWidget)this.editLayerNameBox).setHeightFancyMenu(this.font.lineHeight + 2);
                this.editLayerNameBox.render(graphics, mouseX, mouseY, partial);
            }

        }

        protected void startEditingLayerName() {
            this.editLayerNameBox.setVisible(true);
            this.editLayerNameBox.setFocused(true);
            this.editLayerNameBox.setValue(this.getLayerName());
            this.editLayerNameBox.moveCursorToEnd(false);
            this.displayEditLayerNameBox = true;
        }

        protected void stopEditingLayerName() {
            if (this.displayEditLayerNameBox) {
                String oldLayerName = this.getLayerName();
                this.element.element.customElementLayerName = this.editLayerNameBox.getValue();
                if (Objects.equals(oldLayerName, this.element.element.customElementLayerName)) this.element.element.customElementLayerName = null;
                if ((this.element.element.customElementLayerName != null) && this.element.element.customElementLayerName.replace(" ", "").isEmpty()) this.element.element.customElementLayerName = null;
            }
            this.editLayerNameBox.setFocused(false);
            this.editLayerNameBox.setVisible(false);
            this.displayEditLayerNameBox = false;
        }

        @SuppressWarnings("all")
        public String getLayerName() {
            if (this.element.element.customElementLayerName != null) return this.element.element.customElementLayerName;
            return this.element.element.builder.getDisplayName(this.element.element).getString();
        }

        public float getLayerNameX() {
            return this.getX() + this.getButtonWidth() + 3f;
        }

        public float getLayerNameY() {
            return this.getY() + (this.getHeight() / 2f) - (this.font.lineHeight / 2f);
        }

        public float getMaxLayerNameWidth() {
            return (this.getX() + this.getWidth() - 3f) - this.getLayerNameX();
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
            return isXYInArea(mouseX, mouseY, this.getLayerNameX(), this.getLayerNameY(), this.getMaxLayerNameWidth(), this.font.lineHeight);
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
                    if (this.layerWidget.editor.canMoveLayerUp(this.element)) {
                        if (FancyMenu.getOptions().playUiClickSounds.getValue()) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        this.layerWidget.editor.history.saveSnapshot();
                        if (!this.element.isSelected()) this.layerWidget.editor.deselectAllElements();
                        this.element.setSelected(true);
                        this.layerWidget.editor.moveLayerUp(this.element);
                        this.layerWidget.getAllWidgetsExceptThis().forEach(widget -> widget.editorElementOrderChanged(this.element, true));
                        MainThreadTaskExecutor.executeInMainThread(() -> this.layerWidget.updateList(true), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                    }
                } else if (this.isMoveDownButtonHovered()) {
                    if (this.layerWidget.editor.canMoveLayerDown(this.element)) {
                        if (FancyMenu.getOptions().playUiClickSounds.getValue()) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        this.layerWidget.editor.history.saveSnapshot();
                        if (!this.element.isSelected()) this.layerWidget.editor.deselectAllElements();
                        this.element.setSelected(true);
                        this.layerWidget.editor.moveLayerDown(this.element);
                        this.layerWidget.getAllWidgetsExceptThis().forEach(widget -> widget.editorElementOrderChanged(this.element, false));
                        MainThreadTaskExecutor.executeInMainThread(() -> this.layerWidget.updateList(true), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                    }
                } else {
                    if (FancyMenu.getOptions().playUiClickSounds.getValue()) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    if (!Screen.hasControlDown()) this.layerWidget.editor.deselectAllElements();
                    this.element.setSelected(!this.element.isSelected());
                    if (this.isLayerNameHovered()) {
                        long now = System.currentTimeMillis();
                        if ((this.lastLeftClick + 400) > now) {
                            this.startEditingLayerName();
                        }
                        this.lastLeftClick = now;
                    }
                }
            }
            if (button == 1) {
                if (!this.element.isSelected()) this.layerWidget.editor.deselectAllElements();
                this.element.setSelected(true);
                MainThreadTaskExecutor.executeInMainThread(() -> this.layerWidget.editor.openElementContextMenuAtMouseIfPossible(), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            }
        }

    }

    public static class VanillaLayerElementEntry extends ScrollAreaEntry {

        protected static final ResourceLocation MOVE_TO_TOP_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/layout_editor/widgets/layers/move_top.png");
        protected static final ResourceLocation MOVE_BEHIND_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/layout_editor/widgets/layers/move_bottom.png");

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
        public void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            this.moveTopBottomButtonHovered = this.isMoveTopBottomButtonHovered(mouseX, mouseY);

            RenderSystem.enableBlend();

            ResourceLocation loc = this.layerWidget.editor.layout.renderElementsBehindVanilla ? MOVE_BEHIND_TEXTURE : MOVE_TO_TOP_TEXTURE;
            blitF(graphics, loc, this.x, this.y, 0.0F, 0.0F, this.getButtonWidth(), this.getButtonHeight(), this.getButtonWidth(), this.getButtonHeight(), UIBase.getUIColorTheme().ui_texture_color.getColorInt());

            this.layerWidget.enableComponentScissor(graphics, (int)(this.x + this.getButtonWidth() + 1f), (int) this.y, (int) (this.getWidth() - this.getButtonWidth() - 4f), (int) this.getHeight(), true);
            UIBase.drawElementLabel(graphics, this.font, Component.translatable("fancymenu.editor.widgets.layers.vanilla_elements").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt())), (int)(this.getX() + this.getButtonWidth() + 3f), (int)(this.getY() + (this.getHeight() / 2f) - (this.font.lineHeight / 2f)));
            this.layerWidget.disableComponentScissor(graphics);

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
        public void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            RenderSystem.enableBlend();
            fillF(graphics, this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), UIBase.getUIColorTheme().element_border_color_normal.getColorInt());
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
