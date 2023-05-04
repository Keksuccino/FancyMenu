package de.keksuccino.fancymenu.customization.backend.element;

import com.google.common.collect.Lists;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.LayoutEditorScreen;
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
     * This will deserialize the given {@link SerializedElement}.<br>
     * Since {@link SerializedElement}s get deserialized when constructing an {@link AbstractElement}, simply return a new {@link AbstractElement} instance with the given {@link SerializedElement} as parameter.
     */
    @NotNull
    public abstract E deserializeElement(@NotNull SerializedElement serializedElement);

    /**
     * Will serialize the given {@link AbstractElement} instance.<br>
     * This is used to save the element to a layout file.
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
        sec.addEntry("actionid", element.getInstanceIdentifier());
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
        sec.addEntry("x", "" + element.rawX);
        sec.addEntry("y", "" + element.rawY);
        sec.addEntry("orientation", element.orientation);
        if (element.orientation.equals("element") && (element.orientationElementIdentifier != null)) {
            sec.addEntry("orientation_element", element.orientationElementIdentifier);
        }
        if (element.stretchX) {
            sec.addEntry("x", "0");
            sec.addEntry("width", "%guiwidth%");
        } else {
            sec.addEntry("x", "" + element.rawX);
            sec.addEntry("width", "" + element.getWidth());
        }
        if (element.stretchY) {
            sec.addEntry("y", "0");
            sec.addEntry("height", "%guiheight%");
        } else {
            sec.addEntry("y", "" + element.rawY);
            sec.addEntry("height", "" + element.getHeight());
        }
        element.loadingRequirementContainer.serializeContainerToExistingPropertiesSection(sec);

        return sec;

    }

    /**
     * This will wrap the given {@link AbstractElement} into an {@link AbstractEditorElement} to use it in the {@link LayoutEditorScreen}.
     */
    @NotNull
    public abstract L buildEditorElementInstance(@NotNull E element, @NotNull LayoutEditorScreen editor);

    /**
     * Returns the display name of this element type. Used in the {@link LayoutEditorScreen}.
     */
    @NotNull
    public abstract String getDisplayName();

    /**
     * Returns the description of this element type. Used in the {@link LayoutEditorScreen}.
     */
    @Nullable
    public abstract String[] getDescription();

    @NotNull
    public String getIdentifier() {
        return this.identifier;
    }

    private static SerializedElement removeReservedPropertyKeys(SerializedElement serializedElement) {
        List<String> reserved = Lists.newArrayList("action", "element_type");
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
