package de.keksuccino.fancymenu.api.visibilityrequirements.example;

import de.keksuccino.fancymenu.api.visibilityrequirements.VisibilityRequirement;
import de.keksuccino.konkrete.input.CharacterFilter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ExampleVisibilityRequirementWithValue extends VisibilityRequirement {

    public ExampleVisibilityRequirementWithValue() {
        //The identifier needs to be unique! It's not possible to register multiple requirements with the same identifier!
        super("example_requirement_with_value");
    }

    @Override
    public boolean hasValue() {
        //This requirement needs a value, so we return true here.
        return true;
    }

    //Here you return if the requirement is met (using the given requirement value if the requirement has one).
    //Since this requirement has a value, the value parameter will be the value of a requirement in a layout.
    @Override
    public boolean isRequirementMet( String value) {

        //This requirement has a value that can be any string, but the requirement will only be met if the value is "show_me".
        //We return true if the value is "show_me".
        if (value != null) {
            return value.equalsIgnoreCase("show_me");
        }

        return false;

    }

    //This is the display name of the requirement.
    //You don't have much space for the display name, so try to choose a short one.
    @Override
    public String getDisplayName() {
        return "Example Requirement [With Value]";
    }

    //This is the description of the requirement.
    @Override
    public List<String> getDescription() {
        List<String> l = new ArrayList<>();
        l.add("This is an example requirement");
        l.add("with a value.");
        l.add("It checks if the value is 'show_me'.");
        return l;
    }

    //This is the display name of the VALUE of the requirement.
    //You don't have much space for the display name, so try to choose a short one.
    @Override
    public String getValueDisplayName() {
        return "Example Value Name";
    }

    //This is the content that will be automatically set to the value input field when there is no value already.
    @Override
    public String getValuePreset() {
        return "cool value preset";
    }

    //The character filter of the value input field. Return NULL if you want to allow all characters.
    //Can be used to only allow numbers in the input field, etc.
    @Override
    public CharacterFilter getValueInputFieldFilter() {
        return null;
    }

}
