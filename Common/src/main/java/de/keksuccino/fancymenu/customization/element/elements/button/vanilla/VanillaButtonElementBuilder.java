package de.keksuccino.fancymenu.customization.element.elements.button.vanilla;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.IElementStacker;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.button.custom.ButtonElement;
import de.keksuccino.fancymenu.customization.element.elements.button.custom.ButtonElementBuilder;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VanillaButtonElementBuilder extends ButtonElementBuilder implements IElementStacker<VanillaButtonElement> {

    public static final VanillaButtonElementBuilder INSTANCE = new VanillaButtonElementBuilder();

    @Override
    public @NotNull String getIdentifier() {
        return "vanilla_button";
    }

    @Override
    public @NotNull VanillaButtonElement buildDefaultInstance() {
        VanillaButtonElement element = new VanillaButtonElement(this);
        element.anchorPoint = ElementAnchorPoints.VANILLA;
        return element;
    }

    @Override
    public @Nullable SerializedElement serializeElementInternal(@NotNull AbstractElement elementAbstract) {

        try {

            VanillaButtonElement element = (VanillaButtonElement) elementAbstract;
            SerializedElement serialized = super.serializeElementInternal(element);
            if (serialized != null) {

                serialized.setType("vanilla_button");

                serialized.putProperty("is_hidden", "" + element.vanillaButtonHidden);
                serialized.putProperty("automated_button_clicks", "" + element.automatedButtonClicks);

                return serialized;

            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;

    }

    @Override
    public @NotNull VanillaButtonElement deserializeElement(@NotNull SerializedElement serialized) {

        VanillaButtonElement element = (VanillaButtonElement) super.deserializeElement(serialized);

        String hidden = serialized.getValue("is_hidden");
        if ((hidden != null) && hidden.equalsIgnoreCase("true")) {
            element.vanillaButtonHidden = true;
        }

        String automatedClicks = serialized.getValue("automated_button_clicks");
        if ((automatedClicks != null) && MathUtils.isInteger(automatedClicks)) {
            element.automatedButtonClicks = Integer.parseInt(automatedClicks);
        }

        return element;

    }

    @Override
    public @Nullable VanillaButtonElement deserializeElementInternal(@NotNull SerializedElement serialized) {
        return (VanillaButtonElement) super.deserializeElementInternal(serialized);
    }

    @Override
    public @NotNull VanillaButtonEditorElement wrapIntoEditorElement(@NotNull ButtonElement element, @NotNull LayoutEditorScreen editor) {
        return new VanillaButtonEditorElement(element, editor);
    }

    //Stacking Step 3
    @Override
    public void stackElements(@NotNull VanillaButtonElement e, @NotNull VanillaButtonElement stack) {

        //Don't stack cached button stuff, just the plain customization part + identifier

        //VanillaButtonElement stuff
        if (e.widgetMeta != null) {
            stack.setVanillaButton(e.widgetMeta);
        }
        if (e.vanillaButtonHidden) {
            stack.vanillaButtonHidden = true;
        }
        if (e.automatedButtonClicks != 0) {
            stack.automatedButtonClicks = e.automatedButtonClicks;
        }

        //ButtonElement stuff
        if (e.clickSound != null) {
            stack.clickSound = e.clickSound;
        }
        if (e.hoverSound != null) {
            stack.hoverSound = e.hoverSound;
        }
        if (e.label != null) {
            stack.label = e.label;
        }
        if (e.hoverLabel != null) {
            stack.hoverLabel = e.hoverLabel;
        }
        if (e.tooltip != null) {
            stack.tooltip = e.tooltip;
        }
        if (e.backgroundTextureNormal != null) {
            stack.backgroundTextureNormal = e.backgroundTextureNormal;
        }
        if (e.backgroundTextureHover != null) {
            stack.backgroundTextureHover = e.backgroundTextureHover;
        }
        if (e.backgroundTextureInactive != null) {
            stack.backgroundTextureInactive = e.backgroundTextureInactive;
        }
        if (e.backgroundAnimationNormal != null) {
            stack.backgroundAnimationNormal = e.backgroundAnimationNormal;
        }
        if (e.backgroundAnimationHover != null) {
            stack.backgroundAnimationHover = e.backgroundAnimationHover;
        }
        if (e.backgroundAnimationInactive != null) {
            stack.backgroundAnimationInactive = e.backgroundAnimationInactive;
        }
        if (!e.loopBackgroundAnimations) {
            stack.loopBackgroundAnimations = false;
        }
        if (!e.restartBackgroundAnimationsOnHover) {
            stack.restartBackgroundAnimationsOnHover = false;
        }

    }

    //Stacking Step 2
    @Override
    public void stackElementsSingleInternal(AbstractElement e, AbstractElement stack) {

        IElementStacker.super.stackElementsSingleInternal(e, stack);

        //AbstractElement stuff
        if (e.anchorPoint != ElementAnchorPoints.VANILLA) {
            stack.anchorPoint = e.anchorPoint;
        }

    }

    //Stacking Step 1
    @Override
    public @Nullable VanillaButtonElement stackElementsInternal(AbstractElement stack, AbstractElement... elements) {
        if (stack != null) {
            stack.anchorPoint = ElementAnchorPoints.VANILLA;
        }
        return IElementStacker.super.stackElementsInternal(stack, elements);
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return null;
    }

}
