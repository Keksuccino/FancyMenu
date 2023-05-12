package de.keksuccino.fancymenu.customization.element;

import com.google.common.collect.Lists;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.placeholder.v2.PlaceholderParser;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builder to construct {@link AbstractElement} instances. Every element type needs its own {@link ElementBuilder}.<br>
 * Needs to get registered to the {@link ElementRegistry}.
 */
@SuppressWarnings("all")
public abstract class ElementBuilder<E extends AbstractElement, L extends AbstractEditorElement> {

    private static final Logger LOGGER = LogManager.getLogger();

    private final String identifier;
    private final List<String> alternativeIdentifiers = new ArrayList<>();

    public ElementBuilder(@NotNull String uniqueElementIdentifier) {
        this(uniqueElementIdentifier, (String[]) null);
    }

    public ElementBuilder(@NotNull String uniqueElementIdentifier, @Nullable String... alternativeIdentifiers) {
        this.identifier = uniqueElementIdentifier;
        if (alternativeIdentifiers != null) {
            this.alternativeIdentifiers.addAll(Arrays.asList(alternativeIdentifiers));
        }
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
     * {@link ElementBuilder#deserializeElementInternal(SerializedElement)},
     * so just deserialize the <b>custom</b> properties of the element type here.
     */
    public abstract E deserializeElement(@NotNull SerializedElement serializedElement);

    /**
     * Only for internal use. Don't touch this if you don't know what you're doing!
     */
    @Nullable
    public E deserializeElementInternal(@NotNull SerializedElement serializedElement) {

        try {

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
            String delay = serializedElement.getEntryValue("appearance_delay");
            //Compatibility layer for old appearance delay format
            if (delay == null) {
                String da = serializedElement.getEntryValue("delayappearance");
                if ((da != null) && da.equalsIgnoreCase("true")) {
                    delay = AbstractElement.AppearanceDelay.FIRST_TIME.name;
                }
                String dae = serializedElement.getEntryValue("delayappearanceeverytime");
                if ((dae != null) && dae.equalsIgnoreCase("true")) {
                    delay = AbstractElement.AppearanceDelay.EVERY_TIME.name;
                }
            }
            if (delay != null) {
                AbstractElement.AppearanceDelay appearanceDelay = AbstractElement.AppearanceDelay.getByName(delay);
                if (appearanceDelay != null) {
                    element.appearanceDelay = appearanceDelay;
                }
            }
            String delaySec = serializedElement.getEntryValue("appearance_delay_seconds");
            if (delaySec == null) {
                delaySec = serializedElement.getEntryValue("delayappearanceseconds");
            }
            if ((delaySec != null) && MathUtils.isFloat(delaySec)) {
                element.appearanceDelayInSeconds = Float.parseFloat(delaySec);
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

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to deserialize element: " + this.getIdentifier());
            ex.printStackTrace();
        }

        return null;

    }

    /**
     * Will serialize the given {@link AbstractElement} instance to the given {@link SerializedElement}.<br>
     * All base properties like width, height, x, y, orientation, etc. are handled by
     * {@link ElementBuilder#serializeElementInternal(AbstractElement)},
     *  so just serialize the <b>custom</b> properties of the element type here.<br>
     *  Return the {@link SerializedElement} instance at the end.
     */
    protected abstract SerializedElement serializeElement(@NotNull E element, @NotNull SerializedElement serializeTo);

    /**
     * Only for internal use. Don't touch this if you don't know what you're doing!
     */
    @Nullable
    public SerializedElement serializeElementInternal(@NotNull AbstractElement element) {

        try {

            SerializedElement sec = removeReservedPropertyKeys(this.serializeElement((E) element, new SerializedElement()));

            //sec.addEntry("action", "custom_layout_element:" + element.builder.getIdentifier());
            sec.addEntry("element_type", element.builder.getIdentifier());
            sec.addEntry("instance_identifier", element.getInstanceIdentifier());

            sec.addEntry("appearance_delay", element.appearanceDelay.name);
            sec.addEntry("appearance_delay_seconds", "" + element.appearanceDelayInSeconds);
            sec.addEntry("fade_in", "" + element.fadeIn);
            sec.addEntry("fade_in_speed", "" + element.fadeInSpeed);

            sec.addEntry("anchor_point", (element.anchorPoint != null) ? element.anchorPoint.getName() : ElementAnchorPoints.TOP_LEFT.getName());
            if ((element.anchorPoint == ElementAnchorPoints.ELEMENT) && (element.anchorPointElementIdentifier != null)) {
                sec.addEntry("anchor_point_element", element.anchorPointElementIdentifier);
            }

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
            if (element.appearanceDelay == null) {
                element.appearanceDelay = AbstractElement.AppearanceDelay.NO_DELAY;
            }
            sec.addEntry("x", "" + element.baseX);
            sec.addEntry("y", "" + element.baseY);
            sec.addEntry("width", "" + element.getWidth());
            sec.addEntry("height", "" + element.getHeight());
            sec.addEntry("stretch_x", "" + element.stretchX);
            sec.addEntry("stretch_y", "" + element.stretchY);

            element.loadingRequirementContainer.serializeContainerToExistingPropertiesSection(sec);

            return sec;

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to serialize element: " + this.getIdentifier());
            ex.printStackTrace();
        }

        return null;

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
    public abstract Component getDisplayName(@Nullable AbstractElement element);

    /**
     * Returns the description of this element type. Used in the {@link LayoutEditorScreen}.
     */
    @Nullable
    public abstract Component[] getDescription(@Nullable AbstractElement element);

    @NotNull
    public String getIdentifier() {
        return this.identifier;
    }

    @NotNull
    public List<String> getAlternativeIdentifiers() {
        return new ArrayList<>(this.alternativeIdentifiers);
    }

    private static SerializedElement removeReservedPropertyKeys(SerializedElement serializedElement) {
        List<String> reserved = Lists.newArrayList(
                "action",
                "element_type",
                "actionid",
                "instance_identifier",
                "button_identifier"
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