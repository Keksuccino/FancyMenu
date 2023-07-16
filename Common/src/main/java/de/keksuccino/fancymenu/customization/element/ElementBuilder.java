package de.keksuccino.fancymenu.customization.element;

import com.google.common.collect.Lists;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
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
@SuppressWarnings("all")
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
     * {@link ElementBuilder#deserializeElementInternal(SerializedElement)},
     * so just deserialize the <b>custom</b> properties of the element type here.
     */
    public abstract E deserializeElement(@NotNull SerializedElement serialized);

    /**
     * Only for internal use. Don't touch this if you don't know what you're doing!
     */
    @Nullable
    public E deserializeElementInternal(@NotNull SerializedElement serialized) {

        try {

            E element = deserializeElement(serialized);

            String id = serialized.getValue("instance_identifier");
            if (id == null) id = serialized.getValue("actionid");
            if (id == null) id = ScreenCustomization.generateUniqueIdentifier();
            if (id.equals("null")) {
                id = ScreenCustomization.generateUniqueIdentifier();
                LOGGER.warn("[FANCYMENU] Automatically corrected broken element identifier from 'null' to '" + id + "'! This could break parts of its parent layout!");
            }
            element.setInstanceIdentifier(id);

            String fi = serialized.getValue("fade_in");
            if (fi == null) {
                fi = serialized.getValue("fadein");
            }
            if ((fi != null) && fi.equalsIgnoreCase("true")) {
                element.fadeIn = true;
            }
            String fis = serialized.getValue("fade_in_speed");
            if (fis == null) {
                fis = serialized.getValue("fadeinspeed");
            }
            if ((fis != null) && MathUtils.isFloat(fis)) {
                element.fadeInSpeed = Float.parseFloat(fis);
            }
            String delay = serialized.getValue("appearance_delay");
            //Compatibility layer for old appearance delay format
            if (delay == null) {
                String da = serialized.getValue("delayappearance");
                if ((da != null) && da.equalsIgnoreCase("true")) {
                    delay = AbstractElement.AppearanceDelay.FIRST_TIME.name;
                }
                String dae = serialized.getValue("delayappearanceeverytime");
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
            String delaySec = serialized.getValue("appearance_delay_seconds");
            if (delaySec == null) {
                delaySec = serialized.getValue("delayappearanceseconds");
            }
            if ((delaySec != null) && MathUtils.isFloat(delaySec)) {
                element.appearanceDelayInSeconds = Float.parseFloat(delaySec);
            }

            String x = serialized.getValue("x");
            String y = serialized.getValue("y");
            if (x != null) {
                x = PlaceholderParser.replacePlaceholders(x);
                if (MathUtils.isInteger(x)) {
                    element.posOffsetX = Integer.parseInt(x);
                }
            }
            if (y != null) {
                y = PlaceholderParser.replacePlaceholders(y);
                if (MathUtils.isInteger(y)) {
                    element.posOffsetY = Integer.parseInt(y);
                }
            }

            String anchor = serialized.getValue("anchor_point");
            if (anchor == null) {
                anchor = serialized.getValue("orientation");
            }
            if (anchor != null) {
                element.anchorPoint = ElementAnchorPoints.getAnchorPointByName(anchor);
                if (element.anchorPoint == null) {
                    element.anchorPoint = ElementAnchorPoints.TOP_LEFT;
                }
            }

            String anchorElement = serialized.getValue("anchor_point_element");
            if (anchorElement == null) {
                anchorElement = serialized.getValue("orientation_element");
            }
            if (anchorElement != null) {
                element.anchorPointElementIdentifier = anchorElement;
            }

            String w = serialized.getValue("width");
            if (w != null) {
                if (w.equals("%guiwidth%")) {
                    element.stretchX = true;
                } else {
                    if (MathUtils.isInteger(w)) {
                        element.baseWidth = Integer.parseInt(w);
                    }
                    if (element.baseWidth < 0) {
                        element.baseWidth = 0;
                    }
                }
            }

            String h = serialized.getValue("height");
            if (h != null) {
                if (h.equals("%guiheight%")) {
                    element.stretchY = true;
                } else {
                    if (MathUtils.isInteger(h)) {
                        element.baseHeight = Integer.parseInt(h);
                    }
                    if (element.baseHeight < 0) {
                        element.baseHeight = 0;
                    }
                }
            }

            String stretchXString = serialized.getValue("stretch_x");
            if ((stretchXString != null) && stretchXString.equals("true")) {
                element.stretchX = true;
            }

            String stretchYString = serialized.getValue("stretch_y");
            if ((stretchYString != null) && stretchYString.equals("true")) {
                element.stretchY = true;
            }

            element.advancedWidth = serialized.getValue("advanced_width");
            element.advancedHeight = serialized.getValue("advanced_height");
            element.advancedX = serialized.getValue("advanced_posx");
            element.advancedY = serialized.getValue("advanced_posy");

            String stayOnScreen = serialized.getValue("stay_on_screen");
            //Setting this to false if null should set it to false for all legacy elements, so old layouts don't break
            if ((stayOnScreen == null) || stayOnScreen.equals("false")) {
                element.stayOnScreen = false;
            }

            element.loadingRequirementContainer = LoadingRequirementContainer.deserializeRequirementContainer(serialized);

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

            sec.putProperty("element_type", element.builder.getIdentifier());
            sec.putProperty("instance_identifier", element.getInstanceIdentifier());

            sec.putProperty("appearance_delay", element.appearanceDelay.name);
            sec.putProperty("appearance_delay_seconds", "" + element.appearanceDelayInSeconds);
            sec.putProperty("fade_in", "" + element.fadeIn);
            sec.putProperty("fade_in_speed", "" + element.fadeInSpeed);

            sec.putProperty("anchor_point", (element.anchorPoint != null) ? element.anchorPoint.getName() : ElementAnchorPoints.TOP_LEFT.getName());
            if ((element.anchorPoint == ElementAnchorPoints.ELEMENT) && (element.anchorPointElementIdentifier != null)) {
                sec.putProperty("anchor_point_element", element.anchorPointElementIdentifier);
            }

            if (element.advancedX != null) {
                sec.putProperty("advanced_posx", element.advancedX);
            }
            if (element.advancedY != null) {
                sec.putProperty("advanced_posy", element.advancedY);
            }
            if (element.advancedWidth != null) {
                sec.putProperty("advanced_width", element.advancedWidth);
            }
            if (element.advancedHeight != null) {
                sec.putProperty("advanced_height", element.advancedHeight);
            }
            if (element.appearanceDelay == null) {
                element.appearanceDelay = AbstractElement.AppearanceDelay.NO_DELAY;
            }
            sec.putProperty("x", "" + element.posOffsetX);
            sec.putProperty("y", "" + element.posOffsetY);
            sec.putProperty("width", "" + element.getAbsoluteWidth());
            sec.putProperty("height", "" + element.getAbsoluteHeight());
            sec.putProperty("stretch_x", "" + element.stretchX);
            sec.putProperty("stretch_y", "" + element.stretchY);

            sec.putProperty("stay_on_screen", "" + element.stayOnScreen);

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
     * Only for internal use. Don't touch this if you don't know what you're doing!
     */
    @Nullable
    public L wrapIntoEditorElementInternal(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        try {
            return this.wrapIntoEditorElement((E) element, editor);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

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

    private static SerializedElement removeReservedPropertyKeys(SerializedElement serialized) {
        List<String> reserved = Lists.newArrayList(
                "action",
                "element_type",
                "actionid",
                "instance_identifier",
                "button_identifier"
        );
        List<String> removed = new ArrayList<>();
        for (String s : reserved) {
            if (serialized.hasProperty(s)) {
                serialized.removeProperty(s);
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
        return serialized;
    }

    protected static boolean isEditor() {
        return (Minecraft.getInstance().screen instanceof LayoutEditorScreen);
    }

}
