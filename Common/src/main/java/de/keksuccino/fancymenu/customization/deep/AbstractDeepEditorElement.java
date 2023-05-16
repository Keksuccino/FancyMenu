package de.keksuccino.fancymenu.customization.deep;

import de.keksuccino.fancymenu.customization.element.IHideableElement;
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
