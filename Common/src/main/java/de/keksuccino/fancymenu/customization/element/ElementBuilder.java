package de.keksuccino.fancymenu.customization.element;

import com.google.common.collect.Lists;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.loadingrequirement.v2.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.placeholder.v2.PlaceholderParser;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder to construct {@link AbstractElement} instances. Every element type needs its own {@link ElementBuilder}.<br>
 * Needs to get registered to the {@link ElementRegistry}.
 */
public abstract class ElementBuilder<E extends AbstractElement, L extends AbstractEditorElement> {

    private static final Logger LOGGER = LogManager.getLogger();

    private final String identifier;

    public ElementBuilder(@NotNull String uniqueElementIdentifier) {
        this.identifier = uniqueElementIdentifier;
    }

    /**
     * This will construct a new instance of this element type with everything set to valid defaults.<br>
     * Is used in the {@link LayoutEditorScreen} when adding a new instance of this element type.
     */
    @NotNull
    public abstract E buildDefaultInstance();

    /**
     * Will deserialize the given {@link SerializedElement}.<br>
     * All base properties like width, height, x, y, orientation, etc. are handled by
     * {@link ElementBuilder#deserializeElementWithBaseProperties(SerializedElement)},
     * so just deserialize the <b>custom</b> properties of the element type here.
     */
    @NotNull
    public abstract E deserializeElement(@NotNull SerializedElement serializedElement);

    /**
     * Only for internal use. Don't touch this if you don't know what you're doing!
     */
    public E deserializeElementWithBaseProperties(@NotNull SerializedElement serializedElement) {

        E element = deserializeElement(serializedElement);

        element.instanceIdentifier = serializedElement.getEntryValue("instance_identifier");
        if (element.instanceIdentifier == null) {
            element.instanceIdentifier = serializedElement.getEntryValue("actionid");
        }
        if (element.instanceIdentifier == null) {
            element.instanceIdentifier = ScreenCustomization.generateUniqueIdentifier();
        }

        String fi = serializedElement.getEntryValue("fadein");
        if ((fi != null) && fi.equalsIgnoreCase("true")) {
            element.fadeIn = true;
        }
        String fis = serializedElement.getEntryValue("fadeinspeed");
        if ((fis != null) && MathUtils.isFloat(fis)) {
            element.fadeInSpeed = Float.parseFloat(fis);
        }
        String da = serializedElement.getEntryValue("delayappearance");
        if ((da != null) && da.equalsIgnoreCase("true")) {
            element.delayAppearance = true;
        }
        String legacyDa = serializedElement.getEntryValue("hideforseconds");
        if (legacyDa != null) {
            element.delayAppearance = true;
        }
        String dae = serializedElement.getEntryValue("delayappearanceeverytime");
        if ((dae != null) && dae.equalsIgnoreCase("true")) {
            element.delayAppearanceEverytime = true;
        }
        String legacyDae = serializedElement.getEntryValue("delayonlyfirsttime");
        if ((legacyDae != null) && legacyDae.equalsIgnoreCase("false")) {
            element.delayAppearanceEverytime = true;
        }
        String das = serializedElement.getEntryValue("delayappearanceseconds");
        if ((das != null) && MathUtils.isFloat(das)) {
            element.delayAppearanceSec = Float.parseFloat(das);
        }
        if ((legacyDa != null) && MathUtils.isFloat(legacyDa)) {
            element.delayAppearanceSec = Float.parseFloat(legacyDa);
        }

        String x = serializedElement.getEntryValue("x");
        String y = serializedElement.getEntryValue("y");
        if (x != null) {
            x = PlaceholderParser.replacePlaceholders(x);
            if (MathUtils.isInteger(x)) {
                element.baseX = Integer.parseInt(x);
            }
        }
        if (y != null) {
            y = PlaceholderParser.replacePlaceholders(y);
            if (MathUtils.isInteger(y)) {
                element.baseY = Integer.parseInt(y);
            }
        }

        String anchor = serializedElement.getEntryValue("anchor_point");
        if (anchor == null) {
            anchor = serializedElement.getEntryValue("orientation");
        }
        if (anchor != null) {
            element.anchorPoint = ElementAnchorPoints.getAnchorPointByName(anchor);
            if (element.anchorPoint == null) {
                element.anchorPoint = ElementAnchorPoints.TOP_LEFT;
            }
        }

        String anchorElement = serializedElement.getEntryValue("anchor_point_element");
        if (anchorElement == null) {
            anchorElement = serializedElement.getEntryValue("orientation_element");
        }
        if (anchorElement != null) {
            element.anchorPointElementIdentifier = anchorElement;
        }

        String w = serializedElement.getEntryValue("width");
        if (w != null) {
            if (w.equals("%guiwidth%")) {
                element.stretchX = true;
            } else {
                if (MathUtils.isInteger(w)) {
                    element.width = Integer.parseInt(w);
                }
                if (element.width < 0) {
                    element.width = 0;
                }
            }
        }

        String h = serializedElement.getEntryValue("height");
        if (h != null) {
            if (h.equals("%guiheight%")) {
                element.stretchY = true;
            } else {
                if (MathUtils.isInteger(h)) {
                    element.height = Integer.parseInt(h);
                }
                if (element.height < 0) {
                    element.height = 0;
                }
            }
        }

        String stretchXString = serializedElement.getEntryValue("stretch_x");
        if ((stretchXString != null) && stretchXString.equals("true")) {
            element.stretchX = true;
        }

        String stretchYString = serializedElement.getEntryValue("stretch_y");
        if ((stretchYString != null) && stretchYString.equals("true")) {
            element.stretchY = true;
        }

        element.advancedWidth = serializedElement.getEntryValue("advanced_width");
        element.advancedHeight = serializedElement.getEntryValue("advanced_height");
        element.advancedX = serializedElement.getEntryValue("advanced_posx");
        element.advancedY = serializedElement.getEntryValue("advanced_posy");

        element.loadingRequirementContainer = LoadingRequirementContainer.deserializeRequirementContainer(serializedElement);

        return element;

    }

