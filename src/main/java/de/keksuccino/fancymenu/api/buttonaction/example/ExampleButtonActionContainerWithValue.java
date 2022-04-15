package de.keksuccino.fancymenu.api.buttonaction.example;

import de.keksuccino.fancymenu.api.buttonaction.ButtonActionContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.network.chat.TextComponent;

public class ExampleButtonActionContainerWithValue extends ButtonActionContainer {

    public ExampleButtonActionContainerWithValue() {
        //The action identifier needs to be unique, so just use your username or something similar as prefix
        super("super_unique_action_identifier");
    }

    //The name of your action. Should be lowercase and without any spaces.
    @Override
    public String getAction() {
        return "customaction2";
    }

    //If the custom action has a value or not
    @Override
    public boolean hasValue() {
        return true;
    }

    //Gets called when a button with this custom action is getting clicked
    @Override
    public void execute(String value) {

        //This will open a new instance of the dirt message screen, when a button with this custom action is getting clicked
        //and will show the action value as message
        if (value != null) {
            Minecraft.getInstance().setScreen(new GenericDirtMessageScreen(new TextComponent(value)));
        }

    }

    //The description of the action
    @Override
    public String getActionDescription() {
        return "Show custom text in a dirt message screen.";
    }

    //The action has a value, so I return a simple and short value description here.
    //This is actually more like a value type description.
    @Override
    public String getValueDescription() {
        return "Display Text";
    }

    //That's an example of how the action value should look like.
    @Override
    public String getValueExample() {
        //Well, it's just a simple String, so what should be the example here >.<
        return "cool text to display";
    }

}
