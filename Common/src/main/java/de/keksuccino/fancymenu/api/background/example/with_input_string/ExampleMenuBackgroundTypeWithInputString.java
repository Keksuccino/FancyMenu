package de.keksuccino.fancymenu.api.background.example.with_input_string;

import de.keksuccino.fancymenu.api.background.MenuBackground;
import de.keksuccino.fancymenu.api.background.MenuBackgroundType;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.elements.BackgroundOptionsPopup;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.elements.ChooseFilePopup;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;

import java.util.ArrayList;
import java.util.List;

//This is a background type that creates backgrounds out of user string inputs.
//It displays a "Choose File" button in the background options of the layout editor, so the user can choose a file for a background.
//This file path (input string) is then used to create an instance of the background.
public class ExampleMenuBackgroundTypeWithInputString extends MenuBackgroundType {

    public ExampleMenuBackgroundTypeWithInputString() {
        //This identifier needs to be UNIQUE! It is not possible to register multiple types with the same identifier.
        super("example_type_input_string");
    }

    @Override
    public void loadBackgrounds() {
        //Empty because background instances get created on-the-fly via createInstanceFromInputString()
    }

    //You don't really have much space for the display name, so try to choose a short one ond explain the type further in the description.
    @Override
    public String getDisplayName() {
        return "Example Type w/ Input";
    }

    //Gets displayed when hovering over the type switcher in the background options menu in the layout editor.
    //This is great for telling users everything important about your background type!
    @Override
    public List<String> getDescription() {
        List<String> l = new ArrayList<>();
        l.add("This is an example type");
        l.add("that uses input strings.");
        l.add("You can choose an image that");
        l.add("then gets displayed as background.");
        return l;
    }

    @Override
    public boolean needsInputString() {
        //Return true to set this background type to the "input string mode".
        //This means it will call the createInstanceFromInputString() method to create instances of your background,
        //instead of getting it from the loaded background instances.
        return true;
    }

    //In input string mode, this will get called whenever a new background instance is needed.
    //Is called when opening a menu or when clicking on the input string button in the background options menu of the layout editor.
    @Override
    public MenuBackground createInstanceFromInputString(String inputString) {
        //Return a new background instance from the inputString.
        return new ExampleMenuBackgroundForInputString(this, inputString);
    }

    //This gets called when the input string button in the background options is pressed by the user.
    //You can basically do everything here.
    @Override
    public void onInputStringButtonPress(LayoutEditorScreen handler, BackgroundOptionsPopup optionsPopup) {

        //This is a file chooser popup to choose the image for the background.
        ChooseFilePopup cf = new ChooseFilePopup((filePath) -> {
            if (filePath != null) {
                //Always create a snapshot before changing the custom background fields!
                handler.history.saveSnapshot(handler.history.createSnapshot());
                //Always reset all backgrounds before setting a new one!
                optionsPopup.resetBackgrounds();
                //Always set the raw input string (file path in this case) to the input string field!
                handler.customMenuBackgroundInputString = filePath;
                //Create a new instance of your background and set it to the custom background field!
                handler.customMenuBackground = this.createInstanceFromInputString(filePath);
            }
            //This will open the parent popup again after choosing an image.
            PopupHandler.displayPopup(optionsPopup);
        }, "jpg", "jpeg", "png");
        if ((handler.customMenuBackgroundInputString != null)) {
            cf.setText(handler.customMenuBackgroundInputString);
        }
        //Open the file chooser popup.
        PopupHandler.displayPopup(cf);

    }

    //The button label of the input string button in the background options.
    @Override
    public String inputStringButtonLabel() {
        return "Choose File";
    }

    //A tooltip that gets displayed when hovering over the input string button in the background options menu of the layout editor.
    @Override
    public List<String> inputStringButtonTooltip() {
        List<String> l = new ArrayList<>();
        l.add("This is a button tooltip");
        l.add("for the 'Choose File' button.");
        return l;
    }

}
