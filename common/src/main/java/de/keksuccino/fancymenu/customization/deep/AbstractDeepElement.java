package de.keksuccino.fancymenu.customization.deep;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.HideableElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractDeepElement extends AbstractElement implements HideableElement {

    public boolean deepElementHidden = false;

    public AbstractDeepElement(DeepElementBuilder<?,?,?> builder) {
        super(builder);
        this.anchorPoint = ElementAnchorPoints.VANILLA;
    }

    @Override
    public boolean shouldRender() {
        if (!this.isDeepElementVisible()) {
            return false;
        }
        return super.shouldRender();
    }

    @Override
    public @NotNull String getInstanceIdentifier() {
        return "deep:" + this.builder.getIdentifier();
    }

    public boolean isDeepElementVisible() {
        if (!this.loadingRequirementsMet()) {
            return false;
        }
        if (this.deepElementHidden) {
            return false;
        }
        if (!this.visible) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isHidden() {
        return this.deepElementHidden;
    }

    @Override
    public void setHidden(boolean hidden) {
        this.deepElementHidden = hidden;
    }

}
