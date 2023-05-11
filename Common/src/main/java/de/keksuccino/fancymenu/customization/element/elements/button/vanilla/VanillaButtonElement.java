package de.keksuccino.fancymenu.customization.element.elements.button.vanilla;

import de.keksuccino.fancymenu.customization.button.ButtonData;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.IActionExecutorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.custom.ButtonEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.custom.ButtonElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VanillaButtonElement extends ButtonElement implements IActionExecutorElement {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    public String vanillaButtonIdentifier;
    @Nullable
    public ButtonData buttonData;
    public boolean vanillaHidden = false;

    public VanillaButtonElement(ElementBuilder<ButtonElement, ButtonEditorElement> builder) {
        super(builder);
        this.renderButton = false;
    }

    @Override
    protected void tick() {

        super.tick();

        //TODO loading requirements handlen

        //TODO besseres visibility handling (damit vanilla visibility changes nicht überschrieben werden)

        if (!this.vanillaHidden) {
            this.button.visible = this.visible;
        }
        if (this.vanillaHidden) {
            this.button.visible = false;
        }

    }

}
