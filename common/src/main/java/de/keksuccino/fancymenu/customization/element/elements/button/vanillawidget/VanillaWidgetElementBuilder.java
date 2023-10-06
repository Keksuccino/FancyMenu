package de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.IElementStacker;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.button.custombutton.ButtonElement;
import de.keksuccino.fancymenu.customization.element.elements.button.custombutton.ButtonElementBuilder;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.SerializationUtils;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VanillaWidgetElementBuilder extends ButtonElementBuilder implements IElementStacker<VanillaWidgetElement> {

    //TODO Wenn Slider background, dann default handle render broken (default texture muss nach custom render neu gesetzt werden)
    //TODO Slider inactive handle texture entfernen (slider haben keine inactive handle texture)

    private static final Logger LOGGER = LogManager.getLogger();

    public static final VanillaWidgetElementBuilder INSTANCE = new VanillaWidgetElementBuilder();

    @Override
    public @NotNull String getIdentifier() {
        return "vanilla_button";
    }

    @Override
    public @NotNull VanillaWidgetElement buildDefaultInstance() {
        VanillaWidgetElement element = new VanillaWidgetElement(this);
        element.anchorPoint = ElementAnchorPoints.VANILLA;
        return element;
    }

    @Override
    public @Nullable SerializedElement serializeElementInternal(@NotNull AbstractElement elementAbstract) {

        try {

            VanillaWidgetElement element = (VanillaWidgetElement) elementAbstract;
            SerializedElement serialized = super.serializeElementInternal(element);
            if (serialized != null) {

                serialized.setType("vanilla_button");

                serialized.putProperty("is_hidden", "" + element.vanillaButtonHidden);
                serialized.putProperty("automated_button_clicks", "" + element.automatedButtonClicks);

                if (element.sliderBackgroundTextureNormal != null) {
                    serialized.putProperty("slider_background_texture_normal", element.sliderBackgroundTextureNormal.getShortPath());
                }
                if (element.sliderBackgroundTextureHighlighted != null) {
                    serialized.putProperty("slider_background_texture_highlighted", element.sliderBackgroundTextureHighlighted.getShortPath());
                }
                serialized.putProperty("slider_background_animation_normal", element.sliderBackgroundAnimationNormal);
                serialized.putProperty("slider_background_animation_highlighted", element.sliderBackgroundAnimationHighlighted);

                return serialized;

            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;

    }

    @Override
    public @NotNull VanillaWidgetElement deserializeElement(@NotNull SerializedElement serialized) {

        VanillaWidgetElement element = (VanillaWidgetElement) super.deserializeElement(serialized);

        String hidden = serialized.getValue("is_hidden");
        if ((hidden != null) && hidden.equalsIgnoreCase("true")) {
            element.vanillaButtonHidden = true;
        }

        String automatedClicks = serialized.getValue("automated_button_clicks");
        if ((automatedClicks != null) && MathUtils.isInteger(automatedClicks)) {
            element.automatedButtonClicks = Integer.parseInt(automatedClicks);
        }

        element.sliderBackgroundTextureNormal = SerializationUtils.deserializeResourceFile(serialized.getValue("slider_background_texture_normal"));
        element.sliderBackgroundTextureHighlighted = SerializationUtils.deserializeResourceFile(serialized.getValue("slider_background_texture_highlighted"));
        element.sliderBackgroundAnimationNormal = serialized.getValue("slider_background_animation_normal");
        element.sliderBackgroundAnimationHighlighted = serialized.getValue("slider_background_animation_highlighted");

        return element;

    }

    @Override
    public @Nullable VanillaWidgetElement deserializeElementInternal(@NotNull SerializedElement serialized) {
        return (VanillaWidgetElement) super.deserializeElementInternal(serialized);
    }

    @Override
    public @NotNull VanillaWidgetEditorElement wrapIntoEditorElement(@NotNull ButtonElement element, @NotNull LayoutEditorScreen editor) {
        return new VanillaWidgetEditorElement(element, editor);
    }

    //Stacking Step 3
    @Override
    public void stackElements(@NotNull VanillaWidgetElement e, @NotNull VanillaWidgetElement stack) {

        //Don't stack cached button stuff, just the plain customization part + identifier

        //VanillaButtonElement stuff
        if (e.widgetMeta != null) {
            stack.setVanillaWidget(e.widgetMeta, false);
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
    public @Nullable VanillaWidgetElement stackElementsInternal(AbstractElement stack, AbstractElement... elements) {
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
