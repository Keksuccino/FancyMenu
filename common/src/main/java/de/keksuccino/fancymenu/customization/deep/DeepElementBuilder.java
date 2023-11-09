package de.keksuccino.fancymenu.customization.deep;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ElementStacker;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.layout.Layout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DeepElementBuilder<D extends DeepScreenCustomizationLayer, E extends AbstractDeepElement, L extends AbstractDeepEditorElement> extends ElementBuilder<E, L> implements ElementStacker<E> {

    public final D layer;

    public DeepElementBuilder(@NotNull String uniqueElementIdentifier, @NotNull D layer) {
        super(uniqueElementIdentifier);
        this.layer = layer;
    }

    @Override
    public @Nullable E deserializeElementInternal(@NotNull SerializedElement serialized) {

        try {

            E element = super.deserializeElementInternal(serialized);
            if (element != null) {

                String hidden = serialized.getValue("is_hidden");
                if (hidden == null) {
                    hidden = serialized.getValue("hidden");
                }
                if ((hidden != null) && hidden.equals("true")) {
                    element.deepElementHidden = true;
                }

            }
            return element;

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;

    }

    @Override
    public @Nullable SerializedElement serializeElementInternal(@NotNull AbstractElement elementAbstract) {
        try {

            AbstractDeepElement element = (AbstractDeepElement) elementAbstract;
            SerializedElement serialized = super.serializeElementInternal(element);
            if (serialized != null) {

                serialized.setType("deep_element");

                serialized.putProperty("is_hidden", "" + element.deepElementHidden);

                return serialized;

            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * In {@link Layout}s, {@link AbstractDeepElement}s get stacked to one element,
     * because otherwise multiple instances of the same vanilla element would get rendered to the screen.<br>
     * All basic properties get stacked in {@link DeepElementBuilder#stackElementsInternal(AbstractDeepElement...)},
     * so just stack the custom properties of this deep element type here.
     */
    @Override
    @SuppressWarnings("all")
    public abstract void stackElements(E element, E stack);

    /**
     * Only for internal use. Don't touch this if you don't know what you're doing!
     */
    @Override
    public void stackElementsSingleInternal(AbstractElement elementAbstract, AbstractElement stackAbstract) {

        ElementStacker.super.stackElementsSingleInternal(elementAbstract, stackAbstract);

        if ((elementAbstract instanceof AbstractDeepElement e) && (stackAbstract instanceof AbstractDeepElement stack)) {

            //AbstractDeepElement stuff
            if (e.deepElementHidden) {
                stack.deepElementHidden = true;
            }

            //AbstractElement stuff
            if (e.anchorPoint != ElementAnchorPoints.VANILLA) {
                stack.anchorPoint = e.anchorPoint;
            }

        }

    }

    @Override
    public @Nullable E stackElementsInternal(AbstractElement stack, AbstractElement... elements) {
        if (stack != null) {
            stack.anchorPoint = ElementAnchorPoints.VANILLA;
        }
        return ElementStacker.super.stackElementsInternal(stack, elements);
    }

}
