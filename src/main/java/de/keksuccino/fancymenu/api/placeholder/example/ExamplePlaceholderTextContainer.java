package de.keksuccino.fancymenu.api.placeholder.example;

import de.keksuccino.fancymenu.api.placeholder.PlaceholderTextContainer;

//This needs to be registered to the PlaceholderTextRegistry at mod init
public class ExamplePlaceholderTextContainer extends PlaceholderTextContainer {

    public ExamplePlaceholderTextContainer() {
        super("example_placeholder_identifier");
    }

    @Override
    public String replacePlaceholders(String rawIn) {

        String placeholder = this.getPlaceholder();
        String realValue = "" + System.currentTimeMillis();

        //Replacing all placeholder occurrences with the real value and returning it
        return rawIn.replace(placeholder, realValue);

    }

    @Override
    public String getPlaceholder() {
        return "%example_placeholder%";
    }

    @Override
    public String getCategory() {
        return "Example Category";
    }

    @Override
    public String getDisplayName() {
        return "Example Placeholder";
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                "This is a multiline",
                "placeholder description."
        };
    }

}
