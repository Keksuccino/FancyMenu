package de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.HideableElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.custombutton.ButtonEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.AnchorPointOverlay;
import de.keksuccino.fancymenu.customization.layout.editor.ChooseAnimationScreen;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.ObjectUtils;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableSlider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.List;

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

        super.init();

        this.rightClickMenu.removeEntry("manage_actions");
        this.rightClickMenu.removeEntry("button_separator_1");

        if (this.isCopyrightButton()) {
            this.rightClickMenu.removeEntry("button_separator_2");
            this.rightClickMenu.removeEntry("edit_label");
            this.rightClickMenu.removeEntry("edit_hover_label");
        }

        if (this.getElement().getWidget() != null) {

            this.rightClickMenu.addClickableEntryAfter("copy_id", "copy_vanilla_widget_locator", Components.translatable("fancymenu.helper.editor.items.vanilla_button.copy_locator"), (menu, entry) ->
                    {
                        Minecraft.getInstance().keyboardHandler.setClipboard(((VanillaWidgetElement)this.element).widgetMeta.getLocator());
                        menu.closeMenu();
                    })
                    .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.vanilla_button.copy_locator.desc")))
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

                    ContextMenu normalSliderBackMenu = new ContextMenu();
                    setBackMenu.addSubMenuEntry("set_normal_slider_background", Components.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.slider.normal"), normalSliderBackMenu)
                            .setStackable(true);

                    this.addImageResourceChooserContextMenuEntryTo(normalSliderBackMenu, "normal_slider_background_texture",
                            VanillaWidgetEditorElement.class,
                            null,
                            consumes -> consumes.getElement().sliderBackgroundTextureNormal,
                            (buttonEditorElement, iTextureResourceSupplier) -> {
                                buttonEditorElement.getElement().sliderBackgroundTextureNormal = iTextureResourceSupplier;
                                buttonEditorElement.getElement().sliderBackgroundAnimationNormal = null;
                            }, Components.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.type.image"), false, null, true, true, true);

                    normalSliderBackMenu.addClickableEntry("normal_slider_background_animation", Components.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.type.animation"), (menu, entry) -> {
                        List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof VanillaWidgetEditorElement));
                        String preSelectedAnimation = null;
                        List<String> allAnimations = ObjectUtils.getOfAll(String.class, selectedElements, consumes -> ((VanillaWidgetElement)consumes.element).sliderBackgroundAnimationNormal);
                        if (!allAnimations.isEmpty() && ListUtils.allInListEqual(allAnimations)) {
                            preSelectedAnimation = allAnimations.get(0);
                        }
                        ChooseAnimationScreen s = new ChooseAnimationScreen(preSelectedAnimation, (call) -> {
                            if (call != null) {
                                this.editor.history.saveSnapshot();
                                for (AbstractEditorElement e : selectedElements) {
                                    ((VanillaWidgetElement)e.element).sliderBackgroundAnimationNormal = call;
                                    ((VanillaWidgetElement)e.element).sliderBackgroundTextureNormal = null;
                                }
                            }
                            Minecraft.getInstance().setScreen(this.editor);
                        });
                        Minecraft.getInstance().setScreen(s);
                    }).setStackable(true);

                    normalSliderBackMenu.addSeparatorEntry("separator_1").setStackable(true);

                    normalSliderBackMenu.addClickableEntry("reset_normal_slider_background", Components.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.reset"), (menu, entry) -> {
                        this.editor.history.saveSnapshot();
                        List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof VanillaWidgetEditorElement));
                        for (AbstractEditorElement e : selectedElements) {
                            ((VanillaWidgetElement)e.element).sliderBackgroundTextureNormal = null;
                            ((VanillaWidgetElement)e.element).sliderBackgroundAnimationNormal = null;
                        }
                    }).setStackable(true);

                    ContextMenu highlightedSliderBackMenu = new ContextMenu();
                    setBackMenu.addSubMenuEntry("set_highlighted_slider_background", Components.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.slider.highlighted"), highlightedSliderBackMenu)
                            .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.buttons.buttonbackground.slider.highlighted.desc")))
                            .setStackable(true);

                    this.addImageResourceChooserContextMenuEntryTo(highlightedSliderBackMenu, "highlighted_slider_background_texture",
                            VanillaWidgetEditorElement.class,
                            null,
                            consumes -> consumes.getElement().sliderBackgroundTextureHighlighted,
                            (buttonEditorElement, iTextureResourceSupplier) -> {
                                buttonEditorElement.getElement().sliderBackgroundTextureHighlighted = iTextureResourceSupplier;
                                buttonEditorElement.getElement().sliderBackgroundAnimationHighlighted = null;
                            }, Components.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.type.image"), false, null, true, true, true);

                    highlightedSliderBackMenu.addClickableEntry("highlighted_slider_background_animation", Components.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.type.animation"), (menu, entry) -> {
                        List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof VanillaWidgetEditorElement));
                        String preSelectedAnimation = null;
                        List<String> allAnimations = ObjectUtils.getOfAll(String.class, selectedElements, consumes -> ((VanillaWidgetElement)consumes.element).sliderBackgroundAnimationHighlighted);
                        if (!allAnimations.isEmpty() && ListUtils.allInListEqual(allAnimations)) {
                            preSelectedAnimation = allAnimations.get(0);
                        }
                        ChooseAnimationScreen s = new ChooseAnimationScreen(preSelectedAnimation, (call) -> {
                            if (call != null) {
                                this.editor.history.saveSnapshot();
                                for (AbstractEditorElement e : selectedElements) {
                                    ((VanillaWidgetElement)e.element).sliderBackgroundAnimationHighlighted = call;
                                    ((VanillaWidgetElement)e.element).sliderBackgroundTextureHighlighted = null;
                                }
                            }
                            Minecraft.getInstance().setScreen(this.editor);
                        });
                        Minecraft.getInstance().setScreen(s);
                    }).setStackable(true);

                    highlightedSliderBackMenu.addSeparatorEntry("separator_1").setStackable(true);

                    highlightedSliderBackMenu.addClickableEntry("reset_highlighted_slider_background", Components.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.reset"), (menu, entry) -> {
                        this.editor.history.saveSnapshot();
                        List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof VanillaWidgetEditorElement));
                        for (AbstractEditorElement e : selectedElements) {
                            ((VanillaWidgetElement)e.element).sliderBackgroundTextureHighlighted = null;
                            ((VanillaWidgetElement)e.element).sliderBackgroundAnimationHighlighted = null;
                        }
                    }).setStackable(true);

                }
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
    protected void renderDraggingNotAllowedOverlay(PoseStack pose) {
        if ((this.element.anchorPoint == ElementAnchorPoints.VANILLA) && (this.renderMovingNotAllowedTime >= System.currentTimeMillis()) && !this.topLeftDisplay.hasLine("vanilla_button_dragging_not_allowed")) {
            this.topLeftDisplay.addLine("vanilla_button_dragging_not_allowed", () -> Components.translatable("fancymenu.elements.vanilla_button.dragging_not_allowed"));
        }
        if ((this.renderMovingNotAllowedTime < System.currentTimeMillis()) && this.topLeftDisplay.hasLine("vanilla_button_dragging_not_allowed")) {
            this.topLeftDisplay.removeLine("vanilla_button_dragging_not_allowed");
        }
        //Display "unable to move" warning for Copyright button
        if (this.isCopyrightButton() && (this.renderMovingNotAllowedTime >= System.currentTimeMillis()) && !this.topLeftDisplay.hasLine("vanilla_button_copyright_unable_to_move")) {
            this.topLeftDisplay.addLine("vanilla_button_copyright_unable_to_move", () -> Components.translatable("fancymenu.elements.vanilla_button.copyright.unable_to_move"));
        }
        if ((this.renderMovingNotAllowedTime < System.currentTimeMillis()) && this.topLeftDisplay.hasLine("vanilla_button_copyright_unable_to_move")) {
            this.topLeftDisplay.removeLine("vanilla_button_copyright_unable_to_move");
        }
        super.renderDraggingNotAllowedOverlay(pose);
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isHidden()) return false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.isHidden()) return false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double $$3, double $$4) {
        if (this.isHidden()) return false;
        return super.mouseDragged(mouseX, mouseY, button, $$3, $$4);
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
        String compId = ((VanillaWidgetElement)this.element).widgetMeta.getUniversalIdentifier();
        return ((compId != null) && compId.equals("mc_titlescreen_copyright_button"));
    }

    public VanillaWidgetElement getElement() {
        return (VanillaWidgetElement) this.element;
    }

}
