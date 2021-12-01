package de.keksuccino.fancymenu.api.buttonaction.example;

import de.keksuccino.fancymenu.api.buttonaction.ButtonActionContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.SaveLevelScreen;
import net.minecraft.text.LiteralText;

public class ExampleButtonActionContainerWithoutValue extends ButtonActionContainer {

    public ExampleButtonActionContainerWithoutValue() {
        //The action identifier needs to be unique, so just use your username or something similar as prefix
        super("very_unique_action_identifier");
    }

    //The name of your action. Should be lowercase and without any spaces.
    @Override
    public String getAction() {
        return "customaction";
    }

    //If the custom action has a value or not
    @Override
    public boolean hasValue() {
        return false;
    }

    //Gets called when a button with this custom action is getting clicked
    @Override
    public void execute(String value) {

        //This will open a new instance of the dirt message screen, when a button with this custom action is getting clicked
        MinecraftClient.getInstance().setScreen(new SaveLevelScreen(new LiteralText("This is a useless message.")));

    }

    //The description of the action
    @Override
    public String getActionDescription() {
        return "Open the dirt message screen.";
    }

    //This action has no value, so I just return NULL here
    @Override
    public String getValueDescription() {
        return null;
    }

    //Same thing. No value. Return NULL.
    @Override
    public String getValueExample() {
        return null;
    }

}
