package de.keksuccino.fancymenu.customization.element.elements.button.vanilla;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.IHideableElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.button.custom.ButtonEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class VanillaButtonEditorElement extends ButtonEditorElement implements IHideableElement {

    private ElementAnchorPoint lastAnchorPoint = null;

    public VanillaButtonEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
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

        if (this.getButtonElement().getButton() instanceof AbstractButton) {

            this.rightClickMenu.addClickableEntryAfter("copy_id", "copy_vanilla_button_locator", Component.translatable("fancymenu.helper.editor.items.vanilla_button.copy_locator"), (menu, entry) ->
                    {
                        Minecraft.getInstance().keyboardHandler.setClipboard(((VanillaButtonElement)this.element).widgetMeta.getLocator());
                        menu.closeMenu();
                    })
                    .setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.vanilla_button.copy_locator.desc")));

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
        //Make the Copyright button un-deletable
        if (this.isCopyrightButton() && (this.settings.isDestroyable() || this.settings.isResizeable())) {
            this.settings.setDestroyable(false);
            this.settings.setResizeable(false);
        }
        //Make it impossible to move the Copyright button out-of-screen
        this.handleCopyrightButtonPositionRestrictions();
    }

    @Override
    protected void renderDraggingNotAllowedOverlay(PoseStack pose) {
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
        super.renderDraggingNotAllowedOverlay(pose);
    }

    protected void handleCopyrightButtonPositionRestrictions() {
        if (this.isCopyrightButton()) {
            if ((this.getX() + this.getWidth()) > this.editor.width) {
                this.element.baseX -= ((this.getX() + this.getWidth()) - this.editor.width);
                this.renderMovingNotAllowedTime = System.currentTimeMillis() + 800;
            }
            if (this.getX() < 0) {
                this.element.baseX += Math.abs(this.getX());
                this.renderMovingNotAllowedTime = System.currentTimeMillis() + 800;
            }
            if ((this.getY() + this.getHeight()) > this.editor.height) {
                this.element.baseY -= ((this.getY() + this.getHeight()) - this.editor.height);
                this.renderMovingNotAllowedTime = System.currentTimeMillis() + 800;
            }
            if (this.getY() < 0) {
                this.element.baseY += Math.abs(this.getY());
                this.renderMovingNotAllowedTime = System.currentTimeMillis() + 800;
            }
        }
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
        return ((IHideableElement)this.element).isHidden();
    }

    @Override
    public void setHidden(boolean hidden) {
        ((IHideableElement)this.element).setHidden(hidden);
        if (this.isHidden()) {
            this.resetElementStates();
        }
    }

    public boolean isCopyrightButton() {
        String compId = ((VanillaButtonElement)this.element).widgetMeta.getCompatibilityIdentifier();
        return ((compId != null) && compId.equals("button_compatibility_id:mc_titlescreen_copyright_button"));
    }

}
