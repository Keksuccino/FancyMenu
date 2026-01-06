package de.keksuccino.fancymenu.util.rendering.ui.dialog;

import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPScreen;

public class DialogBody extends PiPScreen {

    // - Use ExtendedButton for Cancel, Accept, Okay buttons (there are localizations for these already in en_us) (buttons should have UIBase.defaultWidgetSkin() applied
    // - Screen should have no label like "Warning", "Info", etc. (only message), because label gets added as title of the PiPWindow instead
    // - Screen should show an icon on the left side (centered on Y axis) for "Info", "Warning", "Error" depending on the dialog style (icons are available with all styles)
    // - Dialog message should be a Component rendered to the right side of the icon
    // - Dialog message line wrapping with TextFormattingUtils.lineWrapComponents()
    // - Screen should use FancyMenu's default screen style, like ConfirmationScreen (uses UIBase.getTheme() to be theme-able
    // - The Dialog should have a nullable callback (boolean) that gets called on Cancel, Accept, Okay

    @Override
    protected void init() {
        // init buttons and other UI stuff here (buttons should get added as renderableWidgets)
    }

    @Override
    public void onWindowClosedExternally() {
        // counts as cancel -> add logic here
    }

}
