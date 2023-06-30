package de.keksuccino.fancymenu.customization.deep;

import de.keksuccino.fancymenu.customization.element.IHideableElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.editor.EditorElementSettings;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractDeepEditorElement extends AbstractEditorElement implements IHideableElement {

    public AbstractDeepEditorElement(@NotNull AbstractDeepElement element, @NotNull LayoutEditorScreen editor, @Nullable EditorElementSettings settings) {
        super(element, editor, settings);
        this.setDefaultSettings();
    }

    public AbstractDeepEditorElement(@NotNull AbstractDeepElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
        this.setDefaultSettings();
    }

    protected void setDefaultSettings() {
        this.settings.setStretchable(false);
        this.settings.setOrderable(false);
        this.settings.setCopyable(false);
        this.settings.setResizeable(false);
        this.settings.setMovable(false);
        this.settings.setAnchorPointChangeable(false);
        this.settings.setAdvancedPositioningSupported(false);
        this.settings.setAdvancedSizingSupported(false);
        this.settings.setHideInsteadOfDestroy(true);
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

}
