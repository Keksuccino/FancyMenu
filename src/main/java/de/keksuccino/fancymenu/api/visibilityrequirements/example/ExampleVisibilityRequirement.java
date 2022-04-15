package de.keksuccino.fancymenu.api.visibilityrequirements.example;

import de.keksuccino.fancymenu.api.visibilityrequirements.VisibilityRequirement;
import de.keksuccino.konkrete.input.CharacterFilter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

public class ExampleVisibilityRequirement extends VisibilityRequirement {

    public ExampleVisibilityRequirement() {
        //The identifier needs to be unique! It's not possible to register multiple requirements with the same identifier!
        super("example_requirement_no_value");
    }

    @Override
    public boolean hasValue() {
        //We don't need a value in this requirement, so we return false here.
        return false;
    }

    //Here you return if the requirement is met (using the given requirement value if the requirement has one).
    //We don't use a value in this example, so the parameter will be NULL and we can ignore it.
    @Override
    public boolean isRequirementMet(@Nullable String value) {

        //In this example, we just check if the window is in fullscreen mode and if it is, then we return true.
        return Minecraft.getInstance().options.fullscreen;

    }

    //This is the display name of the requirement.
    //You don't have much space for the display name, so try to choose a short one.
    @Override
    public String getDisplayName() {
        return "Example Requirement [No Value]";
    }

    //This is the description of the requirement.
    @Override
    public List<String> getDescription() {
        List<String> l = new ArrayList<>();
        l.add("This is an example requirement");
        l.add("without a value.");
        l.add("It checks if the window is in fullscreen.");
        return l;
    }

    //Since this requirement has no value, just return NULL here.
    @Override
    public String getValueDisplayName() {
        return null;
    }

    //No value, so just return NULL.
    @Override
    public String getValuePreset() {
        return null;
    }

    //You know the drill. No value = return NULL.
    @Override
    public CharacterFilter getValueInputFieldFilter() {
        return null;
    }

}
