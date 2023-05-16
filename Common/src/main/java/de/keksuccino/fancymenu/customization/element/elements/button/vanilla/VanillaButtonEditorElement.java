package de.keksuccino.fancymenu.customization.element.elements.button.vanilla;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.IHideableElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.button.custom.ButtonEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class VanillaButtonEditorElement extends ButtonEditorElement implements IHideableElement {

    private ElementAnchorPoint lastAnchorPoint = null;

    public VanillaButtonEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
        this.settings.setOrderable(false);
        this.settings.setCopyable(false);
        this.settings.setHideInsteadOfDestroy(true);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        this.tick();
        super.render(pose, mouseX, mouseY, partial);
    }

    @Override
    protected void renderDraggingNotAllowedOverlay(PoseStack pose) {
        if ((this.renderMovingNotAllowedTime >= System.currentTimeMillis()) && !this.topLeftDisplay.hasLine("vanilla_button_dragging_not_allowed")) {
            this.topLeftDisplay.addLine("vanilla_button_dragging_not_allowed", () -> Component.translatable("fancymenu.elements.vanilla_button.dragging_not_allowed"));
        }
        if ((this.renderMovingNotAllowedTime < System.currentTimeMillis()) && this.topLeftDisplay.hasLine("vanilla_button_dragging_not_allowed")) {
            this.topLeftDisplay.removeLine("vanilla_button_dragging_not_allowed");
        }
        super.renderDraggingNotAllowedOverlay(pose);
    }

    protected void tick() {
        if ((this.lastAnchorPoint == null) || (this.lastAnchorPoint != this.element.anchorPoint)) {
            if (this.element.anchorPoint == ElementAnchorPoints.VANILLA) {
                this.settings.setMovable(false);
                this.settings.setResizeable(false);
                this.settings.setAdvancedSizingSupported(false);
                this.settings.setAdvancedPositioningSupported(false);
            } else {
                this.settings.setMovable(true);
                this.settings.setResizeable(true);
                this.settings.setAdvancedSizingSupported(true);
                this.settings.setAdvancedPositioningSupported(true);
            }
        }
        this.lastAnchorPoint = this.element.anchorPoint;
    }

    @Override
    public void init() {

        super.init();

        //TODO entries adden

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

}
