package de.keksuccino.fancymenu.api.item;

import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.konkrete.properties.PropertiesSection;

/**
 * A customization item container.<br><br>
 *
 * Needs to be registered to the {@link CustomizationItemRegistry}.
 */
public abstract class CustomizationItemContainer {

    private final String itemIdentifier;

    public CustomizationItemContainer(String uniqueItemIdentifier) {
        this.itemIdentifier = uniqueItemIdentifier;
    }

    /**
     * Returns a new default instance of your item.<br>
     * Is called when you add a new instance of this item in the editor, so if you want to set some default stuff for every new item, do it here.<br><br>
     *
     * <b>Needs to be a default (uncustomized) instance.</b>
     *
     * @return A new default item instance.
     */
    public abstract CustomizationItem constructDefaultItemInstance();

    /**
     * Returns a new customized instance of your item.<br>
     * Is called when a layout gets applied to a menu, to get a customized instance of this item, if this item is part of the layout.<br><br>
     *
     * So in detail, this means that when a layout contains a serialized instance of this item in form of a {@link PropertiesSection},
     * this {@link PropertiesSection} will be used to to create a new (real) instance of the item, to render it in the target menu.<br>
     * Items get serialized to {@link PropertiesSection}s in the layout editor, when a layout gets saved.<br><br>
     *
     * You need to set all variables of the {@link PropertiesSection} to the new customized item instance here.
     *
     * @param serializedItem The serialized instance of the customized item. Contains all variables that need to be set to a customized item instance.
     * @return A new customized item instance.
     */
    public abstract CustomizationItem constructCustomizedItemInstance(PropertiesSection serializedItem);

    /**
     * Returns a new instance of the {@link LayoutEditorElement} of this item.<br>
     * This {@link LayoutEditorElement} allows the user to customize the item in the editor and
     * will serialize the item to a {@link PropertiesSection} when the layout gets saved.<br><br>
     *
     * The {@link LayoutEditorElement} returned here should be an element specifically made for the type of this item.<br>
     * You need to add some things to the element, so it can handle the item correctly.
     *
     * @param item The item instance that needs to be set to the {@link LayoutEditorElement} that will be returned by this method.
     * @param handler The layout editor instance that will handle the element. Needs to be set to the {@link LayoutEditorElement} that will be returned by this method.
     * @return A new {@link LayoutEditorElement} instance.
     */
    public abstract LayoutEditorElement constructEditorElementInstance(CustomizationItem item, LayoutEditorScreen handler);

    /**
     * Returns the display name of this item. Used in the layout editor.<br><br>
     *
     * You can localize the name here.
     */
    public abstract String getDisplayName();

    /**
     * Returns the description of this item. Visible when hovering over the button to add a new item of this type to the editor.<br>
     * Every string in the returned string array is one line of the description tooltip.<br><br>
     *
     * You can localize the description here.
     */
    public abstract String[] getDescription();

    public String getIdentifier() {
        return this.itemIdentifier;
    }

}
