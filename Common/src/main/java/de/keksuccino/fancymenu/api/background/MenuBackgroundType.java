package de.keksuccino.fancymenu.api.background;

import de.keksuccino.fancymenu.customization.frontend.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.elements.BackgroundOptionsPopup;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.elements.ChooseFilePopup;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.localization.Locals;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class MenuBackgroundType {

    private static final Logger LOGGER = LogManager.getLogger();

    private final String typeIdentifier;

    protected Map<String, MenuBackground> backgrounds = new HashMap<>();

    /**
     * A menu background type.<br>
     * An instance of this class holds all {@link MenuBackground} instances related to the type.<br><br>
     *
     * Needs to be registered to the {@link MenuBackgroundTypeRegistry} at mod init.<br><br>
     *
     * @param uniqueTypeIdentifier The <b>unique</b> identifier of the background type.
     */
    public MenuBackgroundType(@Nonnull String uniqueTypeIdentifier) {
        this.typeIdentifier = uniqueTypeIdentifier;
    }

    /**
     * Used to load/initialize or re-load all {@link MenuBackground} instances of this {@link MenuBackgroundType}.<br>
     * This gets called on game start and when the reload button is clicked.<br><br>
     *
     * This is only useful if {@link MenuBackgroundType#needsInputString()} returns {@code false}, because otherwise<br>
     * the background type will not use its loaded instances, but will call {@link MenuBackgroundType#createInstanceFromInputString(String)} instead.<br><br>
     *
     * <b>{@link MenuBackground} instances need to be added to the {@link MenuBackgroundType#backgrounds} map!</b><br><br>
     *
     * So, if you have a folder or something where the user needs to store all {@link MenuBackground}s of this type,<br>
     * load all backgrounds from this folder in this method.<br><br>
     *
     * It's not recommended to do resource-intensive stuff here, this should only be used to "register" the instance,<br>
     * so the type knows about the instance and can work with it. If your {@link MenuBackground}s need time to load, you should do that<br>
     * in another thread and let your {@link MenuBackground} check if it's ready to get rendered.<br><br>
     *
     * If you don't want to reload the {@link MenuBackground}s, just check for the method call and cancel it after the first time.
     */
    public abstract void loadBackgrounds();

    /**
     * Add a background to the loaded backgrounds of this type.
     */
    public void addBackground(MenuBackground background) {
        if (background != null) {
            if (this.backgrounds.containsKey(background.getIdentifier())) {
                LOGGER.warn("[FANCYMENU] Menu background with the identifier '" + background.getIdentifier() + "' is already registered for the '" + this.getIdentifier() + "' background type! Overriding background!");
            }
            this.backgrounds.put(background.getIdentifier(), background);
        }
    }

    /**
     * Remove a previously loaded background from the loaded backgrounds of this type.
     */
    public void removeBackground(String backgroundIdentifier) {
        if (backgroundIdentifier != null) {
            this.backgrounds.remove(backgroundIdentifier);
        }
    }

    /**
     * This method returns all loaded {@link MenuBackground} instances<br>
     * you've previously added to the {@link MenuBackgroundType#backgrounds} map via {@link MenuBackgroundType#loadBackgrounds()}.<br><br>
     *
     * <b>SHOULD NEVER RETURN NULL!</b>
     */
    @Nonnull
    public List<MenuBackground> getBackgrounds() {
        List<MenuBackground> l = new ArrayList<>();
        l.addAll(this.backgrounds.values());
        return l;
    }

    /**
     * Returns the loaded {@link MenuBackground} with the identifier specified as parameter<br>
     * or NULL if there is no {@link MenuBackground} with that identifier.
     */
    public MenuBackground getBackgroundByIdentifier(String backgroundIdentifier) {
        return this.backgrounds.get(backgroundIdentifier);
    }

    /**
     * The display name of the menu background type.<br>
     * Used in the Set Background menu of the layout editor.<br><br>
     *
     * You can localize the display name here.
     */
    public abstract String getDisplayName();

    /**
     * The description lines (multiline) of the menu background type.<br>
     * Used in the Set Background menu of the layout editor.
     */
    public abstract List<String> getDescription();

    /**
     * If this background type needs an string input button in the layout editor instead of the horizontal slider to select loaded instances.<br><br>
     *
     * If this method returns {@code true}, the background type will not use its loaded backgrounds,<br>
     * but will call the {@link MenuBackgroundType#createInstanceFromInputString(String)} method instead to get a menu background instance.<br><br>
     */
    public abstract boolean needsInputString();

    /**
     * Called when a layout with this background type is getting loaded (to get the menu background).<br>
     * This only gets called when {@link MenuBackgroundType#needsInputString()} returns {@code true}.<br><br>
     *
     * This method should always return a <b>NEW</b> {@link MenuBackground} instance or NULL if {@link MenuBackgroundType#needsInputString()} returns {@code false}.<br><br>
     *
     * @param inputString The string to use as base for the {@link MenuBackground} instance. Can be everything (e.g. video file path, URL, etc.).
     * @return The <b>NEW</b> {@link MenuBackground} instance created from the given input string.
     */
    public abstract MenuBackground createInstanceFromInputString(String inputString);

    /**
     * Called when the input string button in the background options of the layout editor is pressed.<br>
     * Will open the file chooser by default.<br><br>
     *
     * You need to call {@link MenuBackgroundType#createInstanceFromInputString(String)} here to set a new instance of the background<br>
     * to {@link LayoutEditorScreen#customMenuBackground}.<br>
     * You also need to set the raw input string to {@link LayoutEditorScreen#customMenuBackgroundInputString}.<br><br>
     *
     * It is important to create a snapshot via {@link LayoutEditorScreen#history} <b>BEFORE</b> changing the menu background fields!<br><br>
     *
     * @param handler The active layout editor instance.
     * @param optionsPopup The active background options popup.
     */
    public void onInputStringButtonPress(LayoutEditorScreen handler, BackgroundOptionsPopup optionsPopup) {
        ChooseFilePopup cf = new ChooseFilePopup((call) -> {
            if (call != null) {
                handler.history.saveSnapshot(handler.history.createSnapshot());
                optionsPopup.resetBackgrounds();
                handler.customMenuBackgroundInputString = call;
                handler.customMenuBackground = this.createInstanceFromInputString(call);
            }
            PopupHandler.displayPopup(optionsPopup);
        });
        if ((handler.customMenuBackgroundInputString != null)) {
            cf.setText(handler.customMenuBackgroundInputString);
        }
        PopupHandler.displayPopup(cf);
    }

    /**
     * The label of the input string button in the layout editor.<br>
     * Only needed when {@link MenuBackgroundType#needsInputString()} returns {@code true}.
     */
    public String inputStringButtonLabel() {
        return Locals.localize("fancymenu.helper.editor.backgrounds.custom.choosefile");
    }

    /**
     * The multiline tooltip of the input string button in the layout editor.<br>
     * Only needed when {@link MenuBackgroundType#needsInputString()} returns {@code true}.
     */
    public List<String> inputStringButtonTooltip() {
        return null;
    }

    /**
     * Get the unique identifier of this {@link MenuBackgroundType}.
     */
    public String getIdentifier() {
        return this.typeIdentifier;
    }

}
