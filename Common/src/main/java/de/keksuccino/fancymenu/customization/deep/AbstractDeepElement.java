package de.keksuccino.fancymenu.customization.deep;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;

public abstract class AbstractDeepElement extends AbstractElement {

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

}
