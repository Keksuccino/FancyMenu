package de.keksuccino.fancymenu.api.background;

//Unfinished
public abstract class MenuBackgroundContainer {

    private final String actionIdentifier;

    public MenuBackgroundContainer(String uniqueActionIdentifier) {
        this.actionIdentifier = uniqueActionIdentifier;
    }

    public String getIdentifier() {
        return this.actionIdentifier;
    }

}
