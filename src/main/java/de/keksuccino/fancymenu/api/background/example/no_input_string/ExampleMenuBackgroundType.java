package de.keksuccino.fancymenu.api.background.example.no_input_string;

import de.keksuccino.fancymenu.api.background.MenuBackground;
import de.keksuccino.fancymenu.api.background.MenuBackgroundType;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

//This is a background type that doesn't use an input string.
//It doesn't create background instances on-the-fly, but has a set of loaded instances to choose from.
public class ExampleMenuBackgroundType extends MenuBackgroundType {

    public ExampleMenuBackgroundType() {
        //This identifier needs to be UNIQUE! It is not possible to register multiple types with the same identifier.
        super("example_type_no_input_string");
    }

    //In normal mode (when not using input strings), this is the place you register all of the background instances for the type.
    //This method gets called on game start and when the reload button gets pressed.
    //
    //For example, when you want to use a directory where users can put their background properties, like FancyMenu does for animations and slideshows,
    //load all backgrounds from this directory here.
    @Override
    public void loadBackgrounds() {

        //Clear the loaded backgrounds first.
        this.backgrounds.clear();

        //Background identifiers need to be UNIQUE when registering backgrounds!
        //It is not possible to register multiple backgrounds with the same identifier!
        //
        //In this case I just directly register a set of background instances,
        //but you can also use this (for example) to load stored background properties, etc.
        this.addBackground(new ExampleMenuBackground("green_background", this, new Color(117, 245, 66)));
        this.addBackground(new ExampleMenuBackground("blue_background", this, new Color(66, 126, 245)));
        this.addBackground(new ExampleMenuBackground("orange_background", this, new Color(245, 164, 51)));

    }

    //You don't really have much space for the display name, so try to choose a short one ond explain the type further in the description.
    @Override
    public String getDisplayName() {
        return "Example Type No Input";
    }

    //Gets displayed when hovering over the type switcher in the background options menu in the layout editor.
    //This is great for telling users everything important about your background type!
    @Override
    public List<String> getDescription() {
        List<String> l = new ArrayList<>();
        l.add("This background type has a set");
        l.add("of backgrounds to choose from.");
        l.add("It doesn't use input strings.");
        return l;
    }

    @Override
    public boolean needsInputString() {
        //Return false, because we don't use input strings for this type.
        return false;
    }

    @Override
    public MenuBackground createInstanceFromInputString(String inputString) {
        //Return null, because we don't use input strings for this type.
        return null;
    }
}
