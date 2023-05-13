package de.keksuccino.fancymenu.customization.element.elements.button.vanilla;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.button.custom.ButtonEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import org.jetbrains.annotations.NotNull;

public class VanillaButtonEditorElement extends ButtonEditorElement {

    private ElementAnchorPoint lastAnchorPoint = null;

    public VanillaButtonEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
        this.settings.setOrderable(false);
        this.settings.setCopyable(false);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        this.tick();
        super.render(pose, mouseX, mouseY, partial);
    }

    protected void tick() {
        if ((this.lastAnchorPoint == null) || (this.lastAnchorPoint != this.element.anchorPoint)) {
            if (this.element.anchorPoint == ElementAnchorPoints.VANILLA) {
                this.settings.setDragable(false);
                this.settings.setResizeable(false);
                this.settings.setAdvancedSizingSupported(false);
                this.settings.setAdvancedPositioningSupported(false);
            } else {
                this.settings.setDragable(true);
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

}