    /**
     * Will serialize the given {@link AbstractElement} instance.<br>
     * All base properties like width, height, x, y, orientation, etc. are handled by
     * {@link ElementBuilder#serializeElementWithBaseProperties(AbstractElement)},
     *  so just serialize the <b>custom</b> properties of the element type here.
     */
    @NotNull
    protected abstract SerializedElement serializeElement(@NotNull E element);

    /**
     * Only for internal use. Don't touch this if you don't know what you're doing!
     */
    @NotNull
    public SerializedElement serializeElementWithBaseProperties(@NotNull E element) {

        SerializedElement sec = removeReservedPropertyKeys(this.serializeElement(element));

        //sec.addEntry("action", "custom_layout_element:" + element.builder.getIdentifier());
        sec.addEntry("element_type", element.builder.getIdentifier());
        sec.addEntry("instance_identifier", element.getInstanceIdentifier());
        if (element.advancedX != null) {
            sec.addEntry("advanced_posx", element.advancedX);
        }
        if (element.advancedY != null) {
            sec.addEntry("advanced_posy", element.advancedY);
        }
        if (element.advancedWidth != null) {
            sec.addEntry("advanced_width", element.advancedWidth);
        }
        if (element.advancedHeight != null) {
            sec.addEntry("advanced_height", element.advancedHeight);
        }
        if (element.delayAppearance) {
            sec.addEntry("delayappearance", "true");
            sec.addEntry("delayappearanceeverytime", "" + element.delayAppearanceEverytime);
            sec.addEntry("delayappearanceseconds", "" + element.delayAppearanceSec);
            if (element.fadeIn) {
                sec.addEntry("fadein", "true");
                sec.addEntry("fadeinspeed", "" + element.fadeInSpeed);
            }
        }
        sec.addEntry("anchor_point", (element.anchorPoint != null) ? element.anchorPoint.getName() : ElementAnchorPoints.TOP_LEFT.getName());
        if ((element.anchorPoint == ElementAnchorPoints.ELEMENT) && (element.anchorPointElementIdentifier != null)) {
            sec.addEntry("anchor_point_element", element.anchorPointElementIdentifier);
        }
        sec.addEntry("x", "" + element.baseX);
        sec.addEntry("y", "" + element.baseY);
        sec.addEntry("width", "" + element.getWidth());
        sec.addEntry("height", "" + element.getHeight());
        sec.addEntry("stretch_x", "" + element.stretchX);
        sec.addEntry("stretch_y", "" + element.stretchY);
        element.loadingRequirementContainer.serializeContainerToExistingPropertiesSection(sec);

        return sec;

    }

    /**
     * This will wrap the given {@link AbstractElement} into an {@link AbstractEditorElement} to use it in the {@link LayoutEditorScreen}.<br>
     * {@link AbstractEditorElement}s are basically a UI layer for {@link AbstractElement}s with everything needed to customize them, like the right-click context menu of elements, the element border with grabbers to resize the element and so on.
     */
    @NotNull
    public abstract L wrapIntoEditorElement(@NotNull E element, @NotNull LayoutEditorScreen editor);

    /**
     * Returns the display name of this element type. Used in the {@link LayoutEditorScreen}.
     */
    @NotNull
    public abstract Component getDisplayName();

    /**
     * Returns the description of this element type. Used in the {@link LayoutEditorScreen}.
     */
    @Nullable
    public abstract Component[] getDescription();

    @NotNull
    public String getIdentifier() {
        return this.identifier;
    }

    private static SerializedElement removeReservedPropertyKeys(SerializedElement serializedElement) {
        List<String> reserved = Lists.newArrayList(
                "action",
                "element_type",
                "actionid",
                "instance_identifier"
        );
        List<String> removed = new ArrayList<>();
        for (String s : reserved) {
            if (serializedElement.hasEntry(s)) {
                serializedElement.removeEntry(s);
                removed.add(s);
            }
        }
        if (!removed.isEmpty()) {
            StringBuilder keys = new StringBuilder();
            for (String s : removed) {
                if (keys.length() > 0) {
                    keys.append(", ");
                }
                keys.append(s);
            }
            LOGGER.error("[FANCYMENU] Failed to add properties to serialized element! Keys reserved by the system: " + keys);
        }
        return serializedElement;
    }

}
