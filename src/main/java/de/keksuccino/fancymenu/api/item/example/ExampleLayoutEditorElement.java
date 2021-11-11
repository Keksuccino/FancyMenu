package de.keksuccino.fancymenu.api.item.example;

import de.keksuccino.fancymenu.api.item.LayoutEditorElement;
import de.keksuccino.fancymenu.menu.fancy.helper.DynamicValueInputPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.rendering.RenderUtils;

import java.awt.*;

public class ExampleLayoutEditorElement extends LayoutEditorElement {

    public ExampleLayoutEditorElement(ExampleCustomizationItemContainer parentContainer, ExampleCustomizationItem customizationItemInstance, LayoutEditorScreen handler) {
        super(parentContainer, customizationItemInstance, true, handler, true);
    }

    @Override
    public void init() {

        //The superclass adds basic stuff to the right-click context menu, like visibility requirement controls, delete controls, orientation, etc.
        super.init();

        //The 'object' field holds the CustomizationItem instance of this element.
        //Cast it to your own item class, to get and set your own fields.
        ExampleCustomizationItem i = ((ExampleCustomizationItem)this.object);

        //This button will be part of the right-click context menu of the element and is uses to change the background color value of the item.
        AdvancedButton backgroundColorButton = new AdvancedButton(0, 0, 0, 0, "Background Color", (press) -> {
            //This is the basic input popup for text content, used in many parts of FancyMenu.
            FMTextInputPopup pop = new FMTextInputPopup(new Color(0, 0, 0, 0), "Background Color HEX", null, 240, (callback) -> {
                //The callback of popups will be null, when pressing ESC in it to force-close it.
                if (callback != null) {
                    if (!callback.equals(i.backgroundColorString)) {
                        Color c = RenderUtils.getColorFromHexString(callback);
                        if (c != null) {
                            //Create a snapshot before every change, so you can undo the change in the editor (using CTRL + Z)
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                            //Now set the new values to the item instance
                            i.backgroundColorString = callback;
                            i.backgroundColor = c;
                        }
                    }
                }
            });
            //Set the current value as default text of the text input popup
            if (i.backgroundColorString != null) {
                pop.setText(i.backgroundColorString);
            }
            //Open the popup
            PopupHandler.displayPopup(pop);
        });
        backgroundColorButton.setDescription("This is just an example button tooltip.");
        //Add the button to the right-click context menu content
        this.rightclickMenu.addContent(backgroundColorButton);

        //This is the button to change the display text of the item. Will also be part of the right-click context menu.
        AdvancedButton displayTextButton = new AdvancedButton(0, 0, 0, 0, "Display Text", (press) -> {
            //This is also a text input popup, but with placeholder text value support (the little icon at the right side of the input field)
            DynamicValueInputPopup pop = new DynamicValueInputPopup(new Color(0, 0, 0, 0), "Set Display Text", null, 240, (callback) -> {
                if (callback != null) {
                    if (!callback.equals(i.displayText)) {
                        //Again, save a snapshot before changing something!
                        this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        //Setting the new display text value
                        i.displayText = callback;
                    }
                }
            });
            if (i.displayText != null) {
                pop.setText(i.displayText);
            }
            PopupHandler.displayPopup(pop);
        });
        this.rightclickMenu.addContent(displayTextButton);

    }

    @Override
    public SimplePropertiesSection serializeItem() {

        ExampleCustomizationItem i = ((ExampleCustomizationItem)this.object);

        SimplePropertiesSection sec = new SimplePropertiesSection();

        //Add your custom item values here, so they get saved and can later be de-serialized again.
        sec.addEntry("background_color", i.backgroundColorString);
        sec.addEntry("display_text", i.displayText);

        return sec;

    }

}
