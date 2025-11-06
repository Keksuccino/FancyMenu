package de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.HideableElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.button.custombutton.ButtonEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.AnchorPointOverlay;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableSlider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class VanillaWidgetEditorElement extends ButtonEditorElement implements HideableElement {

    private ElementAnchorPoint lastAnchorPoint = null;

    public VanillaWidgetEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
        this.settings.setOrderable(false);
        this.settings.setCopyable(false);
        this.settings.setHideInsteadOfDestroy(true);
        this.settings.setVanillaAnchorPointAllowed(true);
    }

    @Override
    public void init() {

        this.showTemplateOptions = false;

        super.init();

        this.rightClickMenu.removeEntry("manage_actions");
        this.rightClickMenu.removeEntry("button_separator_1");

        if (this.isCopyrightButton()) {
            this.rightClickMenu.removeEntry("button_separator_2");
            this.rightClickMenu.removeEntry("edit_label");
            this.rightClickMenu.removeEntry("edit_hover_label");
        }

        if (this.getElement().getWidget() != null) {

            this.rightClickMenu.addClickableEntryAfter("copy_id", "copy_vanilla_widget_locator", Component.translatable("fancymenu.elements.vanilla_button.copy_locator"), (menu, entry) ->
                    {
                        Minecraft.getInstance().keyboardHandler.setClipboard(((VanillaWidgetElement)this.element).widgetMeta.getLocator());
                        menu.closeMenu();
                    })
                    .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.vanilla_button.copy_locator.desc")))
                    .setIcon(ContextMenu.IconFactory.getIcon("notes"));

        }

        if (this.getElement().getWidget() instanceof CustomizableSlider) {

            ContextMenu.ContextMenuEntry<?> buttonBackgroundMenuEntry = this.rightClickMenu.getEntry("button_background");
            if (buttonBackgroundMenuEntry instanceof ContextMenu.SubMenuContextMenuEntry s1) {
                ContextMenu buttonBackgroundMenu = s1.getSubContextMenu();
                ContextMenu.ContextMenuEntry<?> setBackMenuEntry = buttonBackgroundMenu.getEntry("set_background");
                if (setBackMenuEntry instanceof ContextMenu.SubMenuContextMenuEntry s2) {

                    ContextMenu setBackMenu = s2.getSubContextMenu();

                    setBackMenu.addSeparatorEntry("separator_before_slider_background_entries");

                    this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "normal_slider_background_texture",
                            VanillaWidgetEditorElement.class,
                            null,
                            consumes -> consumes.getElement().sliderBackgroundTextureNormal,
                            (buttonEditorElement, iTextureResourceSupplier) -> {
                                buttonEditorElement.getElement().sliderBackgroundTextureNormal = iTextureResourceSupplier;
                            }, Component.translatable("fancymenu.elements.buttons.buttonbackground.slider.normal"), true, null, true, true, true);

                    this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "highlighted_slider_background_texture",
                                    VanillaWidgetEditorElement.class,
                                    null,
                                    consumes -> consumes.getElement().sliderBackgroundTextureHighlighted,
                                    (buttonEditorElement, iTextureResourceSupplier) -> {
                                        buttonEditorElement.getElement().sliderBackgroundTextureHighlighted = iTextureResourceSupplier;
                                    }, Component.translatable("fancymenu.elements.buttons.buttonbackground.slider.highlighted"), true, null, true, true, true)
                            .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.buttons.buttonbackground.slider.highlighted.desc")));

                }

                buttonBackgroundMenu.addSeparatorEntry("separator_before_nine_slider_slider_handle_settings");

                this.addToggleContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_slider_handle", VanillaWidgetEditorElement.class,
                        consumes -> consumes.getElement().nineSliceSliderHandle,
                        (buttonEditorElement, aBoolean) -> buttonEditorElement.getElement().nineSliceSliderHandle = aBoolean,
                        "fancymenu.elements.slider.v2.handle.textures.nine_slice");

                this.addIntegerInputContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_slider_handle_border_x", VanillaWidgetEditorElement.class,
                        consumes -> consumes.getElement().nineSliceSliderHandleBorderX,
                        (buttonEditorElement, integer) -> buttonEditorElement.getElement().nineSliceSliderHandleBorderX = integer,
                        Component.translatable("fancymenu.elements.slider.v2.handle.textures.nine_slice.border_x"), true, 5, null, null);

                this.addIntegerInputContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_slider_handle_border_y", VanillaWidgetEditorElement.class,
                        consumes -> consumes.getElement().nineSliceSliderHandleBorderY,
                        (buttonEditorElement, integer) -> buttonEditorElement.getElement().nineSliceSliderHandleBorderY = integer,
                        Component.translatable("fancymenu.elements.slider.v2.handle.textures.nine_slice.border_y"), true, 5, null, null);

            }

        }

    }

    @Override
    protected void tick() {
        super.tick();
        if ((this.lastAnchorPoint == null) || (this.lastAnchorPoint != this.element.anchorPoint)) {
            if (this.element.anchorPoint == ElementAnchorPoints.VANILLA) {
                this.settings.setMovable(false);
                this.settings.setResizeable(false);
            } else {
                this.settings.setMovable(true);
                this.settings.setResizeable(true);
            }
        }
        this.lastAnchorPoint = this.element.anchorPoint;
        //Restrict customization options for the Copyright button
        if (this.isCopyrightButton() && (this.settings.isDestroyable() || this.settings.isResizeable())) {
            this.settings.setDestroyable(false);
            this.settings.setResizeable(false);
            this.settings.setAdvancedSizingSupported(false);
            this.settings.setAdvancedPositioningSupported(false);
            this.settings.setDelayable(false);
            this.settings.setStretchable(false);
            this.settings.setLoadingRequirementsEnabled(false);
        }
        //Make it impossible to move the Copyright button out-of-screen
        this.handleCopyrightButtonPositionRestrictions();
    }

    @Override
    protected void renderDraggingNotAllowedOverlay(GuiGraphics graphics) {
        if ((this.element.anchorPoint == ElementAnchorPoints.VANILLA) && (this.renderMovingNotAllowedTime >= System.currentTimeMillis()) && !this.topLeftDisplay.hasLine("vanilla_button_dragging_not_allowed")) {
            this.topLeftDisplay.addLine("vanilla_button_dragging_not_allowed", () -> Component.translatable("fancymenu.elements.vanilla_button.dragging_not_allowed"));
        }
        if ((this.renderMovingNotAllowedTime < System.currentTimeMillis()) && this.topLeftDisplay.hasLine("vanilla_button_dragging_not_allowed")) {
            this.topLeftDisplay.removeLine("vanilla_button_dragging_not_allowed");
        }
        //Display "unable to move" warning for Copyright button
        if (this.isCopyrightButton() && (this.renderMovingNotAllowedTime >= System.currentTimeMillis()) && !this.topLeftDisplay.hasLine("vanilla_button_copyright_unable_to_move")) {
            this.topLeftDisplay.addLine("vanilla_button_copyright_unable_to_move", () -> Component.translatable("fancymenu.elements.vanilla_button.copyright.unable_to_move"));
        }
        if ((this.renderMovingNotAllowedTime < System.currentTimeMillis()) && this.topLeftDisplay.hasLine("vanilla_button_copyright_unable_to_move")) {
            this.topLeftDisplay.removeLine("vanilla_button_copyright_unable_to_move");
        }
        super.renderDraggingNotAllowedOverlay(graphics);
    }

    protected void handleCopyrightButtonPositionRestrictions() {
        if (this.isCopyrightButton()) {
            if ((this.getX() + this.getWidth()) > this.editor.width) {
                this.element.posOffsetX -= ((this.getX() + this.getWidth()) - this.editor.width);
                this.renderMovingNotAllowedTime = System.currentTimeMillis() + 800;
            }
            if (this.getX() < 0) {
                this.element.posOffsetX += Math.abs(this.getX());
                this.renderMovingNotAllowedTime = System.currentTimeMillis() + 800;
            }
            if ((this.getY() + this.getHeight()) > this.editor.height) {
                this.element.posOffsetY -= ((this.getY() + this.getHeight()) - this.editor.height);
                this.renderMovingNotAllowedTime = System.currentTimeMillis() + 800;
            }
            if (this.getY() < 0) {
                this.element.posOffsetY += Math.abs(this.getY());
                this.renderMovingNotAllowedTime = System.currentTimeMillis() + 800;
            }
        }
    }

    @Override
    public void setAnchorPointViaOverlay(AnchorPointOverlay.AnchorPointArea anchor, int mouseX, int mouseY) {
        if (this.element.anchorPoint == ElementAnchorPoints.VANILLA) return;
        this.lastAnchorPoint = anchor.anchorPoint;
        super.setAnchorPointViaOverlay(anchor, mouseX, mouseY);
    }

    @Override
    public void setSelected(boolean selected) {
        if (this.isHidden()) return;
        super.setSelected(selected);
    }

    @Override
    public boolean isHovered() {
        if (this.isHidden()) return false;
        return super.isHovered();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (this.isHidden()) return false;
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.isHidden()) return false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double $$3, double $$4) {
        if (this.isHidden()) return false;
        return super.mouseDragged(event, $$3, $$4);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (this.isHidden()) return false;
        return super.isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean isHidden() {
        return ((HideableElement)this.element).isHidden();
    }

    @Override
    public void setHidden(boolean hidden) {
        ((HideableElement)this.element).setHidden(hidden);
        if (this.isHidden()) {
            this.resetElementStates();
        }
    }

    public boolean isCopyrightButton() {
        return ((VanillaWidgetElement)this.element).isCopyrightButton();
    }

    public VanillaWidgetElement getElement() {
        return (VanillaWidgetElement) this.element;
    }

}
